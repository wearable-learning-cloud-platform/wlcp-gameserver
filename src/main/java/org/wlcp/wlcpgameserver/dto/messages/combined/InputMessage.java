package org.wlcp.wlcpgameserver.dto.messages.combined;

import org.wlcp.wlcpgameserver.dto.messages.IMessage;

public class InputMessage {

	public MessageType type;
	public IMessage msg;
	
	public InputMessage(MessageType type, IMessage msg) {
		this.type = type;
		this.msg = msg;
	}
}
