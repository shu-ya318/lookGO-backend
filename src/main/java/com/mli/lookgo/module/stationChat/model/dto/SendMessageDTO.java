package com.mli.lookgo.module.stationChat.model.dto;

import com.mli.lookgo.module.stationChat.enums.ChatTypeEnum;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 處理發送車站聊天留言相關的資料傳輸物件。
 *
 * @author D5042101
 * @since 2026.07.04
 */
@Schema(description = "處理發送車站聊天留言相關的資料傳輸物件")
public class SendMessageDTO {

    @Schema(description = "留言類型 (TEXT=文字訊息, TRIP_PLAN=旅程分享)", example = "TEXT")
    @NotNull(message = "請輸入留言類型!")
    private ChatTypeEnum chatType;

    @Schema(description = "文字訊息內容 (chatType=TEXT 時必填)", example = "這裡的電梯正在維修")
    @Size(max = 1000, message = "文字訊息內容長度不能超過 1000 個字元!")
    private String content;

    @Schema(description = "旅程分享關聯的旅程規劃 id (chatType=TRIP_PLAN 時必填)", example = "10")
    private Integer tripPlanId;

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

    @Override
    public String toString() {
        return "SendMessageDTO{chatType=" + chatType + ", content='" + content + '\'' + ", tripPlanId=" + tripPlanId
                + '}';
    }
}
