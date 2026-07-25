package org.wlcp.wlcpgameserver.service;

import java.util.concurrent.CompletableFuture;

import javax.script.ScriptEngine;

import org.wlcp.wlcpgameserver.datamodel.master.GameInstance;
import org.wlcp.wlcpgameserver.dto.messages.IMessage;
import org.wlcp.wlcpgameserver.feignclient.dto.LogEventGamePlayerCommunicationDto.DataDirection;
import org.wlcp.wlcpgameserver.feignclient.dto.LogEventGamePlayerCommunicationDto.Input;
import org.wlcp.wlcpgameserver.feignclient.dto.LogEventGamePlayerCommunicationDto.Output;
import org.wlcp.wlcpgameserver.feignclient.dto.LogEventGamePlayerServerEventDto.Event;
import org.wlcp.wlcpgameserver.feignclient.dto.LogEventGamePlayerServerEventDto.Type;
import org.wlcp.wlcpgameserver.feignclient.dto.LogEventGamePlayerServerMessageDto.LogEventGamePlayerServerType;
import org.wlcp.wlcpgameserver.feignclient.dto.StartLoggingGameInstanceDto;
import org.wlcp.wlcpgameserver.model.Player;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface MetricsService {
	
	void logServerEvent(Integer logEventGameInstanceId, Player player, Type type, Event event, ScriptEngine scriptEngine);
	void logServerMessage(Integer logEventGameInstanceId, LogEventGamePlayerServerType logEventGamePlayerServerType, String message);
	void logServerCommunication(Integer logEventGameInstanceId, int team, int player, DataDirection dataDirection, Output output, Input input, String message);
	StartLoggingGameInstanceDto startLoggingGameInstance(String gameId, String usernameId, boolean debugInstance);
	void stopLoggingGameInstance(Integer logEventGameInstanceId);
	boolean isEnabled();
	
	public static String convertMessage(IMessage msg) {
		try {
			return new ObjectMapper().writeValueAsString(msg);
		} catch (Exception e) {
			
		}
		return "";
	}

}
