package com.itman.datakit.admin.service;

import com.itman.datakit.admin.common.dataqueue.ObjectQueue;
import com.itman.datakit.admin.component.CommonProcess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 监控处理
 *
 * @author: ItMan
 * @since: 2024/01/15  15:09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorService {
    private final CommonProcess commonProcess;

    @Scheduled(fixedDelay = 3000)
    @Async("ScheduleMonitor")
    public void scheduledMonitorTask() {
        try {
            log.info("scheduledMonitorTask------->");

            ConcurrentHashMap<Long, AtomicInteger> dataSumCountMap = ObjectQueue.getDataSumCountMap();
            dataSumCountMap.forEach((x, y) -> {
                commonProcess.saveMonitor(x, y.get());
            });

        } catch (Exception e) {
            log.error("---------Exception=", e);
        }
    }
}
