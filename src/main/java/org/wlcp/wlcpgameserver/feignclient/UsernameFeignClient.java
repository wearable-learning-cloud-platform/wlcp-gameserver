package org.wlcp.wlcpgameserver.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.wlcp.wlcpgameserver.dto.UsernameDto;

@FeignClient(contextId="username-client", name="wlcp-api")
public interface UsernameFeignClient {
	
    @RequestMapping(method=RequestMethod.GET, value="/usernameController/getUsername/{usernameId}")
    UsernameDto getUsername(@PathVariable String usernameId);

}
