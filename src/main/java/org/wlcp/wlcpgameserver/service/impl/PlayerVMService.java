package org.wlcp.wlcpgameserver.service.impl;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.wlcp.wlcpgameserver.datamodel.enums.GameStatus;
import org.wlcp.wlcpgameserver.datamodel.master.GameInstancePlayer;
import org.wlcp.wlcpgameserver.dto.messages.DisplayPhotoMessage;
import org.wlcp.wlcpgameserver.dto.messages.DisplayTextMessage;
import org.wlcp.wlcpgameserver.dto.messages.EmptyMessage;
import org.wlcp.wlcpgameserver.dto.messages.IMessage;
import org.wlcp.wlcpgameserver.dto.messages.KeyboardInputMessage;
import org.wlcp.wlcpgameserver.dto.messages.NoStateMessage;
import org.wlcp.wlcpgameserver.dto.messages.NoTransitionMessage;
import org.wlcp.wlcpgameserver.dto.messages.PlaySoundMessage;
import org.wlcp.wlcpgameserver.dto.messages.PlayVideoMessage;
import org.wlcp.wlcpgameserver.dto.messages.SequenceButtonPressMessage;
import org.wlcp.wlcpgameserver.dto.messages.SingleButtonPressMessage;
import org.wlcp.wlcpgameserver.dto.messages.TimerDurationMessage;
import org.wlcp.wlcpgameserver.dto.messages.combined.CombinedMessage;
import org.wlcp.wlcpgameserver.dto.messages.combined.InputMessage;
import org.wlcp.wlcpgameserver.dto.messages.combined.MessageType;
import org.wlcp.wlcpgameserver.dto.messages.combined.OutputMessage;
import org.wlcp.wlcpgameserver.feignclient.dto.LogEventGamePlayerCommunicationDto.DataDirection;
import org.wlcp.wlcpgameserver.feignclient.dto.LogEventGamePlayerCommunicationDto.Input;
import org.wlcp.wlcpgameserver.feignclient.dto.LogEventGamePlayerCommunicationDto.Output;
import org.wlcp.wlcpgameserver.feignclient.dto.LogEventGamePlayerServerEventDto.Event;
import org.wlcp.wlcpgameserver.feignclient.dto.LogEventGamePlayerServerEventDto.Type;
import org.wlcp.wlcpgameserver.model.Player;
import org.wlcp.wlcpgameserver.repository.GameInstanceRepository;
import org.wlcp.wlcpgameserver.service.MetricsService;

import jdk.nashorn.api.scripting.JSObject;

@Controller
@Scope("prototype")
public class PlayerVMService extends Thread {
	
	Logger logger = LoggerFactory.getLogger(GameInstanceService.class);
	
	@Autowired
	SimpMessagingTemplate messageTemplate;
	
	@Autowired
	private GameInstanceRepository gameInstanceRepository;
	
	@Autowired
	private MetricsService metricsService;
	
	private GameInstanceService gameInstanceService;
	private Player player;
	private String transpiledGame;
	private ScriptEngine scriptEngine;
	private boolean block = true;
	private boolean reconnect = false;
	private boolean shutdown = false;
	private boolean timerElapsed = false;
	private int timerElapsedNextState = 0;
	private IMessage blockMessage = null;
	private IMessage lastSentPacket = null;
	private CombinedMessage combinedMessage = new CombinedMessage();
	private Semaphore masterGlobalVariableMutex = new Semaphore(1);
	
	public void setupVariables(GameInstanceService gameInstanceService, Player player, String transpiledGame) {
		this.gameInstanceService = gameInstanceService;
		this.player = player;
		this.transpiledGame = transpiledGame;
		logger.info("PlayerVM for username :" + player.usernameClientData.username.usernameId + " started on game instance: " + gameInstanceService.getGameInstance().getGameInstanceId());
		this.setName("WLCP-" + gameInstanceService.getGame().gameId + "-" + gameInstanceService.getGameInstance().getGameInstanceId() + "-" + player.usernameClientData.username.usernameId + "T" + player.teamPlayer.team + "P" + player.teamPlayer.player);
	}

