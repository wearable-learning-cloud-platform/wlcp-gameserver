package org.wlcp.wlcpgameserver.feignclient.dto;

public class LogEventGamePlayerCommunicationDto extends LogEventGamePlayerDto {
	
	public enum DataDirection {
		CLIENT_RECEIVE,
		CLIENT_SEND,
		SERVER_RECEIVE,
		SERVER_SEND
	}
	
	public enum Output {
		NONE,
		DISPLAY_TEXT,
		DISPLAY_PHOTO,
		PLAY_SOUND,
		PLAY_VIDEO
	}
	
	public enum Input {
		NONE,
		SINGLE_BUTTON_PRESS,
		SEQUENCE_BUTTON_PRESS,
	    KEYBOARD_INPUT,
	    TIMER,
	    RANDOM
	}
	
	public DataDirection dataDirection;
	public Output output;
	public Input input;
	public String data;
	
	public LogEventGamePlayerCommunicationDto(LogEventGamePlayerType logEventGamePlayerType, Integer logEventGameInstance, Integer team,
			Integer player, DataDirection dataDirection, Output output, Input input, String data) {
		super(logEventGamePlayerType, logEventGameInstance, team, player);
		this.dataDirection = dataDirection;
		this.output = output;
		this.input = input;
		this.data = data;
	}

	public DataDirection getDataDirection() {
		return dataDirection;
	}

	public void setDataDirection(DataDirection dataDirection) {
		this.dataDirection = dataDirection;
	}

	public Output getOutput() {
		return output;
	}

	public void setOutput(Output output) {
		this.output = output;
	}

	public Input getInput() {
		return input;
	}

	public void setInput(Input input) {
		this.input = input;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

}
