package org.wlcp.wlcpgameserver.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.wlcp.wlcpgameserver.feignclient.dto.LogEventGamePlayerDto;
import org.wlcp.wlcpgameserver.feignclient.dto.StartLoggingGameInstanceDto;
import org.wlcp.wlcpgameserver.feignclient.dto.StopLoggingGameInstanceDto;

@FeignClient(contextId="metrics-client", name="wlcp-metrics", url="${wlcp-metrics-url:}")
public interface MetricsFeignClient {
	
    @RequestMapping(method=RequestMethod.POST, value="/logEventGameInstanceController/startLoggingGameInstance")
    StartLoggingGameInstanceDto startLoggingGameInstance(StartLoggingGameInstanceDto startLoggingGameInstanceDto);
    
    @RequestMapping(method=RequestMethod.POST, value="/logEventGameInstanceController/stopLoggingGameInstance")
    void stopLoggingGameInstance(StopLoggingGameInstanceDto stopLoggingGameInstanceDto);
	
	@RequestMapping(method=RequestMethod.POST, value="/logEventGameInstanceController/logEventGamePlayer")
    LogEventGamePlayerDto logEventGamePlayer(LogEventGamePlayerDto logEventGamePlayerDto);

}
