package org.wlcp.wlcpgameserver.service.impl;

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
import org.wlcp.wlcpgameserver.dto.messages.DisplayPhotoMessage;
import org.wlcp.wlcpgameserver.dto.messages.DisplayTextMessage;
import org.wlcp.wlcpgameserver.dto.messages.IMessage;
import org.wlcp.wlcpgameserver.dto.messages.KeyboardInputMessage;
import org.wlcp.wlcpgameserver.dto.messages.NoStateMessage;
import org.wlcp.wlcpgameserver.dto.messages.NoTransitionMessage;
import org.wlcp.wlcpgameserver.dto.messages.SequenceButtonPressMessage;
import org.wlcp.wlcpgameserver.dto.messages.SingleButtonPressMessage;
import org.wlcp.wlcpgameserver.model.Player;

import jdk.nashorn.api.scripting.JSObject;

@Controller
@Scope("prototype")
public class PlayerVMService extends Thread {
	
	Logger logger = LoggerFactory.getLogger(GameInstanceService.class);
	
	@Autowired
	SimpMessagingTemplate messageTemplate;
	
	private GameInstanceService gameInstanceService;
	private Player player;
	private String transpiledGame;
	private ScriptEngine scriptEngine;
	private boolean block = true;
	private boolean reconnect = false;
	private boolean shutdown = false;
	private IMessage blockMessage = null;
	private IMessage lastSentPacket = null;
	
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
				try {
					scriptEngine.eval("FSMGame.oldState = FSMGame.oldState - 1");
				} catch (ScriptException e) {
					e.printStackTrace();
				}
				return (int) ((JSObject)scriptEngine.get("FSMGame")).getMember("state");
			}
			if(shutdown) {
				return - 3;
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
			try {
				scriptEngine.eval("FSMGame.oldState = FSMGame.oldState - 1");
			} catch (ScriptException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void NoState() {
		NoStateMessage msg = new NoStateMessage();
		messageTemplate.convertAndSend("/subscription/gameInstance/" + gameInstanceService.getGameInstance().getGameInstanceId() + "/noState/" + player.usernameClientData.username.usernameId + "/" + player.teamPlayer.team + "/" + player.teamPlayer.player,  msg);
	}
	
	public void DisplayText(String text) {
		DisplayTextMessage msg = new DisplayTextMessage();
		msg.displayText = text;
		messageTemplate.convertAndSend("/subscription/gameInstance/" + gameInstanceService.getGameInstance().getGameInstanceId() + "/displayText/" + player.usernameClientData.username.usernameId + "/" + player.teamPlayer.team + "/" + player.teamPlayer.player,  msg);
	}
	
	public void DisplayPhoto(String url, int scale) {
		DisplayPhotoMessage msg = new DisplayPhotoMessage();
		msg.url = url;
		msg.scale = scale;
		messageTemplate.convertAndSend("/subscription/gameInstance/" + gameInstanceService.getGameInstance().getGameInstanceId() + "/displayPhoto/" + player.usernameClientData.username.usernameId + "/" + player.teamPlayer.team + "/" + player.teamPlayer.player,  msg);
		
	}
	
	public void NoTransition() {
		NoTransitionMessage msg = new NoTransitionMessage();
		messageTemplate.convertAndSend("/subscription/gameInstance/" + gameInstanceService.getGameInstance().getGameInstanceId() + "/noTransition/" + player.usernameClientData.username.usernameId + "/" + player.teamPlayer.team + "/" + player.teamPlayer.player,  msg);
	}
	
	public int SingleButtonPress(String[] buttons, int[] transitions) throws ScriptException {
		block = true;
		SingleButtonPressMessage msg = new SingleButtonPressMessage();
		lastSentPacket = msg;
		messageTemplate.convertAndSend("/subscription/gameInstance/" + gameInstanceService.getGameInstance().getGameInstanceId() + "/singleButtonPressRequest/" + player.usernameClientData.username.usernameId + "/" + player.teamPlayer.team + "/" + player.teamPlayer.player,  msg);
		int state;
		while((state = block()) == -2) {}
		if(state != -2 && state != -1) { return state; }
		msg = (SingleButtonPressMessage) blockMessage;
		for(int i = 0; i < buttons.length; i++) {
			if(buttons[i].equals(Integer.toString(msg.buttonPress))) {
				return transitions[i];
			}
		}
		return gotoSameState();
	}
	
	public int SequenceButtonPress(String[] buttons, int[] transitions) {
		block = true;
		SequenceButtonPressMessage msg = new SequenceButtonPressMessage();
		lastSentPacket = msg;
		messageTemplate.convertAndSend("/subscription/gameInstance/" + gameInstanceService.getGameInstance().getGameInstanceId() + "/sequenceButtonPressRequest/" + player.usernameClientData.username.usernameId + "/" + player.teamPlayer.team + "/" + player.teamPlayer.player,  msg);
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
		return gotoSameState();
	}
	
	public int KeyboardInput(String[] keyboardInput, int[] transitions) {
		block = true;
		KeyboardInputMessage msg = new KeyboardInputMessage();
		lastSentPacket = msg;
		messageTemplate.convertAndSend("/subscription/gameInstance/" + gameInstanceService.getGameInstance().getGameInstanceId() + "/keyboardInputRequest/" + player.usernameClientData.username.usernameId + "/" + player.teamPlayer.team + "/" + player.teamPlayer.player,  msg);
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
		return gotoSameState();
	}
	
	private int gotoSameState() {
		try {
			scriptEngine.eval("FSMGame.oldState = FSMGame.oldState - 1");
		} catch (ScriptException e) {
			e.printStackTrace();
		}
		return (int) ((JSObject)scriptEngine.get("FSMGame")).getMember("state");
	}
}
