package com.itman.datakit.admin.common.feignClient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.net.URI;

@FeignClient(name = "DatakitFeignClient", url = "null")
public interface DatakitFeignClient {

    @RequestMapping(value = "/datakit/repairData", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    String sentRepairMessage(URI uri, @RequestBody String repairMessageRequest);
}
