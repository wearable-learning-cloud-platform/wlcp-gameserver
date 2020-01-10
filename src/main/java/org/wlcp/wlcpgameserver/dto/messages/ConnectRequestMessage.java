package org.wlcp.wlcpgameserver.dto.messages;

public class ConnectRequestMessage implements IMessage{
	public int gameInstanceId;
	public String usernameId;
	public int team;
	public int player;
}
