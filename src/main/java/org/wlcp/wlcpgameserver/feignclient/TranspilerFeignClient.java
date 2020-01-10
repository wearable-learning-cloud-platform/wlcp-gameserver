package org.wlcp.wlcpgameserver.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(contextId="transpiler-client", name="wlcp-transpiler")
public interface TranspilerFeignClient {

    @RequestMapping(method=RequestMethod.GET, value="/transpilerController/transpileGame")
    String transpileGame(@RequestParam("gameId") String gameId);

}
