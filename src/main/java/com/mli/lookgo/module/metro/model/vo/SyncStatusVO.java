package com.mli.lookgo.module.metro.model.vo;

import java.time.LocalDateTime;

import com.mli.lookgo.module.metro.enums.SyncStatusEnum;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 票價背景同步作業的狀態回應物件，供前端每 30 秒輪詢追蹤進度。
 * 狀態為 in-memory 儲存，伺服器重啟後遺失並回到 {@link SyncStatusEnum#IDLE}
 * （upsert 冪等，重按一次即可），單機部署下屬可接受行為。
 *
 * @author D5042101
 * @since 2026.07.12
 */
@Schema(description = "票價背景同步作業的狀態")
public class SyncStatusVO {

    @Schema(description = "同步狀態", example = "RUNNING")
    private SyncStatusEnum status;

    @Schema(description = "進度百分比 (0-100 整數)，供前端顯示", example = "63")
    private int progressPercentage;

    @Schema(description = "當前階段訊息，供前端顯示", example = "票價資料更新中... (63%)")
    private String message;

    @Schema(description = "同步開始時間 (UTC, ISO 8601)，尚未開始為 null", example = "2026-07-12T03:21:00Z")
    private LocalDateTime startedAt;

    @Schema(description = "同步結束時間 (UTC, ISO 8601)，SUCCESS / FAILED 時有值", example = "2026-07-12T03:28:00Z")
    private LocalDateTime finishedAt;

    public SyncStatusVO() {
    }

    public SyncStatusVO(SyncStatusEnum status, int progressPercentage, String message,
            LocalDateTime startedAt, LocalDateTime finishedAt) {
        this.status = status;
        this.progressPercentage = progressPercentage;
        this.message = message;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }

    public SyncStatusEnum getStatus() {
        return status;
    }

    public void setStatus(SyncStatusEnum status) {
        this.status = status;
    }

    public int getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(int progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    @Override
    public String toString() {
        return "SyncStatusVO{" +
                "status=" + status +
                ", progressPercentage=" + progressPercentage +
                ", message='" + message + '\'' +
                ", startedAt=" + startedAt +
                ", finishedAt=" + finishedAt +
                '}';
    }
}
