package com.mli.lookgo.module.stationChat.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mli.lookgo.module.stationChat.enums.ChatEventTypeEnum;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 回傳車站聊天即時事件資料的物件，透過 STOMP 廣播給訂閱該車站的使用者。
 *
 * @author D5042101
 * @since 2026.07.04
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "回傳車站聊天即時事件資料的物件")
public class StationChatEventVO {

    @Schema(description = "事件類型 (NEW=新訊息, DELETE=刪除訊息)", example = "NEW")
    private ChatEventTypeEnum eventType;

    @Schema(description = "新增的留言內容 (eventType=NEW 時提供)")
    private StationChatMessageVO message;

    @Schema(description = "被刪除的留言 id (eventType=DELETE 時提供)", example = "5")
    private Integer deletedMessageId;

    public StationChatEventVO() {
    }

    public StationChatEventVO(ChatEventTypeEnum eventType, StationChatMessageVO message, Integer deletedMessageId) {
        this.eventType = eventType;
        this.message = message;
        this.deletedMessageId = deletedMessageId;
    }

    public ChatEventTypeEnum getEventType() {
        return eventType;
    }

    public void setEventType(ChatEventTypeEnum eventType) {
        this.eventType = eventType;
    }

    public StationChatMessageVO getMessage() {
        return message;
    }

    public void setMessage(StationChatMessageVO message) {
        this.message = message;
    }

    public Integer getDeletedMessageId() {
        return deletedMessageId;
    }

    public void setDeletedMessageId(Integer deletedMessageId) {
        this.deletedMessageId = deletedMessageId;
    }

    @Override
    public String toString() {
        return "StationChatEventVO{eventType=" + eventType + ", deletedMessageId=" + deletedMessageId + '}';
    }
}
