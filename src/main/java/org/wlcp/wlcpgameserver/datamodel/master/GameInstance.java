package org.wlcp.wlcpgameserver.datamodel.master;

import java.io.Serializable;
import javax.persistence.*;

/**
 * Entity implementation class for Entity: GameInstance
 *
 */
@Entity
@Table(name = "GAME_INSTANCE")
public class GameInstance implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "GAME_INSTANCE_ID")
	private Integer gameInstanceId;
	
	@Column(name = "GAME_ID")
	private String gameId;
	
	@Column(name = "USERNAME_ID")
	private String usernameId;
	
	@Column(name = "DEBUG_INSTANCE")
	private boolean debugInstance;
	
	public GameInstance() {
		super();
	}

	public GameInstance(Integer gameInstanceId, String gameId, String usernameId, boolean debugInstance) {
		super();
		this.gameInstanceId = gameInstanceId;
		this.gameId = gameId;
		this.usernameId = usernameId;
		this.debugInstance = debugInstance;
	}

	public Integer getGameInstanceId() {
		return gameInstanceId;
	}

	public void setGameInstanceId(Integer gameInstanceId) {
		this.gameInstanceId = gameInstanceId;
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	public String getUsernameId() {
		return usernameId;
	}

	public void setUsernameId(String usernameId) {
		this.usernameId = usernameId;
	}

	public boolean isDebugInstance() {
		return debugInstance;
	}

	public void setDebugInstance(boolean debugInstance) {
		this.debugInstance = debugInstance;
	}
   
}
