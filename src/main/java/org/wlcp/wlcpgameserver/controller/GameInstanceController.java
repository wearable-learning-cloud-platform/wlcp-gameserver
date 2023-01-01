package org.wlcp.wlcpgameserver.controller;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.wlcp.wlcpgameserver.datamodel.master.GameInstance;
import org.wlcp.wlcpgameserver.datamodel.master.GameInstancePlayer;
import org.wlcp.wlcpgameserver.dto.GameDto;
import org.wlcp.wlcpgameserver.dto.StartDebugGameInstanceDto;
import org.wlcp.wlcpgameserver.dto.StartGameInstanceDto;
import org.wlcp.wlcpgameserver.dto.StopGameInstanceDto;
import org.wlcp.wlcpgameserver.dto.UsernameDto;
import org.wlcp.wlcpgameserver.dto.messages.ConnectRequestMessage;
import org.wlcp.wlcpgameserver.dto.messages.DisconnectResponseMessage;
import org.wlcp.wlcpgameserver.dto.messages.IMessage;
import org.wlcp.wlcpgameserver.dto.messages.PlayerAvaliableMessage;
import org.wlcp.wlcpgameserver.feignclient.GameFeignClient;
import org.wlcp.wlcpgameserver.feignclient.UsernameFeignClient;
import org.wlcp.wlcpgameserver.model.Player;
import org.wlcp.wlcpgameserver.repository.GameInstanceRepository;
import org.wlcp.wlcpgameserver.service.impl.GameInstanceService;

@Controller
@RequestMapping("/gameInstanceController")
public class GameInstanceController {
	
	@Autowired
	ApplicationContext context;
	
	@Autowired
	private GameFeignClient gameFeignClient;
	
	@Autowired
	private UsernameFeignClient usernameFeignClient;
	
	@Autowired
	private GameInstanceRepository gameInstanceRepository;
	
	@Autowired
	private SimpMessagingTemplate messageTemplate;
	
	@Value("${security.jwt-token}")
	private String jwtToken;
	
	public CopyOnWriteArrayList<GameInstanceService> gameInstances = new CopyOnWriteArrayList<GameInstanceService>();
	
	@GetMapping("/gameInstances")
	public ResponseEntity<List<GameInstance>> getGameInstances(@RequestParam("usernameId") String usernameId) {
		return new ResponseEntity<List<GameInstance>>(gameInstanceRepository.findByUsernameIdAndDebugInstance(usernameId, false), HttpStatus.OK);
	}
	
	@GetMapping("/allGameInstances")
	public ResponseEntity<List<GameInstance>> getAllGameInstances() {
		return new ResponseEntity<List<GameInstance>>(gameInstanceRepository.findAll(), HttpStatus.OK);
	}
	
