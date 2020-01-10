package org.wlcp.wlcpgameserver.controller;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.wlcp.wlcpgameserver.dto.GameDto;
import org.wlcp.wlcpgameserver.dto.StartGameInstanceDto;
import org.wlcp.wlcpgameserver.dto.UsernameDto;
import org.wlcp.wlcpgameserver.dto.messages.ConnectRequestMessage;
import org.wlcp.wlcpgameserver.dto.messages.DisconnectResponseMessage;
import org.wlcp.wlcpgameserver.dto.messages.IMessage;
import org.wlcp.wlcpgameserver.dto.messages.PlayerAvaliableMessage;
import org.wlcp.wlcpgameserver.feignclients.GameFeignClient;
import org.wlcp.wlcpgameserver.feignclients.UsernameFeignClient;
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
	private SimpMessagingTemplate messageTemplate;
	
	public CopyOnWriteArrayList<GameInstanceService> gameInstances = new CopyOnWriteArrayList<GameInstanceService>();
	
//	@GetMapping(value="/startGameInstance/{gameId}/{usernameId}")
//	public ResponseEntity<String> startGameInstance(@PathVariable String gameId, @PathVariable String usernameId) {
//		if(gameRepository.existsById(gameId) && usernameRepository.existsById(usernameId)) {
//			GameInstanceService service = context.getBean(GameInstanceService.class);
//			service.setupVariables(gameRepository.getOne(gameId), usernameRepository.getOne(usernameId), false);
//			service.start();
//			gameInstances.add(service);
//			return ResponseEntity.status(HttpStatus.OK).body("");
//		} else {
//			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("The game " + gameId + " username " + usernameId + " does not exist, so an insance could not be started!");
//		}
//	}
	
	@GetMapping(value="/test")
	public void test() {
		GameDto game = gameFeignClient.getGame("private");
		game.toString();
	}
	
	@PostMapping(value="/startGameInstance")
	public ResponseEntity<String> startGameInstance(@RequestBody StartGameInstanceDto startGameInstanceDto) {
		GameDto gameDto = gameFeignClient.getGame(startGameInstanceDto.gameId);
		UsernameDto usernameDto = usernameFeignClient.getUsername(startGameInstanceDto.usernameId);
		if(gameDto != null && usernameDto != null) {
			GameInstanceService service = context.getBean(GameInstanceService.class);
			service.setupVariables(gameDto, usernameDto, false);
			service.start();
			gameInstances.add(service);
			return ResponseEntity.status(HttpStatus.OK).body("");
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("The game " + startGameInstanceDto.gameId + " username " + startGameInstanceDto.usernameId + " does not exist, so an insance could not be started!");
		}
	}
	
	@GetMapping("/playersAvaliable/{gameInstanceId}/{usernameId}")
	public ResponseEntity<List<PlayerAvaliableMessage>> playersAvailable(@PathVariable int gameInstanceId, @PathVariable String usernameId) {
		UsernameDto usernameDto = usernameFeignClient.getUsername(usernameId);
		if(usernameDto == null) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null); }
		for(GameInstanceService gameInstance : gameInstances) {
			if(gameInstance.getGameInstance().getGameInstanceId().equals(gameInstanceId)) {
				return ResponseEntity.status(HttpStatus.OK).body(gameInstance.getTeamsAndPlayers(usernameId));
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
