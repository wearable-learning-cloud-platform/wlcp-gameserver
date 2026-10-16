package org.wlcp.wlcpgameserver.dto.messages;

import java.util.Map;

/**
 * Photo check transition message. The same class is used in both directions, like
 * KeyboardInputMessage, because PlayerVMService.unblock only accepts a reply whose class
 * equals the class of the last request sent.
 *
 * Server to phone: modelUrl is set, scores and error are null.
 * Phone to server: scores holds class name to probability (0..1) from the model run on the
 * phone, or error holds a short code when no scores could be produced
 * (CAMERA_DENIED, MODEL_LOAD_FAILED, NO_PHOTO, PREDICT_FAILED). The photo itself never
 * leaves the phone.
 */
public class PhotoCheckMessage implements IMessage {
	public String modelUrl;
	public Map<String, Double> scores;
	public String error;
}
