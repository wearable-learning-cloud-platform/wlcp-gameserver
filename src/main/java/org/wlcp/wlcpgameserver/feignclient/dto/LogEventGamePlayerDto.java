package org.wlcp.wlcpgameserver.feignclient.dto;

public class LogEventGamePlayerDto {
	
	public enum LogEventGamePlayerType {

		CLIENT_MESSAGE,
		SERVER_MESSAGE,
		SERVER_EVENT,
		COMMUNICATION
		
	}
	
	public static class LogEventGameInstance {
		public Integer id;
		
		public LogEventGameInstance() {
			
		}
		
		public LogEventGameInstance(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
		
	}
	
	public LogEventGamePlayerType logEventGamePlayerType;
	public LogEventGameInstance logEventGameInstance;
	public Integer team;
	public Integer player;
	
	public LogEventGamePlayerDto() {
		
	}
	
	public LogEventGamePlayerDto(LogEventGamePlayerType logEventGamePlayerType, Integer logEventGameInstance, Integer team,
			Integer player) {
		super();
		this.logEventGamePlayerType = logEventGamePlayerType;
		this.logEventGameInstance = new LogEventGameInstance(logEventGameInstance);
		this.team = team;
		this.player = player;
	}

	public LogEventGamePlayerDto(LogEventGamePlayerType logEventGamePlayerType, LogEventGameInstance logEventGameInstance, Integer team,
			Integer player) {
		super();
		this.logEventGamePlayerType = logEventGamePlayerType;
		this.logEventGameInstance = logEventGameInstance;
		this.team = team;
		this.player = player;
	}

	public LogEventGamePlayerType getLogEventGamePlayerType() {
		return logEventGamePlayerType;
	}

	public void setLogEventGamePlayerType(LogEventGamePlayerType logEventGamePlayerType) {
		this.logEventGamePlayerType = logEventGamePlayerType;
	}

	public LogEventGameInstance getLogEventGameInstance() {
		return logEventGameInstance;
	}

	public void setLogEventGameInstance(LogEventGameInstance logEventGameInstance) {
		this.logEventGameInstance = logEventGameInstance;
	}

	public Integer getTeam() {
		return team;
	}

	public void setTeam(Integer team) {
		this.team = team;
	}

	public Integer getPlayer() {
		return player;
	}

	public void setPlayer(Integer player) {
		this.player = player;
	}

}
