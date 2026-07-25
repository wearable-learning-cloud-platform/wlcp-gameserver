package org.wlcp.wlcpgameserver.datamodel.master;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Formula;

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
	
	@Column(name = "START_TIME")
	@CreationTimestamp
	private Timestamp startTime;
	
	@Column(name = "END_TIME")
	private Timestamp endTime;
	
	@Column(name = "DURATION")
	private Long duration;
	
	@Column(name = "DEBUG_INSTANCE")
	private Boolean debugInstance;
	
	@Column(name = "GAME_ENDED")
	private Boolean gameEnded;
	
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

	public Timestamp getEndTime() {
		return endTime;
	}

	public void setEndTime(Timestamp endTime) {
		this.endTime = endTime;
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

	public Timestamp getStartTime() {
		return startTime;
	}

	public Boolean getGameEnded() {
		return gameEnded;
	}

	public void setGameEnded(Boolean gameEnded) {
		this.gameEnded = gameEnded;
	}

	public List<GameInstancePlayer> getPlayers() {
		return players;
	}

	public void setPlayers(List<GameInstancePlayer> players) {
		this.players = players;
	}
   
}
