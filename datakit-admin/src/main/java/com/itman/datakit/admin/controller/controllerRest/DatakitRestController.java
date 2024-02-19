package com.itman.datakit.admin.controller.controllerRest;

import com.alibaba.fastjson.JSON;
import com.itman.datakit.admin.common.api.RepairRequest;
import com.itman.datakit.admin.common.api.RepairResponse;
import com.itman.datakit.admin.common.exception.DatakitException;
import com.itman.datakit.admin.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/datakit")
@RequiredArgsConstructor
public class DatakitRestController {
    private final TaskService datakitService;

    /**
     * 修复数据服务
     */
    @PostMapping(path = "/repairData", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<RepairResponse> repairData(@RequestBody String repairRequest) {
        log.info("repairRequest=" + repairRequest);
        RepairResponse repairResponse = new RepairResponse();
        try {
            datakitService.repairData(JSON.parseObject(repairRequest, RepairRequest.class));
            repairResponse.setResultCode("0");
            repairResponse.setResultMsg("repair success!");
        } catch (DatakitException dke) {
            log.error("DatakitException=", dke);
            repairResponse.setResultCode("1");
            repairResponse.setResultMsg(dke.getErrMsg());
        } catch (Exception e) {
            log.error("Exception=", e);
            repairResponse.setResultCode("2");
            repairResponse.setResultMsg("unknow exception!");
        }
        return new ResponseEntity<>(repairResponse, HttpStatus.OK);
    }
}