	@PostMapping("/startGameInstance")
	public ResponseEntity<Object> startGameInstance(@RequestBody StartGameInstanceDto startGameInstanceDto) throws InterruptedException {
		GameDto gameDto = gameFeignClient.getGame(startGameInstanceDto.gameId, jwtToken);
		UsernameDto usernameDto = usernameFeignClient.getUsername(startGameInstanceDto.usernameId, jwtToken);
		if(gameDto != null && usernameDto != null) {
			GameInstanceService service = context.getBean(GameInstanceService.class);
			service.setupVariables(gameDto, usernameDto, false, false);
			service.start();
			gameInstances.add(service);
			service.done.await();
			return new ResponseEntity<Object>(service.getGameInstance(), HttpStatus.OK);
		} else {
			return new ResponseEntity<Object>("The game " + startGameInstanceDto.gameId + " username " + startGameInstanceDto.usernameId + " does not exist, so an insance could not be started!", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@PostMapping("/startDebugGameInstance")
	public ResponseEntity<Integer> startDebugGameInstance(@RequestBody StartDebugGameInstanceDto startDebugGameInstance) throws InterruptedException {
		GameDto gameDto = null;
		if(startDebugGameInstance.archivedGame) {
			gameDto = gameFeignClient.getArchivedGame(startDebugGameInstance.gameId, jwtToken);
		} else {
			gameDto = gameFeignClient.getGame(startDebugGameInstance.gameId, jwtToken);
		}
		UsernameDto usernameDto = usernameFeignClient.getUsername(startDebugGameInstance.usernameId, jwtToken);
		if(gameDto != null && usernameDto != null) {
			List<GameInstance> foundGameInstances = null;
			if(startDebugGameInstance.restart == false) {
				if((foundGameInstances = gameInstanceRepository.findByUsernameIdAndDebugInstance(usernameDto.usernameId, true)).size() > 0) {
					for(GameInstanceService instance : gameInstances) {
						if(instance.getGameInstance().getGameInstanceId().equals(foundGameInstances.get(0).getGameInstanceId())) {
							return ResponseEntity.status(HttpStatus.OK).body(instance.getGameInstance().getGameInstanceId());
						}
					}
				}
			}
			if((foundGameInstances = gameInstanceRepository.findByUsernameIdAndDebugInstance(usernameDto.usernameId, true)).size() > 0) {
				for(GameInstanceService instance : gameInstances) {
					if(instance.getGameInstance().getGameInstanceId().equals(foundGameInstances.get(0).getGameInstanceId())) {
						instance.shutdown();
						gameInstances.remove(instance);
						break;
					}
				}
				GameInstanceService service = context.getBean(GameInstanceService.class);
				service.setupVariables(gameDto, usernameDto, true, startDebugGameInstance.archivedGame);
				service.start();
				gameInstances.add(service);
				service.done.await();
				return ResponseEntity.status(HttpStatus.OK).body(service.getGameInstance().getGameInstanceId());
			} else {
				GameInstanceService service = context.getBean(GameInstanceService.class);
				service.setupVariables(gameDto, usernameDto, true, startDebugGameInstance.archivedGame);
				service.start();
				gameInstances.add(service);
				service.done.await();
				return ResponseEntity.status(HttpStatus.OK).body(service.getGameInstance().getGameInstanceId());
			}
		}
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(-1);
	}
	
	@PostMapping("/stopGameInstance")
	public ResponseEntity<Object> stopGameInstance(@RequestBody StopGameInstanceDto stopGameInstanceDto) {
		if(gameInstanceRepository.existsById(stopGameInstanceDto.gameInstanceId)) {
			for(GameInstanceService instance : gameInstances) {
				if(instance.getGameInstance().getGameInstanceId().equals(stopGameInstanceDto.gameInstanceId)) {
					instance.shutdown();
					gameInstances.remove(instance);
					return new ResponseEntity<Object>(instance.getGameInstance(), HttpStatus.OK);
				}
			}
			return new ResponseEntity<Object>("The game instance: " + stopGameInstanceDto.gameInstanceId + " does not exist, so it could not be stopped!", HttpStatus.INTERNAL_SERVER_ERROR);
		} else {
			return new ResponseEntity<Object>("The game instance: " + stopGameInstanceDto.gameInstanceId + " does not exist, so it could not be stopped!", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@GetMapping("/playersAvaliable/{gameInstanceId}/{usernameId}")
	public ResponseEntity<List<PlayerAvaliableMessage>> playersAvailable(@PathVariable int gameInstanceId, @PathVariable String usernameId) {
		if(usernameId == null) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null); }
		UsernameDto usernameDto = new UsernameDto();
		usernameDto.usernameId = usernameId;
		for(GameInstanceService gameInstance : gameInstances) {
			if(gameInstance.getGameInstance().getGameInstanceId().equals(gameInstanceId)) {
				for(GameInstancePlayer player : gameInstance.getGameInstance().getPlayers()) {
					if(player.getUsernameId().equals(usernameDto.usernameId)) {
						List<PlayerAvaliableMessage> msgs = gameInstance.getTeamsAndPlayers(usernameId);
						for(PlayerAvaliableMessage msg : msgs) {
							msg.type = PlayerAvaliableMessage.Type.USERNAME_EXISTS;
						}
						return ResponseEntity.status(HttpStatus.OK).body(msgs);
					}
				}
				return ResponseEntity.status(HttpStatus.OK).body(gameInstance.getTeamsAndPlayers(usernameId));
			}
		}
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
	}
	
	@GetMapping("/checkDebugInstanceRunning/{usernameId}")
	public ResponseEntity<Boolean> checkDebugInstanceRunning(@PathVariable String usernameId)  {
		UsernameDto usernameDto = usernameFeignClient.getUsername(usernameId, jwtToken);
		if(usernameDto != null) {
			if(gameInstanceRepository.findByUsernameIdAndDebugInstance(usernameDto.usernameId, true).size() > 0) {
				return ResponseEntity.status(HttpStatus.OK).body(true);
			}
			return ResponseEntity.status(HttpStatus.OK).body(false);
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
		}
	}
	
	@GetMapping("getPlayerUserMap/{gameInstanceId}")
	public ResponseEntity<List<Map<String, String>>> getPlayerUserMap(@PathVariable int gameInstanceId) {
		for(GameInstanceService gameInstance : gameInstances) {
			if(gameInstance.getGameInstance().getGameInstanceId().equals(gameInstanceId)) {
				return ResponseEntity.status(HttpStatus.OK).body(gameInstance.getPlayerUserList());
			}
		}
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
	}
	
	@MessageMapping("/gameInstance/{gameInstanceId}/connectToGameInstance/{usernameId}/{team}/{player}")
	public IMessage connectToGameInstance(@DestinationVariable int gameInstanceId, @DestinationVariable String usernameId, @DestinationVariable int team, @DestinationVariable int player) {
		for(GameInstanceService instance : gameInstances) {
			if(instance.getGameInstance().getGameInstanceId().equals(gameInstanceId)) {
				ConnectRequestMessage msg = new ConnectRequestMessage(); 
				msg.gameInstanceId = gameInstanceId;
				msg.usernameId = usernameId;
				msg.team = team;
				msg.player = player;
				messageTemplate.convertAndSend("/subscription/connectionResult/" + gameInstanceId + "/" + usernameId + "/" + team + "/" + player, instance.userConnect(msg));
			}
		}
		return null;
	}
	
	@MessageMapping("/gameInstance/{gameInstanceId}/disconnectFromGameInstance/{usernameId}/{team}/{player}")
	public IMessage disconnectFromGameInstance(@DestinationVariable int gameInstanceId, @DestinationVariable String usernameId, @DestinationVariable int team, @DestinationVariable int player) {
		for(GameInstanceService instance : gameInstances) {
			if(instance.getGameInstance().getGameInstanceId().equals(gameInstanceId)) {
			   instance.userDisconnect(team, player);
			   messageTemplate.convertAndSend("/subscription/disconnectionResult/" + gameInstanceId + "/" + usernameId + "/" + team + "/" + player, new DisconnectResponseMessage());
			}
		}
		return null;
	}

}
