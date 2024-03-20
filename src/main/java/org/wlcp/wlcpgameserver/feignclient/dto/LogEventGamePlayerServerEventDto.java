package org.wlcp.wlcpgameserver.feignclient.dto;

public class LogEventGamePlayerServerEventDto extends LogEventGamePlayerDto {
	
	public enum Type {
		STATE,
		TRANSITION
	}
	
	public enum Event {
		ENTER,
		EXIT
	}
	
	public Type type;
	public Event event;
	public Integer state;
	public Integer oldState;
	public Boolean running;
	public String stateName;

	public LogEventGamePlayerServerEventDto(LogEventGamePlayerType logEventGamePlayerType, Integer logEventGameInstance,
			Integer team, Integer player, Type type, Event event, Integer state, Integer oldState, Boolean running,
			String stateName) {
		super(logEventGamePlayerType, logEventGameInstance, team, player);
		this.type = type;
		this.event = event;
		this.state = state;
		this.oldState = oldState;
		this.running = running;
		this.stateName = stateName;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Event getEvent() {
		return event;
	}

	public void setEvent(Event event) {
		this.event = event;
	}

	public Integer getState() {
		return state;
	}

	public void setState(Integer state) {
		this.state = state;
	}

	public Integer getOldState() {
		return oldState;
	}

	public void setOldState(Integer oldState) {
		this.oldState = oldState;
	}

	public Boolean getRunning() {
		return running;
	}

	public void setRunning(Boolean running) {
		this.running = running;
	}

	public String getStateName() {
		return stateName;
	}

	public void setStateName(String stateName) {
		this.stateName = stateName;
	}

}
