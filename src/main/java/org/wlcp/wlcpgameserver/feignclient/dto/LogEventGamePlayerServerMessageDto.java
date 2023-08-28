package org.wlcp.wlcpgameserver.feignclient.dto;

public class LogEventGamePlayerServerMessageDto extends LogEventGamePlayerDto {
	
	public enum LogEventGamePlayerServerType {
		
		GAME_INSTANCE_STARTED,
		GAME_INSTANCE_STOPPED,
		CONNECT,
		RECONNECT,
		DISCONNECT

	}
	
	public LogEventGamePlayerServerType logEventGamePlayerServerType;
	public String message;
	
	public LogEventGamePlayerServerMessageDto(LogEventGamePlayerType logEventGamePlayerType, Integer logEventGameInstance, Integer team,
			Integer player, LogEventGamePlayerServerType logEventGamePlayerServerType, String message) {
		super(logEventGamePlayerType, logEventGameInstance, team, player);
		this.logEventGamePlayerServerType = logEventGamePlayerServerType;
		this.message = message;
	}

	public LogEventGamePlayerServerType getLogEventGamePlayerServerType() {
		return logEventGamePlayerServerType;
	}

	public void setLogEventGamePlayerServerType(LogEventGamePlayerServerType logEventGamePlayerServerType) {
		this.logEventGamePlayerServerType = logEventGamePlayerServerType;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
