package org.wlcp.wlcpgameserver.service.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.wlcp.wlcpgameserver.controller.GameInstanceController;
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
import org.wlcp.wlcpgameserver.model.Player;
import org.wlcp.wlcpgameserver.model.TeamPlayer;
import org.wlcp.wlcpgameserver.model.UsernameClientData;
import org.wlcp.wlcpgameserver.repository.GameInstanceRepository;
import org.wlcp.wlcpgameserver.security.SecurityConstants;

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
	private GameInstanceController gameInstanceController;
	
	@Autowired
	private ApplicationContext context;
	
	private GameDto game;
	private UsernameDto username;
	private GameInstance gameInstance;
	private boolean debugInstance;
	private boolean archivedGame;
	
	private String transpiledGame;
	
	private CopyOnWriteArrayList<IMessage> messages = new CopyOnWriteArrayList<IMessage>();
	private CopyOnWriteArrayList<Player> players = new CopyOnWriteArrayList<Player>();
	
	private boolean running = true;
	
	public void setupVariables(GameDto game, UsernameDto username, boolean debugInstance, boolean archivedGame) {
		this.game = game;
		this.username = username;
		this.debugInstance = debugInstance;
		this.archivedGame = archivedGame;
	}
	
	@Override
	public void run() {
		setup();
		while(running) {
			for(IMessage message : messages) {
				handleMessage(message);
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
	}
	
	public ConnectResponseMessage userConnect(ConnectRequestMessage connect) {
		
		//Get the user from the db
		UsernameDto usernameDto = usernameFeignClient.getUsername(connect.usernameId, SecurityConstants.JWT_TOKEN);
		
		if(usernameDto == null) {
			usernameDto = new UsernameDto();
			usernameDto.usernameId = connect.usernameId;
			usernameDto.tempPlayer = true;
		}
		
		//Check to make sure the player doesnt already exist in the game (for reconnect)
		if(!debugInstance) {
			for(Player player : players) {
				if(player.usernameClientData.username.usernameId.equals(usernameDto.usernameId)) {
					//User already exists in the game, maybe they are trying to reconnect?
					player.playerVM.reconnect();
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
		UsernameClientData usernameClientData = new UsernameClientData(usernameDto);
		
		//Get the team palyer
		TeamPlayer teamPlayer = new TeamPlayer(connect.team, connect.player);
		
		//Store the player data
		Player player = new Player(usernameClientData, teamPlayer);
		player.playerVM = StartPlayerVM(player);
		
		//Add the player to a list
		players.add(player);
		
		//Log the event
		logger.info("user " + player.usernameClientData.username.usernameId + " joined" + " playing the game" + "\"" + game.gameId + "\"");
		
		gameInstance.getPlayers().add(new GameInstancePlayer(usernameDto.tempPlayer, usernameDto.usernameId));
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
				
				//Log the event
				logger.info("User " + player.usernameClientData.username.usernameId+ " is disconnecting...");
				
				//Stop the VM's thread
				player.playerVM.shutdown();

				//Remove the player
				players.remove(player);
				
				break;
			}
		}
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
		gameInstanceRepository.save(gameInstance);	
		logger.info("Game Instance: " + gameInstance.getGameInstanceId() + " stopped! No longer playing the game: " + game.gameId);
	}
	
	public List<Map<String,String>> getPlayerUserList() {
		List<Map<String, String>> playerUserList = new ArrayList<Map<String, String>>();
		for(Player player : players) {
			Map<String, String> map = new HashMap<String, String>();
			map.put("key", "Team " + (player.teamPlayer.team + 1) + " Player " + (player.teamPlayer.player + 1) + " ( " + player.usernameClientData.username.usernameId + " )");
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
		return "";
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

}