	@Override
	public void run() {
		try {
			startVM();
			endOfGame();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ScriptException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void startVM() throws ScriptException, NoSuchMethodException {
		scriptEngine = new ScriptEngineManager().getEngineByName("nashorn");
		scriptEngine.eval(transpiledGame);
		scriptEngine.eval("FSMGame.gameInstanceId = " + gameInstanceService.getGameInstance().getGameInstanceId() + ";");
		Object json = scriptEngine.get("FSMGame");
		Invocable invocable = (Invocable) scriptEngine;
		invocable.invokeFunction("SetGameVariables", gameInstanceService.getGameInstance().getGameInstanceId(), player.teamPlayer.team + 1, player.teamPlayer.player + 1, this);
		invocable.invokeMethod(json, "start");
	}
	
	private void endOfGame() {
		for(GameInstancePlayer player : gameInstanceService.getGameInstance().getPlayers()) {
			if(player.getUsernameId().equals(this.player.usernameClientData.username.usernameId)) {
				player.setGameStatus(GameStatus.GAME_ENDED);
				gameInstanceRepository.save(gameInstanceService.getGameInstance());
				messageTemplate.convertAndSend("/subscription/gameInstance/" + gameInstanceService.getGameInstance().getGameInstanceId() + "/gameEnded/" + this.player.usernameClientData.username.usernameId + "/" + this.player.teamPlayer.team + "/" + this.player.teamPlayer.player,  "{}");
				break;
			}
		}
	}
	
	public void shutdown() {
		
		//Set the running variable to false
		try {
			scriptEngine.eval("FSMGame.running = false;");
		} catch (ScriptException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Shutdown
		shutdown = true;
	}
	
	private int block() {
		if(block) {
			if(reconnect) {
				unblock(null);
				reconnect = false;
				return gotoSameState();
			}
			if(shutdown) {
				return - 3;
			}
			if(timerElapsed) {
				timerElapsed = false;
				return timerElapsedNextState;
			}
			try {
				Thread.sleep(17);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return -2;
		}
		return -1;
	}
	
	public void unblock(IMessage message) {
		metricsService.logServerEvent(gameInstanceService.getStartLoggingGameInstanceDto().id, player, Type.TRANSITION, Event.EXIT, scriptEngine);
		if(message == null || lastSentPacket == null) { block = false; blockMessage = null; return;}
		if(message.getClass().equals(lastSentPacket.getClass())) {
			block = false;
			blockMessage = message;
		}
	}
	
	public void reconnect() {
		if(block) {
			reconnect = true;
		} else {
			gotoSameState();
			try {
				scriptEngine.eval("FSMGame.running = true");
				Invocable invocable = (Invocable) scriptEngine;
				invocable.invokeMethod(scriptEngine.get("FSMGame"), "start");
			} catch (NoSuchMethodException | ScriptException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void NoState() {
		metricsService.logServerEvent(gameInstanceService.getStartLoggingGameInstanceDto().id, player, Type.STATE, Event.ENTER, scriptEngine);
		NoStateMessage msg = new NoStateMessage();
		messageTemplate.convertAndSend("/subscription/gameInstance/" + gameInstanceService.getGameInstance().getGameInstanceId() + "/noState/" + player.usernameClientData.username.usernameId + "/" + player.teamPlayer.team + "/" + player.teamPlayer.player,  msg);
		combinedMessage.outputMessages.add(new OutputMessage(MessageType.NO_STATE, msg));
		messageTemplate.convertAndSend("/subscription/gameInstance/" + gameInstanceService.getGameInstance().getGameInstanceId() + "/combinedMessage/" + player.usernameClientData.username.usernameId + "/" + player.teamPlayer.team + "/" + player.teamPlayer.player,  combinedMessage);
		combinedMessage.outputMessages.clear();
		combinedMessage.inputMessages.clear();
		metricsService.logServerCommunication(gameInstanceService.getGameInstance().getGameInstanceId(), player.teamPlayer.team, player.teamPlayer.player, DataDirection.SERVER_SEND, Output.NONE, Input.NONE, "No State");
		metricsService.logServerEvent(gameInstanceService.getStartLoggingGameInstanceDto().id, player, Type.STATE, Event.EXIT, scriptEngine);
	}
	
	public void DisplayText(String text) throws ScriptException {
		metricsService.logServerEvent(gameInstanceService.getStartLoggingGameInstanceDto().id, player, Type.STATE, Event.ENTER, scriptEngine);
		DisplayTextMessage msg = new DisplayTextMessage();
		msg.displayText = text;
		messageTemplate.convertAndSend("/subscription/gameInstance/" + gameInstanceService.getGameInstance().getGameInstanceId() + "/displayText/" + player.usernameClientData.username.usernameId + "/" + player.teamPlayer.team + "/" + player.teamPlayer.player,  msg);
		combinedMessage.outputMessages.add(new OutputMessage(MessageType.DISPLAY_TEXT, msg));
		metricsService.logServerCommunication(gameInstanceService.getStartLoggingGameInstanceDto().id, player.teamPlayer.team, player.teamPlayer.player, DataDirection.SERVER_SEND, Output.DISPLAY_TEXT, Input.NONE, MetricsService.convertMessage(msg));
		metricsService.logServerEvent(gameInstanceService.getStartLoggingGameInstanceDto().id, player, Type.STATE, Event.EXIT, scriptEngine);
	}
	
	public void DisplayPhoto(String url, int scale) {
		metricsService.logServerEvent(gameInstanceService.getStartLoggingGameInstanceDto().id, player, Type.STATE, Event.ENTER, scriptEngine);
		DisplayPhotoMessage msg = new DisplayPhotoMessage();
		msg.url = url;
		msg.scale = scale;
		messageTemplate.convertAndSend("/subscription/gameInstance/" + gameInstanceService.getGameInstance().getGameInstanceId() + "/displayPhoto/" + player.usernameClientData.username.usernameId + "/" + player.teamPlayer.team + "/" + player.teamPlayer.player,  msg);
		combinedMessage.outputMessages.add(new OutputMessage(MessageType.DISPLAY_PHOTO, msg));
		metricsService.logServerCommunication(gameInstanceService.getStartLoggingGameInstanceDto().id, player.teamPlayer.team, player.teamPlayer.player, DataDirection.SERVER_SEND, Output.DISPLAY_PHOTO, Input.NONE, MetricsService.convertMessage(msg));
		metricsService.logServerEvent(gameInstanceService.getStartLoggingGameInstanceDto().id, player, Type.STATE, Event.EXIT, scriptEngine);
	}
	
	public void PlaySound(String url) {
		metricsService.logServerEvent(gameInstanceService.getStartLoggingGameInstanceDto().id, player, Type.STATE, Event.ENTER, scriptEngine);
		PlaySoundMessage msg = new PlaySoundMessage();
		msg.url = url;
		messageTemplate.convertAndSend("/subscription/gameInstance/" + gameInstanceService.getGameInstance().getGameInstanceId() + "/playSound/" + player.usernameClientData.username.usernameId + "/" + player.teamPlayer.team + "/" + player.teamPlayer.player,  msg);
		combinedMessage.outputMessages.add(new OutputMessage(MessageType.PLAY_SOUND, msg));
		metricsService.logServerCommunication(gameInstanceService.getStartLoggingGameInstanceDto().id, player.teamPlayer.team, player.teamPlayer.player, DataDirection.SERVER_SEND, Output.PLAY_SOUND, Input.NONE, MetricsService.convertMessage(msg));
		metricsService.logServerEvent(gameInstanceService.getStartLoggingGameInstanceDto().id, player, Type.STATE, Event.EXIT, scriptEngine);
	} 
	
	public void PlayVideo(String url) {
		metricsService.logServerEvent(gameInstanceService.getStartLoggingGameInstanceDto().id, player, Type.STATE, Event.ENTER, scriptEngine);
		PlayVideoMessage msg = new PlayVideoMessage();
		msg.url = url;
		messageTemplate.convertAndSend("/subscription/gameInstance/" + gameInstanceService.getGameInstance().getGameInstanceId() + "/playVideo/" + player.usernameClientData.username.usernameId + "/" + player.teamPlayer.team + "/" + player.teamPlayer.player,  msg);
		combinedMessage.outputMessages.add(new OutputMessage(MessageType.PLAY_VIDEO, msg));
		metricsService.logServerCommunication(gameInstanceService.getStartLoggingGameInstanceDto().id, player.teamPlayer.team, player.teamPlayer.player, DataDirection.SERVER_SEND, Output.PLAY_VIDEO, Input.NONE, MetricsService.convertMessage(msg));
		metricsService.logServerEvent(gameInstanceService.getStartLoggingGameInstanceDto().id, player, Type.STATE, Event.EXIT, scriptEngine);
	}
		
	
	public void NoTransition() {
		metricsService.logServerEvent(gameInstanceService.getStartLoggingGameInstanceDto().id, player, Type.TRANSITION, Event.ENTER, scriptEngine);
		NoTransitionMessage msg = new NoTransitionMessage();
		messageTemplate.convertAndSend("/subscription/gameInstance/" + gameInstanceService.getGameInstance().getGameInstanceId() + "/noTransition/" + player.usernameClientData.username.usernameId + "/" + player.teamPlayer.team + "/" + player.teamPlayer.player,  msg);
		combinedMessage.inputMessages.add(new InputMessage(MessageType.NO_TRANSITION, msg));
		messageTemplate.convertAndSend("/subscription/gameInstance/" + gameInstanceService.getGameInstance().getGameInstanceId() + "/combinedMessage/" + player.usernameClientData.username.usernameId + "/" + player.teamPlayer.team + "/" + player.teamPlayer.player,  combinedMessage);
		combinedMessage.outputMessages.clear();
		combinedMessage.inputMessages.clear();
		try {
			metricsService.logServerCommunication(gameInstanceService.getStartLoggingGameInstanceDto().id, player.teamPlayer.team, player.teamPlayer.player, DataDirection.SERVER_SEND, Output.NONE, Input.NONE, "No Transition");
			metricsService.logServerEvent(gameInstanceService.getStartLoggingGameInstanceDto().id, player, Type.TRANSITION, Event.EXIT, scriptEngine);
		} catch (Exception e) {
			
		}
	}

	public int SingleButtonPress(String[] buttons, int[] transitions, String[] labels) {
		while(true) {
			block = true;
			SingleButtonPressMessage msg = new SingleButtonPressMessage();
			msg.label1 = labels[0];
			msg.label2 = labels[1];
			msg.label3 = labels[2];
			msg.label4 = labels[3];
			lastSentPacket = msg;
			messageTemplate.convertAndSend("/subscription/gameInstance/" + gameInstanceService.getGameInstance().getGameInstanceId() + "/singleButtonPressRequest/" + player.usernameClientData.username.usernameId + "/" + player.teamPlayer.team + "/" + player.teamPlayer.player,  msg);
			combinedMessage.inputMessages.add(new InputMessage(MessageType.SINGLE_BUTTON_PRESS, msg));
			messageTemplate.convertAndSend("/subscription/gameInstance/" + gameInstanceService.getGameInstance().getGameInstanceId() + "/combinedMessage/" + player.usernameClientData.username.usernameId + "/" + player.teamPlayer.team + "/" + player.teamPlayer.player,  combinedMessage);
			combinedMessage.outputMessages.clear();
			combinedMessage.inputMessages.clear();
			metricsService.logServerCommunication(gameInstanceService.getStartLoggingGameInstanceDto().id, player.teamPlayer.team, player.teamPlayer.player, DataDirection.SERVER_SEND, Output.NONE, Input.SINGLE_BUTTON_PRESS, MetricsService.convertMessage(msg));
			metricsService.logServerEvent(gameInstanceService.getStartLoggingGameInstanceDto().id, player, Type.TRANSITION, Event.ENTER, scriptEngine);
			int state;
			while((state = block()) == -2) {}
			if(state != -2 && state != -1) { return state; }
			msg = (SingleButtonPressMessage) blockMessage;
			for(int i = 0; i < buttons.length; i++) {
				if(buttons[i].equals(Integer.toString(msg.buttonPress))) {
					return transitions[i];
				}
			}
		}
	}
	
	public int SequenceButtonPress(String[] buttons, int[] transitions) {
		while(true) {
			block = true;
			SequenceButtonPressMessage msg = new SequenceButtonPressMessage();
			lastSentPacket = msg;
			messageTemplate.convertAndSend("/subscription/gameInstance/" + gameInstanceService.getGameInstance().getGameInstanceId() + "/sequenceButtonPressRequest/" + player.usernameClientData.username.usernameId + "/" + player.teamPlayer.team + "/" + player.teamPlayer.player,  msg);
			combinedMessage.inputMessages.add(new InputMessage(MessageType.SEQUENCE_BUTTON_PRESS, msg));
			messageTemplate.convertAndSend("/subscription/gameInstance/" + gameInstanceService.getGameInstance().getGameInstanceId() + "/combinedMessage/" + player.usernameClientData.username.usernameId + "/" + player.teamPlayer.team + "/" + player.teamPlayer.player,  combinedMessage);
			combinedMessage.outputMessages.clear();
			combinedMessage.inputMessages.clear();
			metricsService.logServerCommunication(gameInstanceService.getStartLoggingGameInstanceDto().id, player.teamPlayer.team, player.teamPlayer.player, DataDirection.SERVER_SEND, Output.NONE, Input.SEQUENCE_BUTTON_PRESS, MetricsService.convertMessage(msg));
			metricsService.logServerEvent(gameInstanceService.getStartLoggingGameInstanceDto().id, player, Type.TRANSITION, Event.ENTER, scriptEngine);
			int state;
			while((state = block()) == -2) {}
			if(state != -2 && state != -1) { return state; }
			msg = (SequenceButtonPressMessage) blockMessage;
			for(int i = 0; i < buttons.length; i++) {
				if(buttons[i].equals(msg.sequenceButtonPress)) {
					return transitions[i];
				}
			}
			for(int i = 0; i < buttons.length; i++) {
				if(buttons[i].equals("")) {
					return transitions[i];
				}
			}
		}
	}
	
	public int KeyboardInput(String[] keyboardInput, int[] transitions) {
		while(true) {
			block = true;
			KeyboardInputMessage msg = new KeyboardInputMessage();
			lastSentPacket = msg;
			messageTemplate.convertAndSend("/subscription/gameInstance/" + gameInstanceService.getGameInstance().getGameInstanceId() + "/keyboardInputRequest/" + player.usernameClientData.username.usernameId + "/" + player.teamPlayer.team + "/" + player.teamPlayer.player,  msg);
			combinedMessage.inputMessages.add(new InputMessage(MessageType.KEYBOARD_INPUT, msg));
			messageTemplate.convertAndSend("/subscription/gameInstance/" + gameInstanceService.getGameInstance().getGameInstanceId() + "/combinedMessage/" + player.usernameClientData.username.usernameId + "/" + player.teamPlayer.team + "/" + player.teamPlayer.player,  combinedMessage);
			combinedMessage.outputMessages.clear();
			combinedMessage.inputMessages.clear();
			metricsService.logServerCommunication(gameInstanceService.getStartLoggingGameInstanceDto().id, player.teamPlayer.team, player.teamPlayer.player, DataDirection.SERVER_SEND, Output.NONE, Input.KEYBOARD_INPUT, MetricsService.convertMessage(msg));
			metricsService.logServerEvent(gameInstanceService.getStartLoggingGameInstanceDto().id, player, Type.TRANSITION, Event.ENTER, scriptEngine);
			int state;
			while((state = block()) == -2) {}
			if(state != -2 && state != -1) { return state; }
			msg = (KeyboardInputMessage) blockMessage;
			for(int i = 0; i < keyboardInput.length; i++) {
				if(keyboardInput[i].equals(msg.keyboardInput)) {
					return transitions[i];
				}
			}
			for(int i = 0; i < keyboardInput.length; i++) {
				if(keyboardInput[i].equals("")) {
					return transitions[i];
				}
			}
		}
	}
	
	public int RandomTransition(int[] randomStates) {
		while(true) {
			block = true;
			EmptyMessage msg = new EmptyMessage();
			lastSentPacket = msg;
			messageTemplate.convertAndSend("/subscription/gameInstance/" + gameInstanceService.getGameInstance().getGameInstanceId() + "/randomInputRequest/" + player.usernameClientData.username.usernameId + "/" + player.teamPlayer.team + "/" + player.teamPlayer.player,  msg);
			combinedMessage.inputMessages.add(new InputMessage(MessageType.RANDOM, msg));
			messageTemplate.convertAndSend("/subscription/gameInstance/" + gameInstanceService.getGameInstance().getGameInstanceId() + "/combinedMessage/" + player.usernameClientData.username.usernameId + "/" + player.teamPlayer.team + "/" + player.teamPlayer.player,  combinedMessage);
			combinedMessage.outputMessages.clear();
			combinedMessage.inputMessages.clear();
			metricsService.logServerCommunication(gameInstanceService.getStartLoggingGameInstanceDto().id, player.teamPlayer.team, player.teamPlayer.player, DataDirection.SERVER_SEND, Output.NONE, Input.RANDOM, MetricsService.convertMessage(msg));
			metricsService.logServerEvent(gameInstanceService.getStartLoggingGameInstanceDto().id, player, Type.TRANSITION, Event.ENTER, scriptEngine);
			int state;
			while((state = block()) == -2) {}
			if(state != -2 && state != -1) { return state; }
			msg = (EmptyMessage) blockMessage;
			Random random = new Random(System.currentTimeMillis());
			return randomStates[random.nextInt(randomStates.length - 1)];
		}
	}
	
	public void Delay(int delay) throws InterruptedException {
		block = true;
		TimerDurationMessage msg = new TimerDurationMessage();
		msg.duration = delay;
		lastSentPacket = msg;
		messageTemplate.convertAndSend("/subscription/gameInstance/" + gameInstanceService.getGameInstance().getGameInstanceId() + "/timerDurationRequest/" + player.usernameClientData.username.usernameId + "/" + player.teamPlayer.team + "/" + player.teamPlayer.player,  msg);
		combinedMessage.inputMessages.add(new InputMessage(MessageType.TIMER_DURATION, msg));
		messageTemplate.convertAndSend("/subscription/gameInstance/" + gameInstanceService.getGameInstance().getGameInstanceId() + "/combinedMessage/" + player.usernameClientData.username.usernameId + "/" + player.teamPlayer.team + "/" + player.teamPlayer.player,  combinedMessage);
		combinedMessage.outputMessages.clear();
		combinedMessage.inputMessages.clear();
		metricsService.logServerCommunication(gameInstanceService.getStartLoggingGameInstanceDto().id, player.teamPlayer.team, player.teamPlayer.player, DataDirection.SERVER_SEND, Output.NONE, Input.TIMER, MetricsService.convertMessage(msg));
		metricsService.logServerEvent(gameInstanceService.getStartLoggingGameInstanceDto().id, player, Type.TRANSITION, Event.ENTER, scriptEngine);
		Thread.sleep(delay * 1000);
		block = false;
		return;
	}
	
	public void Timer(int delay, int nextState) {
		TimerDurationMessage msg = new TimerDurationMessage();
		msg.duration = delay;
		msg.isTimer = true;
		lastSentPacket = msg;
		messageTemplate.convertAndSend("/subscription/gameInstance/" + gameInstanceService.getGameInstance().getGameInstanceId() + "/timerDurationRequest/" + player.usernameClientData.username.usernameId + "/" + player.teamPlayer.team + "/" + player.teamPlayer.player,  msg);
		combinedMessage.inputMessages.add(new InputMessage(MessageType.TIMER_DURATION, msg));
		this.timerElapsedNextState = nextState;
		Timer timer = new Timer("Timer");
		timer.schedule(new TimerTask() {
	        public void run() {
	            timerElapsed = true;
	        }}, delay * 1000);
		metricsService.logServerCommunication(gameInstanceService.getStartLoggingGameInstanceDto().id, player.teamPlayer.team, player.teamPlayer.player, DataDirection.SERVER_SEND, Output.NONE, Input.TIMER, MetricsService.convertMessage(msg));
		metricsService.logServerEvent(gameInstanceService.getStartLoggingGameInstanceDto().id, player, Type.TRANSITION, Event.ENTER, scriptEngine);
	}
	
	public Object getGlobalVariableMaster(String variableName) throws ScriptException, InterruptedException {
		masterGlobalVariableMutex.acquire();
		Object value = scriptEngine.eval("FSMGame." + variableName);
		masterGlobalVariableMutex.release();
		return value;
	}

	public Object getGlobalVariable(String variableName) throws ScriptException, InterruptedException {
		return gameInstanceService.getMasterPlayerVMService().getGlobalVariableMaster(variableName);
	}

	public Object setGlobalVariableMaster(String expression) throws ScriptException, InterruptedException {
		masterGlobalVariableMutex.acquire();
		Object value = scriptEngine.eval(expression);
		masterGlobalVariableMutex.release();
		return value;
	}

	public Object setGlobalVariable(String expression) throws ScriptException, InterruptedException {
		return gameInstanceService.getMasterPlayerVMService().setGlobalVariableMaster(expression);
	}
	
	private int gotoSameState() {
		try {
			scriptEngine.eval("FSMGame.state = FSMGame.oldState");
		} catch (ScriptException e) {
			e.printStackTrace();
		}
		return (int) ((JSObject)scriptEngine.get("FSMGame")).getMember("state");
	}
}
