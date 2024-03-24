package org.wlcp.wlcpgameserver.service.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpAttributesContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.wlcp.wlcpgameserver.controller.GameInstanceController;
import org.wlcp.wlcpgameserver.datamodel.enums.ConnectionStatus;
import org.wlcp.wlcpgameserver.datamodel.enums.GameStatus;
import org.wlcp.wlcpgameserver.datamodel.master.GameInstance;
import org.wlcp.wlcpgameserver.datamodel.master.GameInstancePlayer;
import org.wlcp.wlcpgameserver.dto.GameDto;
import org.wlcp.wlcpgameserver.dto.UsernameDto;
import org.wlcp.wlcpgameserver.dto.messages.ConnectRequestMessage;
import org.wlcp.wlcpgameserver.dto.messages.ConnectResponseMessage;
import org.wlcp.wlcpgameserver.dto.messages.ConnectResponseMessage.Code;
import org.wlcp.wlcpgameserver.dto.messages.EmptyMessage;
import org.wlcp.wlcpgameserver.dto.messages.IMessage;
import org.wlcp.wlcpgameserver.dto.messages.KeyboardInputMessage;
import org.wlcp.wlcpgameserver.dto.messages.PlayerAvaliableMessage;
import org.wlcp.wlcpgameserver.dto.messages.SequenceButtonPressMessage;
import org.wlcp.wlcpgameserver.dto.messages.SingleButtonPressMessage;
import org.wlcp.wlcpgameserver.feignclient.TranspilerFeignClient;
import org.wlcp.wlcpgameserver.feignclient.UsernameFeignClient;
import org.wlcp.wlcpgameserver.feignclient.dto.LogEventGamePlayerCommunicationDto.DataDirection;
import org.wlcp.wlcpgameserver.feignclient.dto.LogEventGamePlayerCommunicationDto.Input;
import org.wlcp.wlcpgameserver.feignclient.dto.LogEventGamePlayerCommunicationDto.Output;
import org.wlcp.wlcpgameserver.feignclient.dto.LogEventGamePlayerServerMessageDto.LogEventGamePlayerServerType;
import org.wlcp.wlcpgameserver.feignclient.dto.StartLoggingGameInstanceDto;
import org.wlcp.wlcpgameserver.model.Player;
import org.wlcp.wlcpgameserver.model.TeamPlayer;
import org.wlcp.wlcpgameserver.model.UsernameClientData;
import org.wlcp.wlcpgameserver.repository.GameInstanceRepository;
import org.wlcp.wlcpgameserver.service.MetricsService;

@Controller
@RequestMapping("/controllers")
@Scope("prototype")
public class GameInstanceService extends Thread {
	
	Logger logger = LoggerFactory.getLogger(GameInstanceService.class);
	
	@Autowired
	private GameInstanceRepository gameInstanceRepository;
	
	@Autowired
	private UsernameFeignClient usernameFeignClient;
	
	@Autowired
	private TranspilerFeignClient transpilerFeignClient;
	
	@Autowired
	private MetricsService metricsService;
	
	@Autowired
	private GameInstanceController gameInstanceController;
	
	@Autowired
	private ApplicationContext context;
	
	@Value("${security.jwt-token}")
	private String jwtToken;
	
	private GameDto game;
	private UsernameDto username;
	private GameInstance gameInstance;
	private boolean debugInstance;
	private boolean archivedGame;
	private StartLoggingGameInstanceDto startLoggingGameInstanceDto;
	private Map<String, String> playerNames;
	
	private String transpiledGame;
	
	private CopyOnWriteArrayList<IMessage> messages = new CopyOnWriteArrayList<IMessage>();
	private CopyOnWriteArrayList<Player> players = new CopyOnWriteArrayList<Player>();
	
	private boolean running = true;
	
	private PlayerVMService masterPlayerVMService;
	
	public CountDownLatch done = new CountDownLatch(1);
	
	private Timer shutdownTimer = null;
	private long shutdownDelay = 300000; //5 minutes * 60 seconds * 1000 miliseconds
	private TimerTask shutdownTimerTask = null;
	
