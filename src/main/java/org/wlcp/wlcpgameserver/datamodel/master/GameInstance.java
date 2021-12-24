package org.wlcp.wlcpgameserver.datamodel.master;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

/**
 * Entity implementation class for Entity: GameInstance
 *
 */
@Entity
@Table(name = "GAME_INSTANCE")
public class GameInstance implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@Id
	@Column(name = "GAME_INSTANCE_ID")
	@GeneratedValue(strategy = GenerationType.IDENTITY )
	private Integer gameInstanceId;
	
	@Column(name = "GAME_ID")
	private String gameId;
	
	@Column(name = "USERNAME_ID")
	private String usernameId;
	
	@Column(name = "START")
	@CreationTimestamp
	private Timestamp start;
	
	@Column(name = "END")
	private Timestamp end;
	
	@Column(name = "DURATION")
	private Long duration;
	
	@Column(name = "DEBUG_INSTANCE")
	private Boolean debugInstance;
	
	@ElementCollection
	private List<GameInstancePlayer> players = new ArrayList<GameInstancePlayer>();
	
	public GameInstance() {
		super();
	}

	public GameInstance(String gameId, String usernameId, Boolean debugInstance) {
		super();
		this.gameId = gameId;
		this.usernameId = usernameId;
		this.debugInstance = debugInstance;
	}

	public Timestamp getEnd() {
		return end;
	}

	public void setEnd(Timestamp end) {
		this.end = end;
	}

	public Long getDuration() {
		return duration;
	}

	public void setDuration(Long duration) {
		this.duration = duration;
	}

	public String getUsernameId() {
		return usernameId;
	}

	public void setUsernameId(String usernameId) {
		this.usernameId = usernameId;
	}

	public Boolean getDebugInstance() {
		return debugInstance;
	}

	public void setDebugInstance(Boolean debugInstance) {
		this.debugInstance = debugInstance;
	}

	public Integer getGameInstanceId() {
		return gameInstanceId;
	}

	public String getGameId() {
		return gameId;
	}

	public Timestamp getStart() {
		return start;
	}

	public List<GameInstancePlayer> getPlayers() {
		return players;
	}

	public void setPlayers(List<GameInstancePlayer> players) {
		this.players = players;
	}
	
//	@Id
//	@GeneratedValue(strategy = GenerationType.IDENTITY)
//	@Column(name = "GAME_INSTANCE_ID")
//	private Integer gameInstanceId;
//	
//	@Column(name = "GAME_ID")
//	private String gameId;
//	
//	@Column(name = "USERNAME_ID")
//	private String usernameId;
//	
//	@Column(name = "DEBUG_INSTANCE")
//	private boolean debugInstance;
//	
//	public GameInstance() {
//		super();
//	}
//
//	public GameInstance(Integer gameInstanceId, String gameId, String usernameId, boolean debugInstance) {
//		super();
//		this.gameInstanceId = gameInstanceId;
//		this.gameId = gameId;
//		this.usernameId = usernameId;
//		this.debugInstance = debugInstance;
//	}
//
//	public Integer getGameInstanceId() {
//		return gameInstanceId;
//	}
//
//	public void setGameInstanceId(Integer gameInstanceId) {
//		this.gameInstanceId = gameInstanceId;
//	}
//
//	public String getGameId() {
//		return gameId;
//	}
//
//	public void setGameId(String gameId) {
//		this.gameId = gameId;
//	}
//
//	public String getUsernameId() {
//		return usernameId;
//	}
//
//	public void setUsernameId(String usernameId) {
//		this.usernameId = usernameId;
//	}
//
//	public boolean isDebugInstance() {
//		return debugInstance;
//	}
//
//	public void setDebugInstance(boolean debugInstance) {
//		this.debugInstance = debugInstance;
//	}
   
}
