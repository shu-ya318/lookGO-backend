package com.mli.lookgo.module.metro.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import com.mli.lookgo.module.metro.enums.SyncStatusEnum;
import com.mli.lookgo.module.metro.model.vo.SyncStatusVO;

/**
 * 執行緒安全的票價背景同步進度狀態容器（單例）。
 * 以 in-memory 方式保存單一票價同步作業的狀態、進度與時間戳；
 * 單機部署下即足夠，若未來多實例部署需改存 Redis（專案已有 RedisService，預留即可）。
 *
 * @author D5042101
 * @since 2026.07.12
 */
@Component
public class StationFareSyncStateHolder {

    private final AtomicReference<SyncStatusEnum> status = new AtomicReference<>(SyncStatusEnum.IDLE);
    private final AtomicInteger progressPercentage = new AtomicInteger(0);

    private volatile String message = "尚未執行票價同步";
    private volatile LocalDateTime startedAt;
    private volatile LocalDateTime finishedAt;

    /**
     * 嘗試啟動一次同步。
     * 以 synchronized 保證「檢查目前非 RUNNING → 設為 RUNNING」的複合動作為原子操作，
     * 確保同時只有一個票價同步作業能啟動。
     *
     * @return 是否成功取得啟動權（false 表示已有同步在進行中）
     */
    public synchronized boolean tryStart() {
        if (status.get() == SyncStatusEnum.RUNNING) {
            return false;
        }
        status.set(SyncStatusEnum.RUNNING);
        progressPercentage.set(0);
        message = "已開始背景同步票價資料...";
        startedAt = LocalDateTime.now(ZoneOffset.UTC);
        finishedAt = null;

        return true;
    }

    /**
     * 更新同步進度與階段訊息。
     *
     * @param progress 進度百分比（自動限制在 0-100）
     * @param message  當前階段訊息
     */
    public void updateProgress(int progress, String message) {
        progressPercentage.set(Math.max(0, Math.min(100, progress)));
        this.message = message;
    }

    /**
     * 標記同步成功完成。
     */
    public void markSuccess() {
        progressPercentage.set(100);
        message = "票價資料同步成功!";
        finishedAt = LocalDateTime.now(ZoneOffset.UTC);
        status.set(SyncStatusEnum.SUCCESS);
    }

    /**
     * 標記同步失敗，保留失敗當下的進度值。
     *
     * @param message 失敗訊息
     */
    public void markFailed(String message) {
        this.message = message;
        finishedAt = LocalDateTime.now(ZoneOffset.UTC);
        status.set(SyncStatusEnum.FAILED);
    }

    /**
     * 取得當前狀態的一致性快照。
     *
     * @return SyncStatusVO
     */
    public SyncStatusVO snapshot() {
        return new SyncStatusVO(
                status.get(),
                progressPercentage.get(),
                message,
                startedAt,
                finishedAt);
    }
}
