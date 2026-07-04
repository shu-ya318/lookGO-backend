package com.mli.lookgo.module.stationChat.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

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

    @Schema(description = "事件類型 (1=新訊息, 2=刪除訊息)", example = "1")
    private Integer eventType;

    @Schema(description = "新增的留言內容 (eventType=1 時提供)")
    private StationChatMessageVO message;

    @Schema(description = "被刪除的留言 id (eventType=2 時提供)", example = "5")
    private Integer deletedMessageId;

    public StationChatEventVO() {
    }

    public StationChatEventVO(Integer eventType, StationChatMessageVO message, Integer deletedMessageId) {
        this.eventType = eventType;
        this.message = message;
        this.deletedMessageId = deletedMessageId;
    }

    public Integer getEventType() {
        return eventType;
    }

    public void setEventType(Integer eventType) {
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
