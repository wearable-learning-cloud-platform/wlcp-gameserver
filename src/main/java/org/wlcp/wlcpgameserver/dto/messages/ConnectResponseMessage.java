package org.wlcp.wlcpgameserver.dto.messages;

public class ConnectResponseMessage implements IMessage {
	public enum Code { SUCCESS, FAIL, RECONNECT}
	public Code code;
	public int team;
	public int player;
}
