package org.wlcp.wlcpgameserver.feignclient.dto;

public class StopLoggingGameInstanceDto {
	
	public Integer logEventGameInstanceId;
	
	public StopLoggingGameInstanceDto() {
		
	}

	public StopLoggingGameInstanceDto(Integer logEventGameInstanceId) {
		super();
		this.logEventGameInstanceId = logEventGameInstanceId;
	}

	public Integer getLogEventGameInstanceId() {
		return logEventGameInstanceId;
	}

	public void setLogEventGameInstanceId(Integer logEventGameInstanceId) {
		this.logEventGameInstanceId = logEventGameInstanceId;
	}
   
}
