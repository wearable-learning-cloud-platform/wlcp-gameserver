package org.wlcp.wlcpgameserver.model;

import org.wlcp.wlcpgameserver.service.impl.PlayerVMService;

public class Player {
	public UsernameClientData usernameClientData;
	public TeamPlayer teamPlayer;
	public PlayerVMService playerVM;
	
	public Player(UsernameClientData usernameClientData, TeamPlayer teamPlayer) {
		this.usernameClientData = usernameClientData;
		this.teamPlayer = teamPlayer;
	}
}




