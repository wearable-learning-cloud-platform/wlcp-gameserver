package org.wlcp.wlcpgameserver.dto.messages.combined;

import java.util.ArrayList;
import java.util.List;

import org.wlcp.wlcpgameserver.dto.messages.IMessage;

public class CombinedMessage implements IMessage {
	public List<OutputMessage> outputMessages = new ArrayList<OutputMessage>();
	public List<InputMessage> inputMessages = new ArrayList<InputMessage>();
}
