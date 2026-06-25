package com.mli.lookgo.module.metro.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mli.lookgo.module.metro.service.MetroSyncService;

/**
 * 定時排程，自動從外部 API 同步捷運資料到資料庫。
 *
 * @author D5042101
 * @since 2026.06.24
 */
@Component
public class MetroSyncScheduler {

    private final MetroSyncService metroSyncService;
    private static final Logger logger = LoggerFactory.getLogger(MetroSyncScheduler.class);

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入所需的依賴。
     *
     * @param metroSyncService
     */
    public MetroSyncScheduler(MetroSyncService metroSyncService) {
        this.metroSyncService = metroSyncService;
    }

    /**
     * 每 7 天自動從 TDX API 同步路線資料。
     */
    @Scheduled(fixedRate = 7 * 24 * 60 * 60 * 1000)
    public void syncAllLine() {
        logger.debug("開始進行同步路線資料的排程作業");
        metroSyncService.syncAllLine();
        logger.debug("完成同步路線資料的排程作業");
    }

    /**
     * 每 7 天自動從 TDX + TPE API 同步車站資料。
     */
    // @Scheduled(fixedRate = 7 * 24 * 60 * 60 * 1000)
    // public void syncAllStation() {
    // logger.debug("開始進行同步車站資料的排程作業");
    // metroSyncService.syncAllStation();
    // logger.debug("完成同步車站資料的排程作業");
    // }
}
