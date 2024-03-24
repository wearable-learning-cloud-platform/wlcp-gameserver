package org.wlcp.wlcpgameserver.dto.messages;

public class PlayerAvaliableMessage implements IMessage {
	
	public enum Type {
		USERNAME_EXISTS,
		USERNAME_DOES_NOT_EXIST
	}
	
	public Type type;
	public int team;
	public int player;
	public String playerName;
}