	public void setupVariables(GameDto game, UsernameDto username, boolean debugInstance, boolean archivedGame, Map<String, String> playerNames) {
		this.game = game;
		this.username = username;
		this.debugInstance = debugInstance;
		this.archivedGame = archivedGame;
		this.playerNames = playerNames;
	}
	
	@Override
	public void run() {
		setup();
		while(running) {
			for(IMessage message : messages) {
				handleMessage(message);
			}
			try {
				Thread.sleep(17);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void handleMessage(IMessage message) {

	}
	
	private void setup() {
		gameInstance = new GameInstance(game.gameId, username.usernameId, debugInstance);
		gameInstance = gameInstanceRepository.save(gameInstance);
		if(!debugInstance) { logger.info("Game Instance: " + gameInstance.getGameInstanceId() + " started! Playing the game: " + game.gameId); }
		if(debugInstance) { logger.info("Debug Game Instance: " + gameInstance.getGameInstanceId() + " started! Playing the game: " + game.gameId); }
		this.setName("WLCP-" + game.gameId + "-" + gameInstance.getGameInstanceId());
		transpiledGame = transpilerFeignClient.transpileGame(game.gameId, archivedGame);
		startLoggingGameInstanceDto = metricsService.startLoggingGameInstance(gameInstance.getGameId(), username.usernameId, debugInstance);
		metricsService.logServerMessage(startLoggingGameInstanceDto.id, LogEventGamePlayerServerType.GAME_INSTANCE_STARTED, "Game Instance: " + gameInstance.getGameInstanceId() + " started! Playing the game: " + game.gameId);
		masterPlayerVMService = this.StartMasterVM();
		setupShutdownTimer();
		done.countDown();
	}
	
	private void setupShutdownTimer() {
		shutdownTimer = new Timer("ShutdownTimer-" + gameInstance.getGameId() + "-" + gameInstance.getGameInstanceId());
		shutdownTimerTask = new TimerTask() {
	        public void run() {
	        	for(GameInstancePlayer player : gameInstance.getPlayers()) {
	    			if(player.getWebSocketConnectionStatus().equals(ConnectionStatus.CONNECTED)) {
	    				logger.info("Shutdown Timer has elapsed. Players are still connected. Game instance will continue to run.");
	    				return;
	    			}
	    		}
	        	logger.info("Shutdown Timer has elapsed. No players are connected. Game instance will shutdown.");
	        	shutdownTimer.cancel();
	    		shutdown();
	        }
	    };
		shutdownTimer.schedule(shutdownTimerTask, shutdownDelay, shutdownDelay);
	}
	
	public ConnectResponseMessage userConnect(ConnectRequestMessage connect) {
		
		//Get the user from the db
		UsernameDto usernameDto = usernameFeignClient.getUsername(connect.usernameId, jwtToken);
		
		if(usernameDto == null) {
			usernameDto = new UsernameDto();
			usernameDto.usernameId = connect.usernameId;
			usernameDto.tempPlayer = true;
		}
		
		//Check to make sure the player doesnt already exist in the game (for reconnect)
		if(!debugInstance) {
			for(Player player : players) {
				if(player.usernameClientData.username.usernameId.equals(usernameDto.usernameId)) {
					for(GameInstancePlayer gameInstancePlayer : gameInstance.getPlayers()) {
						if(gameInstancePlayer.getUsernameId().equals(player.usernameClientData.username.usernameId)) {
							gameInstancePlayer.setSessionId(SimpAttributesContextHolder.currentAttributes().getSessionId());
							gameInstancePlayer.setWebSocketConnectionStatus(ConnectionStatus.CONNECTED);
							gameInstancePlayer.setGameInstanceConnectionStatus(ConnectionStatus.CONNECTED);
							gameInstanceRepository.save(gameInstance);
							player.usernameClientData.sessionId = gameInstancePlayer.getSessionId();
							logger.info("WebSocket Reconnection Made Session Id: " + gameInstancePlayer.getSessionId());
							break;
						}
					}
					//User already exists in the game, maybe they are trying to reconnect?
					player.playerVM.reconnect();
					metricsService.logServerMessage(startLoggingGameInstanceDto.id, LogEventGamePlayerServerType.RECONNECT, "Reconnecting " + usernameDto.usernameId);
					ConnectResponseMessage msg = new ConnectResponseMessage();
					msg.team = player.teamPlayer.team;
					msg.player = player.teamPlayer.player;
					msg.code = Code.RECONNECT;
					return msg;
				}
			}
		}
		
		//Check if someone is already occupying their team / player
		for(Player player : players) {
			if(player.teamPlayer.team == connect.team && player.teamPlayer.player == connect.player) {
				//That player already exists!
				ConnectResponseMessage msg = new ConnectResponseMessage();
				msg.code = Code.FAIL;
				return msg;
			}
		}
		
		//They passed our tests, they can join
		UsernameClientData usernameClientData = new UsernameClientData(usernameDto, SimpAttributesContextHolder.currentAttributes().getSessionId());
		
		//Get the team palyer
		TeamPlayer teamPlayer = new TeamPlayer(connect.team, connect.player);
		
		//Store the player data
		Player player = new Player(usernameClientData, teamPlayer);
		player.playerVM = StartPlayerVM(player);
		
		//Add the player to a list
		players.add(player);
		
		//Log the event
		logger.info("user " + player.usernameClientData.username.usernameId + " joined" + " playing the game" + "\"" + game.gameId + "\"" + " with SessionID: " + "\"" + usernameClientData.sessionId + "\"");
		metricsService.logServerMessage(startLoggingGameInstanceDto.id, LogEventGamePlayerServerType.CONNECT, "user " + player.usernameClientData.username.usernameId + " joined" + " playing the game" + "\"" + game.gameId + "\"" + " with SessionID: " + "\"" + usernameClientData.sessionId + "\"");
		
		gameInstance.getPlayers().add(new GameInstancePlayer(usernameDto.tempPlayer, usernameDto.usernameId, usernameClientData.sessionId, ConnectionStatus.CONNECTED, ConnectionStatus.CONNECTED, GameStatus.GAME_RUNNING));
		gameInstance = gameInstanceRepository.save(gameInstance);
		
		ConnectResponseMessage msg = new ConnectResponseMessage();
		msg.team = teamPlayer.team;
		msg.player = teamPlayer.player;
		msg.code = Code.SUCCESS;
		return msg;
	}
	
	public void userDisconnect(int team, int playerNum) {
		
		for(Player player : players) {
			if(player.teamPlayer.team == team && player.teamPlayer.player == playerNum) {
				for(GameInstancePlayer gameInstancePlayer : gameInstance.getPlayers()) {
					if(gameInstancePlayer.getUsernameId().equals(player.usernameClientData.username.usernameId)) {
						gameInstancePlayer.setWebSocketConnectionStatus(ConnectionStatus.DISCONNECTED);
						gameInstancePlayer.setGameInstanceConnectionStatus(ConnectionStatus.DISCONNECTED);
						gameInstanceRepository.save(gameInstance);
					}
					
				}
				
				//Log the event
				logger.info("User " + player.usernameClientData.username.usernameId+ " is disconnecting... with SessionID: " + SimpAttributesContextHolder.currentAttributes().getSessionId());
				metricsService.logServerMessage(startLoggingGameInstanceDto.id, LogEventGamePlayerServerType.DISCONNECT, "User " + player.usernameClientData.username.usernameId+ " is disconnecting... with SessionID: " + SimpAttributesContextHolder.currentAttributes().getSessionId());
				
				//Stop the VM's thread
				player.playerVM.shutdown();

				//Remove the player
				players.remove(player);
				
				break;
			}
		}
	}
	
	private PlayerVMService StartMasterVM() {
		UsernameDto usernameDto = new UsernameDto();
		usernameDto.usernameId = "MasterVM";
		Player player = new Player(new UsernameClientData(usernameDto, null), new TeamPlayer(-1, -1));

		PlayerVMService service = context.getBean(PlayerVMService.class);
		service.setupVariables(this, player, transpiledGame.replace("running : true", "running : false"));
		service.start();
		return service;
	}
	
	private PlayerVMService StartPlayerVM(Player player) {
		
		PlayerVMService service = context.getBean(PlayerVMService.class);
		service.setupVariables(this, player, transpiledGame);
		service.start();
		
		return service;
	}
		
	public List<PlayerAvaliableMessage> getTeamsAndPlayers(String usernameId) {

		List<PlayerAvaliableMessage> teamPlayers = new ArrayList<PlayerAvaliableMessage>();
		
		if(!debugInstance) {
			for(Player player : players) {
				if(player.usernameClientData.username.usernameId.equals(usernameId)) {
					PlayerAvaliableMessage msg = new PlayerAvaliableMessage();
					msg.team = player.teamPlayer.team;
					msg.player = player.teamPlayer.player;
					if(playerNames != null) {
						for(Entry<String, String> entry : playerNames.entrySet()) {
							if(entry.getKey().equals("Team " + msg.team + " Player " + msg.player)) {
								msg.playerName = entry.getValue();
							}
						}
					}
					teamPlayers.add(msg);
					return teamPlayers;
				}
			}
		}
		
		for(int i = 0; i < game.teamCount; i++) {
			for(int n = 0; n < game.playersPerTeam; n++) {
				boolean alreadyExists = false;
				for(Player p : players) {
					if(p.teamPlayer.team == i && p.teamPlayer.player == n) {
						alreadyExists = true;
					}
				}
				if(!alreadyExists) {
					PlayerAvaliableMessage msg = new PlayerAvaliableMessage();
					msg.team = i;
					msg.player = n;
					if(playerNames != null) {
						for(Entry<String, String> entry : playerNames.entrySet()) {
							if(entry.getKey().equals("Team " + (msg.team + 1) + " Player " + (msg.player + 1))) {
								msg.playerName = entry.getValue();
							}
						}
					}
					teamPlayers.add(msg);
				}
			}
		}
		
		return teamPlayers;
	}
	
	public void shutdown() {
		for(Player player : players) {
			player.playerVM.shutdown();
		}
		running = false;
		gameInstance.setEnd(Timestamp.from(Instant.now()));
		gameInstance.setDuration(gameInstance.getEnd().getTime() - gameInstance.getStart().getTime());
		gameInstance.setGameEnded(true);
		gameInstanceRepository.delete(gameInstance);	
		logger.info("Game Instance: " + gameInstance.getGameInstanceId() + " stopped! No longer playing the game: " + game.gameId);
		metricsService.logServerMessage(startLoggingGameInstanceDto.id, LogEventGamePlayerServerType.GAME_INSTANCE_STOPPED, "Game Instance: " + gameInstance.getGameInstanceId() + " stopped! No longer playing the game: " + game.gameId);
		metricsService.stopLoggingGameInstance(startLoggingGameInstanceDto.id);
	}
	
	public List<Map<String,String>> getPlayerUserList() {
		List<Map<String, String>> playerUserList = new ArrayList<Map<String, String>>();
		for(Player player : players) {
			Map<String, String> map = new HashMap<String, String>();
			if(player.usernameClientData.username.tempPlayer) {
				map.put("key", "Team " + (player.teamPlayer.team + 1) + " Player " + (player.teamPlayer.player + 1) + " ( " + player.usernameClientData.username.usernameId + " ) (guest)");
			} else {
				map.put("key", "Team " + (player.teamPlayer.team + 1) + " Player " + (player.teamPlayer.player + 1) + " ( " + player.usernameClientData.username.usernameId + " )");
			}
			playerUserList.add(map);
		}
		return playerUserList;
	}
	
	@MessageMapping("/gameInstance/{gameInstanceId}/singleButtonPress/{usernameId}/{team}/{player}")
	public String singleButtonPress(@DestinationVariable int gameInstanceId, @DestinationVariable String usernameId, @DestinationVariable int team, @DestinationVariable int player, @RequestBody SingleButtonPressMessage msg) {
		for(GameInstanceService gameInstance : gameInstanceController.gameInstances) {
			if(gameInstance.getGameInstance().getGameInstanceId().equals(gameInstanceId)) {
				for(Player p : gameInstance.players) {
					if(p.teamPlayer.team == team && p.teamPlayer.player == player) {
						p.playerVM.unblock(msg);
					}
				}
			}
		}
		metricsService.logServerCommunication(gameInstanceId, team, player, DataDirection.SERVER_RECEIVE, Output.NONE, Input.SINGLE_BUTTON_PRESS, MetricsService.convertMessage(msg));
		return "";
	}
	
	@MessageMapping("/gameInstance/{gameInstanceId}/sequenceButtonPress/{usernameId}/{team}/{player}")
	public String sequenceButtonPress(@DestinationVariable int gameInstanceId, @DestinationVariable String usernameId, @DestinationVariable int team, @DestinationVariable int player, @RequestBody SequenceButtonPressMessage msg) {
		for(GameInstanceService gameInstance : gameInstanceController.gameInstances) {
			if(gameInstance.getGameInstance().getGameInstanceId().equals(gameInstanceId)) {
				for(Player p : gameInstance.players) {
					if(p.teamPlayer.team == team && p.teamPlayer.player == player) {
						p.playerVM.unblock(msg);
					}
				}
			}
		}
		metricsService.logServerCommunication(gameInstanceId, team, player, DataDirection.SERVER_RECEIVE, Output.NONE, Input.SEQUENCE_BUTTON_PRESS, MetricsService.convertMessage(msg));
		return "";
	}
	
	@MessageMapping("/gameInstance/{gameInstanceId}/keyboardInput/{usernameId}/{team}/{player}")
	public String keyboardInput(@DestinationVariable int gameInstanceId, @DestinationVariable String usernameId, @DestinationVariable int team, @DestinationVariable int player, @RequestBody KeyboardInputMessage msg) {
		for(GameInstanceService gameInstance : gameInstanceController.gameInstances) {
			if(gameInstance.getGameInstance().getGameInstanceId().equals(gameInstanceId)) {
				for(Player p : gameInstance.players) {
					if(p.teamPlayer.team == team && p.teamPlayer.player == player) {
						p.playerVM.unblock(msg);
					}
				}
			}
		}
		metricsService.logServerCommunication(gameInstanceId, team, player, DataDirection.SERVER_RECEIVE, Output.NONE, Input.KEYBOARD_INPUT, MetricsService.convertMessage(msg));
		return "";
	}
	
	@MessageMapping("/gameInstance/{gameInstanceId}/randomInput/{usernameId}/{team}/{player}")
	public String RandomInput(@DestinationVariable int gameInstanceId, @DestinationVariable String usernameId, @DestinationVariable int team, @DestinationVariable int player, @RequestBody EmptyMessage msg) {
		for(GameInstanceService gameInstance : gameInstanceController.gameInstances) {
			if(gameInstance.getGameInstance().getGameInstanceId().equals(gameInstanceId)) {
				for(Player p : gameInstance.players) {
					if(p.teamPlayer.team == team && p.teamPlayer.player == player) {
						p.playerVM.unblock(msg);
					}
				}
			}
		}
		metricsService.logServerCommunication(gameInstanceId, team, player, DataDirection.SERVER_RECEIVE, Output.NONE, Input.RANDOM, MetricsService.convertMessage(msg));
		return "";
	}
	
	public Player searchPlayers(String sessionId) {
		for(Player player : players) {
			if(player.usernameClientData.sessionId.equals(sessionId)) {
				return player;
			}
			
		}
		return null;
	}
	
	public void addMessage(IMessage message) {
		this.messages.add(message);
	}

	public GameDto getGame() {
		return game;
	}

	public UsernameDto getUsername() {
		return username;
	}

	public GameInstance getGameInstance() {
		return gameInstance;
	}
	
	public PlayerVMService getMasterPlayerVMService() {
		return masterPlayerVMService;
	}

	public StartLoggingGameInstanceDto getStartLoggingGameInstanceDto() {
		return startLoggingGameInstanceDto;
	}

	public void setStartLoggingGameInstanceDto(StartLoggingGameInstanceDto startLoggingGameInstanceDto) {
		this.startLoggingGameInstanceDto = startLoggingGameInstanceDto;
	}

	public Map<String, String> getPlayerNames() {
		return playerNames;
	}
	
	public boolean hasPlayerNames() {
		return !getPlayerNames().isEmpty();
	}

}
