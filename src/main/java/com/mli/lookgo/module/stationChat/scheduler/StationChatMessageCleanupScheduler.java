package com.mli.lookgo.module.stationChat.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mli.lookgo.module.stationChat.service.StationChatService;

/**
 * 定時排程，每日清除各車站的聊天留言。
 *
 * @author D5042101
 * @since 2026.07.04
 */
@Component
public class StationChatMessageCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(StationChatMessageCleanupScheduler.class);

    private final StationChatService stationChatService;

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入所需的依賴。
     *
     * @param stationChatService
     */
    public StationChatMessageCleanupScheduler(StationChatService stationChatService) {
        this.stationChatService = stationChatService;
    }

    /**
     * 每日凌晨 3 點自動清除所有車站的聊天留言。
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void clearAllStationChatMessages() {
        logger.debug("開始執行車站聊天留言每日清除排程");

        try {
            int deletedCount = stationChatService.clearAllMessages();
            logger.debug("車站聊天留言每日清除排程順利完成，共清除 {} 筆", deletedCount);
        } catch (Exception exception) {
            logger.error("車站聊天留言每日清除排程發生錯誤", exception);
        }
    }
}
