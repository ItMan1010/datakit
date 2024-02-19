package com.itman.datakit.admin.common.threadpool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import static com.itman.datakit.admin.common.constants.DatakitConstant.A_THREAD_POOL_EXECUTOR;

@Configuration
@EnableAsync
@Slf4j
public class SourceThreadPool {
    /**
     * A线程池初始化线程个数
     */
    @Value("${datakit.athreadpool.core-pool-size:5}")
    private Integer corePoolSize;

    /**
     * A线程池最大线程个数
     */
    @Value("${datakit.athreadpool.max-pool-size:20}")
    private Integer maxPoolSize;

    /**
     * A角色对象处理线程数
     */
    @Value("${datakit.aobject.actual-thread-count:10}")
    private Integer aThreadCount;

    @Bean(A_THREAD_POOL_EXECUTOR)
    public Executor myExecutor() {
        if (corePoolSize == null || corePoolSize <= 0) {
            log.error(" error corePoolSize={}", corePoolSize);
            return null;
        }

        if (maxPoolSize == null || maxPoolSize > 100) {
            log.error(" error maxPoolSize={}", maxPoolSize);
            return null;
        }

        if (corePoolSize > maxPoolSize) {
            log.error(" error corePoolSize > maxPoolSize corePoolSize={},maxPoolSize={}", corePoolSize, maxPoolSize);
            return null;
        }

        if (aThreadCount > maxPoolSize) {
            log.error(" error aThreadCount > maxPoolSize aThreadCount={},maxPoolSize={}", aThreadCount, maxPoolSize);
            return null;
        }

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：线程池创建时候初始化的线程数
        executor.setCorePoolSize(corePoolSize);
        // 最大线程数：线程池最大的线程数，只有在缓冲队列满了之后才会申请超过核心线程数的线程
        executor.setMaxPoolSize(maxPoolSize);
        // 缓冲队列：用来缓冲执行任务的队列
        executor.setQueueCapacity(0);
        // 允许线程的空闲时间60秒：当超过了核心线程之外的线程在空闲时间到达之后会被销毁
        executor.setKeepAliveSeconds(60);
        // 线程池名的前缀：设置好了之后可以方便我们定位处理任务所在的线程池
        executor.setThreadNamePrefix("SourceExecutor-");
        // 缓冲队列满了之后的拒绝策略：由调用线程处理（一般是主线程）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        return executor;
    }
}
