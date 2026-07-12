package com.mli.lookgo.module.metro.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 啟用非同步方法執行，並提供票價背景同步專用的執行緒池。
 * corePoolSize=1、maxPoolSize=1、queueCapacity=0 使此執行緒池天然保證同時只有一個票價同步操作執行，
 * 與 {@code StationFareSyncStateHolder.tryStart()} 形成雙保險，也不影響 Spring 預設 executor 上的其他任務。
 *
 * @author D5042101
 * @since 2026.07.12
 */
@Configuration
@EnableAsync
public class MetroSyncAsyncConfig {

    /**
     * 票價背景同步專用的單執行緒池。
     *
     * @return Executor
     */
    @Bean("metroSyncExecutor")
    Executor metroSyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("metro-sync-");
        executor.initialize();

        return executor;
    }
}
