package org.wlcp.wlcpgameserver.dto.messages;

import java.util.Map;

/**
 * Photo check transition message. The same class is used in both directions, like
 * KeyboardInputMessage, because PlayerVMService.unblock only accepts a reply whose class
 * equals the class of the last request sent.
 *
 * Server to phone: modelUrl, targetClasses and thresholds are set so the phone can explain the
 * result; the server still makes the decision. retry is true when the request is sent again
 * because the last photo matched nothing. scores and error are null.
 * Phone to server: scores holds class name to probability (0..1) from the model run on the
 * phone, or error holds a short code when no scores could be produced
 * (MODEL_LOAD_FAILED, PREDICT_FAILED). The photo itself never leaves the phone.
 */
public class PhotoCheckMessage implements IMessage {
	public String modelUrl;
	public String[] targetClasses;
	public double[] thresholds;
	public boolean retry;
	public Map<String, Double> scores;
	public String error;
}
