package org.wlcp.wlcpgameserver.datamodel.master;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class GameInstancePlayer {
	
	@Column(name = "TEMP_PLAYER")
	private Boolean tempPlayer;
	
	@Column(name = "USERNAME_ID")
	private String usernameId;
	
	public GameInstancePlayer() {
		super();
	}

	public GameInstancePlayer(Boolean tempPlayer, String usernameId) {
		super();
		this.tempPlayer = tempPlayer;
		this.usernameId = usernameId;
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

}
