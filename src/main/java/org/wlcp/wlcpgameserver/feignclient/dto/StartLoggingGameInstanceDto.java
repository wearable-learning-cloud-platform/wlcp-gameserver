package org.wlcp.wlcpgameserver.feignclient.dto;

public class StartLoggingGameInstanceDto {

    	public Integer id;
		public String gameId;
    	public String usernameId;
    	public Boolean debugInstance;
    	
		public StartLoggingGameInstanceDto(String gameId, String usernameId, Boolean debugInstance) {
			super();
			this.gameId = gameId;
			this.usernameId = usernameId;
			this.debugInstance = debugInstance;
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
		
		public Boolean getDebugInstance() {
			return debugInstance;
		}
		
		public void setDebugInstance(Boolean debugInstance) {
			this.debugInstance = debugInstance;
		}
   
}
