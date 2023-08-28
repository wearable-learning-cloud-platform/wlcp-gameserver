package org.wlcp.wlcpgameserver.service.impl;

import javax.script.ScriptEngine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.wlcp.wlcpgameserver.datamodel.master.GameInstance;
import org.wlcp.wlcpgameserver.feignclient.MetricsFeignClient;
import org.wlcp.wlcpgameserver.feignclient.dto.LogEventGamePlayerCommunicationDto;
import org.wlcp.wlcpgameserver.feignclient.dto.LogEventGamePlayerCommunicationDto.DataDirection;
import org.wlcp.wlcpgameserver.feignclient.dto.LogEventGamePlayerCommunicationDto.Input;
import org.wlcp.wlcpgameserver.feignclient.dto.LogEventGamePlayerCommunicationDto.Output;
import org.wlcp.wlcpgameserver.feignclient.dto.LogEventGamePlayerDto.LogEventGamePlayerType;
import org.wlcp.wlcpgameserver.feignclient.dto.LogEventGamePlayerServerEventDto;
import org.wlcp.wlcpgameserver.feignclient.dto.LogEventGamePlayerServerEventDto.Event;
import org.wlcp.wlcpgameserver.feignclient.dto.LogEventGamePlayerServerEventDto.Type;
import org.wlcp.wlcpgameserver.feignclient.dto.LogEventGamePlayerServerMessageDto;
import org.wlcp.wlcpgameserver.feignclient.dto.LogEventGamePlayerServerMessageDto.LogEventGamePlayerServerType;
import org.wlcp.wlcpgameserver.feignclient.dto.StartLoggingGameInstanceDto;
import org.wlcp.wlcpgameserver.feignclient.dto.StopLoggingGameInstanceDto;
import org.wlcp.wlcpgameserver.model.Player;
import org.wlcp.wlcpgameserver.service.MetricsService;

import jdk.nashorn.api.scripting.JSObject;

@Service
public class MetricsServiceImpl implements MetricsService {
	
	private final boolean isEnabled = true;
	
	@Autowired
	private MetricsFeignClient metricsFeignClient;
	
	@Override
	@Async
	public void logServerEvent(Integer logEventGameInstanceId, Player player, Type type, Event event, ScriptEngine scriptEngine) {
		logEvent(logEventGameInstanceId, player, type, event, scriptEngine);
	}

	@Override
	@Async
	public void logServerMessage(Integer logEventGameInstanceId, LogEventGamePlayerServerType logEventGamePlayerServerType, String message) {
		logMessage(logEventGameInstanceId, logEventGamePlayerServerType, message);
	}

	@Override
	@Async
	public void logServerCommunication(Integer logEventGameInstanceId, int team, int player, DataDirection dataDirection, Output output, Input input, String message) {
		logCommunication(logEventGameInstanceId, team, player, dataDirection, output, input, message);
	}
	
	private void logEvent(Integer logEventGameInstanceId, Player player, Type type, Event event, ScriptEngine scriptEngine) {
		if(!isEnabled()) { return; }
		try {
			metricsFeignClient.logEventGamePlayer(new LogEventGamePlayerServerEventDto(LogEventGamePlayerType.SERVER_EVENT, logEventGameInstanceId, player.teamPlayer.team, player.teamPlayer.player, type, event, (int) ((JSObject)scriptEngine.get("FSMGame")).getMember("state"), (int) ((JSObject)scriptEngine.get("FSMGame")).getMember("oldState") - 1, (boolean) ((JSObject)scriptEngine.get("FSMGame")).getMember("running"), (String) scriptEngine.eval(String.format("Object.keys(states)[%d]", (int) ((JSObject)scriptEngine.get("FSMGame")).getMember("state")))));
		} catch (Exception e) {
			
		}
	}
	
	private void logMessage(Integer logEventGameInstanceId, LogEventGamePlayerServerType logEventGamePlayerServerType, String message) {
		if(!isEnabled()) { return; }
		try {
			metricsFeignClient.logEventGamePlayer(new LogEventGamePlayerServerMessageDto(LogEventGamePlayerType.SERVER_MESSAGE, logEventGameInstanceId, 0, 0, logEventGamePlayerServerType, message));

		} catch (Exception e) {
			
		}
	}
	
	private void logCommunication(Integer logEventGameInstanceId, int team, int player, DataDirection dataDirection, Output output, Input input, String message) {
		if(!isEnabled()) { return; }
		try {
			metricsFeignClient.logEventGamePlayer(new LogEventGamePlayerCommunicationDto(LogEventGamePlayerType.COMMUNICATION, logEventGameInstanceId, team, player, dataDirection, output, input, message));
		} catch (Exception e) {
			
		}
	}
	
	@Override
	public StartLoggingGameInstanceDto startLoggingGameInstance(String gameId, String usernameId, boolean debugInstance) {
		if(!isEnabled()) { return new StartLoggingGameInstanceDto("", "", false); }
		try {
			return metricsFeignClient.startLoggingGameInstance(new StartLoggingGameInstanceDto(gameId, usernameId, debugInstance));
		} catch (Exception e) {
			
		}
		return null;
	}
	
	@Override
	public void stopLoggingGameInstance(Integer logEventGameInstanceId) {
		if(!isEnabled()) { return; }
		try {
			metricsFeignClient.stopLoggingGameInstance(new StopLoggingGameInstanceDto(logEventGameInstanceId));
		} catch (Exception e) {
			
		}
	}

	@Override
	public boolean isEnabled() {
		return isEnabled;
	}

}
