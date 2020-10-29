package org.wlcp.wlcpgameserver.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.wlcp.wlcpgameserver.dto.GameDto;

@FeignClient(contextId="game-client", name="wlcp-api", url="${wlcp-api-url:}")
public interface GameFeignClient {
	
    @RequestMapping(method=RequestMethod.GET, value="/gameController/getGame/{gameId}")
    GameDto getGame(@PathVariable String gameId, @RequestHeader("Authorization") String bearerToken);

}
