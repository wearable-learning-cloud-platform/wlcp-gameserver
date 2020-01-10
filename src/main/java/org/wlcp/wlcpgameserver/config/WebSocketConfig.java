package org.wlcp.wlcpgameserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
	
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/wlcpGameServer-ws/{gameInstanceId}").setAllowedOrigins("*");
		registry.addEndpoint("/wlcpGameServer-js/{gameInstanceId}").setAllowedOrigins("*").withSockJS();
	}

	
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.setApplicationDestinationPrefixes("/app");
		config.enableSimpleBroker("/subscription");
	}
}
