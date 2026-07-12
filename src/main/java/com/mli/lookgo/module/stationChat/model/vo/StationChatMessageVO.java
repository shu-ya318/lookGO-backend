package com.mli.lookgo.module.stationChat.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.mli.lookgo.module.stationChat.enums.ChatTypeEnum;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 回傳車站聊天留言資料的物件。
 *
 * @author D5042101
 * @since 2026.07.03
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "回傳車站聊天留言資料的物件")
public class StationChatMessageVO {

    @Schema(description = "留言 id", example = "1")
    private Integer id;

    @Schema(description = "留言者名稱", example = "小明")
    private String username;

    @Schema(description = "留言者頭像（base64 data URI 或預設頭像相對路徑）", example = "/assets/default-avatar.png")
    private String avatar;

    @Schema(description = "留言類型 (TEXT=文字訊息, TRIP_PLAN=旅程分享)", example = "TEXT")
    private ChatTypeEnum chatType;

    @Schema(description = "文字訊息內容 (chatType=TEXT 時提供)", example = "這裡的電梯正在維修")
    private String content;

    @Schema(description = "旅程分享關聯的旅程規劃 id (chatType=TRIP_PLAN 時提供)", example = "10")
    private Integer tripPlanId;

    @Schema(description = "起始車站名稱 (chatType=TRIP_PLAN 時提供)", example = "淡水站")
    private String fromStationName;

    @Schema(description = "終點車站名稱 (chatType=TRIP_PLAN 時提供)", example = "台北車站")
    private String toStationName;

    @Schema(description = "票種 (1=全票, 4=學生, 5=兒童, 7=愛心；chatType=TRIP_PLAN 時提供)", example = "1")
    private Integer fareType;

    @Schema(description = "票價 (元；chatType=TRIP_PLAN 時提供)", example = "45.00")
    private BigDecimal farePrice;

    @Schema(description = "轉乘次數 (chatType=TRIP_PLAN 時提供)", example = "1")
    private Integer transferCount;

    @Schema(description = "總車程時間 (秒，含轉乘時間；chatType=TRIP_PLAN 時提供)", example = "1080")
    private Integer travelTimeSeconds;

    @Schema(description = "旅程規劃備註 (chatType=TRIP_PLAN 時提供)", example = "週末出遊路線")
    private String notes;

    @Schema(description = "留言建立時間 (UTC, ISO 8601)", example = "2026-07-03T12:00:00Z")
    private LocalDateTime createdAt;

    /** 起始車站 id，僅供後端即時計算車程時間使用，不對外輸出。 */
    @JsonIgnore
    @Schema(hidden = true)
    private Integer fromStationId;

    /** 終點車站 id，僅供後端即時計算車程時間使用，不對外輸出。 */
    @JsonIgnore
    @Schema(hidden = true)
    private Integer toStationId;

    /** 路線規劃策略，僅供後端即時計算車程時間使用，不對外輸出。 */
    @JsonIgnore
    @Schema(hidden = true)
    private Integer routingStrategy;

    public StationChatMessageVO() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public ChatTypeEnum getChatType() {
        return chatType;
    }

    public void setChatType(ChatTypeEnum chatType) {
        this.chatType = chatType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getTripPlanId() {
        return tripPlanId;
    }

    public void setTripPlanId(Integer tripPlanId) {
        this.tripPlanId = tripPlanId;
    }

    public String getFromStationName() {
        return fromStationName;
    }

    public void setFromStationName(String fromStationName) {
        this.fromStationName = fromStationName;
    }

    public String getToStationName() {
        return toStationName;
    }

    public void setToStationName(String toStationName) {
        this.toStationName = toStationName;
    }

    public Integer getFareType() {
        return fareType;
    }

    public void setFareType(Integer fareType) {
        this.fareType = fareType;
    }

    public BigDecimal getFarePrice() {
        return farePrice;
    }

    public void setFarePrice(BigDecimal farePrice) {
        this.farePrice = farePrice;
    }

    public Integer getTransferCount() {
        return transferCount;
    }

    public void setTransferCount(Integer transferCount) {
        this.transferCount = transferCount;
    }

    public Integer getTravelTimeSeconds() {
        return travelTimeSeconds;
    }

    public void setTravelTimeSeconds(Integer travelTimeSeconds) {
        this.travelTimeSeconds = travelTimeSeconds;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getFromStationId() {
        return fromStationId;
    }

    public void setFromStationId(Integer fromStationId) {
        this.fromStationId = fromStationId;
    }

    public Integer getToStationId() {
        return toStationId;
    }

    public void setToStationId(Integer toStationId) {
        this.toStationId = toStationId;
    }

    public Integer getRoutingStrategy() {
        return routingStrategy;
    }

    public void setRoutingStrategy(Integer routingStrategy) {
        this.routingStrategy = routingStrategy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "StationChatMessageVO{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", chatType=" + chatType +
                ", tripPlanId=" + tripPlanId +
                ", createdAt=" + createdAt +
                '}';
    }
}
