package org.wlcp.wlcpgameserver.dto.messages.combined;

import org.wlcp.wlcpgameserver.dto.messages.IMessage;

public class OutputMessage {
	
	public MessageType type;
	public IMessage msg;
	
	public OutputMessage(MessageType type, IMessage msg) {
		this.type = type;
		this.msg = msg;
	}
}
