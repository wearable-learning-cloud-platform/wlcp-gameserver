package org.wlcp.wlcpgameserver.model;

import org.wlcp.wlcpgameserver.dto.UsernameDto;

public class UsernameClientData {

	public UsernameDto username;
	public String sessionId;
	
	public UsernameClientData(UsernameDto username, String sessionId) {
		this.username = username;
		this.sessionId = sessionId;
	}
}
