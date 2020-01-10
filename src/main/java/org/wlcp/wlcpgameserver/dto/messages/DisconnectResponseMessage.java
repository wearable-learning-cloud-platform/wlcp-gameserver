package org.wlcp.wlcpgameserver.dto.messages;

public class DisconnectResponseMessage implements IMessage {
	public enum Code { SUCCESS, FAIL}
	public Code code;
}
