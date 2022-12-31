package org.wlcp.wlcpgameserver.datamodel.master;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.wlcp.wlcpgameserver.datamodel.enums.ConnectionStatus;

@Embeddable
public class GameInstancePlayer {
	
	@Column(name = "TEMP_PLAYER")
	private Boolean tempPlayer;
	
	@Column(name = "USERNAME_ID")
	private String usernameId;
	
	@Column(name = "SESSION_ID")
	private String sessionId;
	
	@Column(name = "WEB_SOCKET_CONNECTION_STATUS")
	@Enumerated(EnumType.ORDINAL)
    private ConnectionStatus webSocketConnectionStatus;
	
	@Column(name = "GAME_INSTANCE_CONNECTION_STATUS")
	@Enumerated(EnumType.ORDINAL)
	private ConnectionStatus gameInstanceConnectionStatus;
	
	public GameInstancePlayer() {
		super();
	}

	public GameInstancePlayer(Boolean tempPlayer, String usernameId, String sessionId, ConnectionStatus webSocketConnectionStatus, ConnectionStatus gameInstanceConnectionStatus) {
		super();
		this.tempPlayer = tempPlayer;
		this.usernameId = usernameId;
		this.sessionId = sessionId;
		this.webSocketConnectionStatus = webSocketConnectionStatus;
		this.gameInstanceConnectionStatus = gameInstanceConnectionStatus;
	}

	public Boolean getTempPlayer() {
		return tempPlayer;
	}

	public void setTempPlayer(Boolean tempPlayer) {
		this.tempPlayer = tempPlayer;
	}

	public String getUsernameId() {
		return usernameId;
	}

	public void setUsernameId(String usernameId) {
		this.usernameId = usernameId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public ConnectionStatus getWebSocketConnectionStatus() {
		return webSocketConnectionStatus;
	}

	public void setWebSocketConnectionStatus(ConnectionStatus webSocketConnectionStatus) {
		this.webSocketConnectionStatus = webSocketConnectionStatus;
	}

	public ConnectionStatus getGameInstanceConnectionStatus() {
		return gameInstanceConnectionStatus;
	}

	public void setGameInstanceConnectionStatus(ConnectionStatus gameInstanceConnectionStatus) {
		this.gameInstanceConnectionStatus = gameInstanceConnectionStatus;
	}

}
