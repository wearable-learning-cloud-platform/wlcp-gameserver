package org.wlcp.wlcpgameserver.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.DefaultManagedTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.wlcp.wlcpgameserver.controller.GameInstanceController;
import org.wlcp.wlcpgameserver.datamodel.enums.ConnectionStatus;
import org.wlcp.wlcpgameserver.datamodel.master.GameInstancePlayer;
import org.wlcp.wlcpgameserver.model.Player;
import org.wlcp.wlcpgameserver.repository.GameInstanceRepository;
import org.wlcp.wlcpgameserver.service.impl.GameInstanceService;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
	
	@Autowired
	private ApplicationContext context;
	
	@Autowired
	private GameInstanceRepository gameInstanceRepository;
	
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/wlcpGameServer-ws/{gameInstanceId}").setAllowedOrigins("*");
		registry.addEndpoint("/wlcpGameServer-js/{gameInstanceId}").setAllowedOrigins("*").withSockJS();
	}

	
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.setApplicationDestinationPrefixes("/app");
		config.enableSimpleBroker("/subscription").setTaskScheduler(new DefaultManagedTaskScheduler()).setHeartbeatValue(new long[] {10000, 10000});
	}
	
	@EventListener
	private void afterConnectionEstablished(SessionConnectEvent event) {

	}
	
	@EventListener
	private void handleSessionDisconnect(SessionDisconnectEvent event) {
		GameInstanceController gameInstanceController = context.getBean(GameInstanceController.class);
		for(GameInstanceService gameInstanceService : gameInstanceController.gameInstances) {
			Player player = gameInstanceService.searchPlayers(event.getSessionId());
			for(GameInstancePlayer p : gameInstanceService.getGameInstance().getPlayers()) {
				if(p.getUsernameId().equals(player.usernameClientData.username.usernameId)) {
					p.setWebSocketConnectionStatus(ConnectionStatus.DISCONNECTED);
					gameInstanceRepository.save(gameInstanceService.getGameInstance());
				}
			}
		}
	}
	
}
