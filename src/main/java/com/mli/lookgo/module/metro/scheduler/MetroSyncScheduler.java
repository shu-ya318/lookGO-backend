package com.mli.lookgo.module.metro.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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
     * 應用程式完全啟動後，檢查資料庫是否為空。
     * 若為空（首次部署或容器初始化），立即執行一次完整同步；已有資料（重啟）則跳過，等待排程時間執行。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeOnFirstDeployment() {
        if (metroSyncService.isMetroDataEmpty()) {
            logger.debug("偵測到資料庫無捷運資料，開始執行初始化同步");
            syncAllDataPipeline();
            return;
        }
        logger.debug("資料庫已有捷運資料，跳過初始化同步，等待排程時間執行");
    }

    /**
     * 每週日 23:00 自動執行捷運資料同步。
     * 依資料庫外鍵相依性嚴格控制執行順序。
     */
    @Scheduled(cron = "0 0 23 * * SUN")
    public void syncAllDataPipeline() {
        logger.debug("開始執行捷運資料定期同步排程");
        long startTime = System.currentTimeMillis();

        try {
            // Layer 1: 無外鍵相依的資料
            metroSyncService.syncAllLine();
            metroSyncService.syncAllStation();

            // Layer 2: 有外鍵相依的資料
            metroSyncService.syncAllLineStation();

            metroSyncService.syncAllLineStationCumulativeTime();
            metroSyncService.syncAllLineTransfer();
            metroSyncService.syncAllStationFare();

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("捷運資料定期同步排程順利完成，總耗時: {} ms", duration);

        } catch (Exception exception) {
            logger.error("捷運資料同步排程發生錯誤，終止後續作業", exception);
        }
    }
}
