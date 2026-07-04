package com.mli.lookgo.module.stationChat.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import com.mli.lookgo.core.result.MessageVO;
import com.mli.lookgo.module.stationChat.enums.ChatEventTypeEnum;
import com.mli.lookgo.module.stationChat.model.dto.SendMessageDTO;
import com.mli.lookgo.module.stationChat.model.vo.StationChatEventVO;
import com.mli.lookgo.module.stationChat.model.vo.StationChatMessageVO;
import com.mli.lookgo.module.stationChat.service.StationChatService;

import jakarta.validation.Valid;

/**
 * 處理站點聊天留言即時發送與刪除的 STOMP 控制器，處理完成後廣播給訂閱該車站的所有使用者。
 *
 * @author D5042101
 * @since 2026.07.04
 */
@Controller
public class StationChatStompController {

        private static final Logger logger = LoggerFactory.getLogger(StationChatStompController.class);
        private static final String TOPIC_PREFIX = "/topic/station-chat/";

        private final StationChatService stationChatService;
        private final SimpMessagingTemplate simpMessagingTemplate;

        /**
         * 讓 Spring 容器能在應用程式啟動時，自動注入所需的依賴。
         *
         * @param stationChatService
         * @param simpMessagingTemplate
         */
        public StationChatStompController(StationChatService stationChatService,
                        SimpMessagingTemplate simpMessagingTemplate) {
                this.stationChatService = stationChatService;
                this.simpMessagingTemplate = simpMessagingTemplate;
        }

        /**
         * 發送一則站點聊天留言（文字訊息或旅程分享），並廣播給訂閱該車站的所有使用者。
         *
         * @param stationId
         * @param sendMessageDTO
         * @param principal
         */
        @MessageMapping("/station-chat/{stationId}/send-message")
        public void sendMessage(@DestinationVariable Integer stationId, @Valid @Payload SendMessageDTO sendMessageDTO,
                        Principal principal) {
                logger.debug("收到 STOMP 發送站點聊天留言的請求，stationId: {}, email: {}, sendMessageDTO: {}", stationId,
                                principal.getName(), sendMessageDTO);

                StationChatMessageVO messageVO = stationChatService.sendMessage(stationId, sendMessageDTO,
                                principal.getName());

                simpMessagingTemplate.convertAndSend(TOPIC_PREFIX + stationId,
                                new StationChatEventVO(ChatEventTypeEnum.NEW.getCode(), messageVO, null));
        }

        /**
         * 刪除指定的站點聊天留言（本人或 ADMIN），並廣播給訂閱該車站的所有使用者。
         *
         * @param stationId
         * @param messageId
         * @param principal
         */
        @MessageMapping("/station-chat/{stationId}/delete-message/{messageId}")
        public void deleteMessage(@DestinationVariable Integer stationId, @DestinationVariable Integer messageId,
                        Principal principal) {
                logger.debug("收到 STOMP 刪除站點聊天留言的請求，stationId: {}, messageId: {}, email: {}", stationId, messageId,
                                principal.getName());

                stationChatService.deleteMessage(stationId, messageId, principal.getName());

                simpMessagingTemplate.convertAndSend(TOPIC_PREFIX + stationId,
                                new StationChatEventVO(ChatEventTypeEnum.DELETE.getCode(), null, messageId));
        }

        /**
         * 處理 STOMP 訊息映射方法拋出的表單驗證例外，回傳欄位名稱與錯誤訊息的對應表給發送者本人。
         *
         * @param exception
         * @return 欄位名稱與錯誤訊息的對應表
         */
        @MessageExceptionHandler(MethodArgumentNotValidException.class)
        @SendToUser("/queue/errors")
        public Map<String, String> handleValidationException(MethodArgumentNotValidException exception) {
                logger.error("STOMP 訊息驗證失敗: {}", exception.getMessage());

                Map<String, String> errors = new HashMap<>();
                exception.getBindingResult().getFieldErrors()
                                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

                return errors;
        }

        /**
         * 處理 STOMP 訊息映射方法拋出的其餘例外，回傳錯誤訊息給發送者本人。
         *
         * @param exception
         * @return MessageVO
         */
        @MessageExceptionHandler
        @SendToUser("/queue/errors")
        public MessageVO handleException(Exception exception) {
                logger.error("STOMP 訊息處理發生例外: {}", exception.getMessage());

                return new MessageVO(exception.getMessage());
        }
}
