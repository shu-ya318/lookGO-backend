package com.mli.lookgo.module.stationChat.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.mli.lookgo.core.result.MessageVO;
import com.mli.lookgo.core.result.PaginatedVO;
import com.mli.lookgo.module.metro.exceptions.StationNotFoundException;
import com.mli.lookgo.module.metro.service.MetroService;
import com.mli.lookgo.module.stationChat.dao.StationChatDAO;
import com.mli.lookgo.module.stationChat.enums.ChatTypeEnum;
import com.mli.lookgo.module.stationChat.exceptions.ChatDailyLimitExceededException;
import com.mli.lookgo.module.stationChat.exceptions.ChatMessageAccessDeniedException;
import com.mli.lookgo.module.stationChat.exceptions.StationChatNotFoundException;
import com.mli.lookgo.module.stationChat.exceptions.TripPlanAccessDeniedException;
import com.mli.lookgo.module.stationChat.exceptions.TripPlanNotFoundException;
import com.mli.lookgo.module.stationChat.model.dto.CreateAnnouncementDTO;
import com.mli.lookgo.module.stationChat.model.dto.SendMessageDTO;
import com.mli.lookgo.module.stationChat.model.dto.UpdateAnnouncementDTO;
import com.mli.lookgo.module.stationChat.model.entity.StationChatAnnouncement;
import com.mli.lookgo.module.stationChat.model.entity.StationChatMessage;
import com.mli.lookgo.module.stationChat.model.vo.StationChatAnnouncementVO;
import com.mli.lookgo.module.stationChat.model.vo.StationChatMessageVO;
import com.mli.lookgo.module.user.dao.UserDAO;
import com.mli.lookgo.module.user.enums.UserRole;
import com.mli.lookgo.module.user.exceptions.UserNotFoundException;
import com.mli.lookgo.module.user.model.entity.User;

/**
 * 處理車站聊天留言與公告相關業務邏輯。
 *
 * @author D5042101
 * @since 2026.07.03
 */
@Service
public class StationChatService {

    private static final Logger logger = LoggerFactory.getLogger(StationChatService.class);

    private final StationChatDAO stationChatDAO;
    private final MetroService metroService;
    private final UserDAO userDAO;

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入所需的依賴。
     *
     * @param stationChatDAO
     * @param metroService
     * @param userDAO
     */
    public StationChatService(StationChatDAO stationChatDAO, MetroService metroService, UserDAO userDAO) {
        this.stationChatDAO = stationChatDAO;
        this.metroService = metroService;
        this.userDAO = userDAO;
    }

    /**
     * 依車站 id 分頁取得該車站的歷史聊天留言，依建立時間新到舊排序。
     *
     * @param stationId
     * @param page
     * @param size
     * @return PaginatedVO<StationChatMessageVO>
     */
    public PaginatedVO<StationChatMessageVO> getMessages(Integer stationId, int page, int size) {
        if (!metroService.existsStationById(stationId)) {
            throw new StationNotFoundException("找不到 id:" + stationId + " 的車站!");
        }

        logger.debug("開始依車站 id 分頁查詢車站聊天留言，stationId: {}, page: {}, size: {}", stationId, page, size);

        List<StationChatMessageVO> messages = stationChatDAO.getMessagesByStationIdPaginated(stationId, page * size,
                size);
        long totalElements = stationChatDAO.countMessagesByStationId(stationId);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return new PaginatedVO<>(messages, page, size, totalElements, totalPages);
    }

    /**
     * 依車站 id 取得該車站的公告列表，依建立時間新到舊排序。
     *
     * @param stationId
     * @return List<StationChatAnnouncementVO>
     * @throws StationNotFoundException 找不到指定車站。
     */
    public List<StationChatAnnouncementVO> getAnnouncements(Integer stationId) {
        if (!metroService.existsStationById(stationId)) {
            throw new StationNotFoundException("找不到 id:" + stationId + " 的車站!");
        }

        logger.debug("開始依車站 id 查詢車站聊天公告，stationId: {}", stationId);

        return stationChatDAO.getAnnouncementsByStationId(stationId);
    }

    /**
     * 新增一筆車站聊天公告。
     *
     * @param createAnnouncementDTO
     * @return MessageVO
     * @throws UserNotFoundException    找不到當前使用者。
     * @throws StationNotFoundException 找不到指定車站。
     */
    public MessageVO createAnnouncement(CreateAnnouncementDTO createAnnouncementDTO) {
        String email = getAuthenticatedEmail();
        logger.debug("開始呼叫 API 來新增車站聊天公告，email: {}, createAnnouncementDTO: {}", email, createAnnouncementDTO);

        User user = userDAO.getByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("找不到當前使用者!"));

        if (!metroService.existsStationById(createAnnouncementDTO.getStationId())) {
            throw new StationNotFoundException("找不到 id:" + createAnnouncementDTO.getStationId() + " 的車站!");
        }

        LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);

        StationChatAnnouncement announcement = new StationChatAnnouncement();
        announcement.setStationId(createAnnouncementDTO.getStationId());
        announcement.setContent(createAnnouncementDTO.getContent());
        announcement.setCreatedBy(user.getId());
        announcement.setCreatedAt(currentTime);
        announcement.setUpdatedAt(currentTime);

        stationChatDAO.insertAnnouncement(announcement);

        return new MessageVO("公告新增成功!");
    }

    /**
     * 編輯指定公告的內容。
     *
     * @param updateAnnouncementDTO
     * @return MessageVO
     * @throws StationChatNotFoundException 找不到指定公告或公告已刪除。
     */
    public MessageVO updateAnnouncement(UpdateAnnouncementDTO updateAnnouncementDTO) {
        logger.debug("開始呼叫 API 來編輯車站聊天公告，updateAnnouncementDTO: {}", updateAnnouncementDTO);

        StationChatAnnouncement announcement = stationChatDAO
                .getAnnouncementById(updateAnnouncementDTO.getAnnouncementId())
                .filter(existing -> existing.getDeletedAt() == null)
                .orElseThrow(() -> new StationChatNotFoundException(
                        "找不到 id:" + updateAnnouncementDTO.getAnnouncementId() + " 的公告!"));

        stationChatDAO.updateAnnouncementContentById(announcement.getId(), updateAnnouncementDTO.getContent(),
                LocalDateTime.now(ZoneOffset.UTC));

        return new MessageVO("公告編輯成功!");
    }

    /**
     * 軟刪除指定公告。
     *
     * @param announcementId
     * @return MessageVO
     * @throws StationChatNotFoundException 找不到指定公告或公告已刪除。
     */
    public MessageVO deleteAnnouncement(Integer announcementId) {
        logger.debug("開始呼叫 API 來刪除車站聊天公告，announcementId: {}", announcementId);

        StationChatAnnouncement announcement = stationChatDAO.getAnnouncementById(announcementId)
                .filter(existing -> existing.getDeletedAt() == null)
                .orElseThrow(() -> new StationChatNotFoundException("找不到 id:" + announcementId + " 的公告!"));

        stationChatDAO.softDeleteAnnouncementById(announcement.getId(), LocalDateTime.now(ZoneOffset.UTC));

        return new MessageVO("公告刪除成功!");
    }

    /**
     * 發送一則車站聊天留言（文字訊息或旅程分享）。
     *
     * @param stationId
     * @param sendMessageDTO
     * @param email
     * @return StationChatMessageVO 供 STOMP 廣播使用
     * @throws UserNotFoundException           找不到當前使用者。
     * @throws StationNotFoundException        找不到指定車站。
     * @throws IllegalArgumentException        留言類型無效，或 content／tripPlanId
     *                                         未依留言類型正確擇一提供。
     * @throws TripPlanNotFoundException       找不到指定旅程規劃，或該旅程規劃已被軟刪除。
     * @throws TripPlanAccessDeniedException   嘗試分享非本人擁有的旅程規劃。
     * @throws ChatDailyLimitExceededException 當日發送則數已達會員等級上限。
     */
    public StationChatMessageVO sendMessage(Integer stationId, SendMessageDTO sendMessageDTO, String email) {
        logger.debug("開始呼叫 API 來發送車站聊天留言，stationId: {}, email: {}, sendMessageDTO: {}", stationId, email,
                sendMessageDTO);

        User user = userDAO.getByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("找不到當前使用者!"));

        if (!metroService.existsStationById(stationId)) {
            throw new StationNotFoundException("找不到 id:" + stationId + " 的車站!");
        }

        ChatTypeEnum chatType = ChatTypeEnum.fromCode(sendMessageDTO.getChatType());

        if (chatType == ChatTypeEnum.TEXT) {
            if (sendMessageDTO.getContent() == null || sendMessageDTO.getContent().isBlank()) {
                throw new IllegalArgumentException("文字訊息類型必須輸入內容!");
            }
            if (sendMessageDTO.getTripPlanId() != null) {
                throw new IllegalArgumentException("文字訊息類型不可攜帶旅程規劃 id!");
            }
        } else {
            if (sendMessageDTO.getTripPlanId() == null) {
                throw new IllegalArgumentException("旅程分享類型必須輸入旅程規劃 id!");
            }
            if (sendMessageDTO.getContent() != null) {
                throw new IllegalArgumentException("旅程分享類型不可攜帶文字內容!");
            }

            Integer tripPlanOwnerId = stationChatDAO.getActiveTripPlanOwnerId(sendMessageDTO.getTripPlanId())
                    .orElseThrow(() -> new TripPlanNotFoundException(
                            "找不到 id:" + sendMessageDTO.getTripPlanId() + " 的旅程規劃!"));

            if (!tripPlanOwnerId.equals(user.getId())) {
                throw new TripPlanAccessDeniedException("不得分享非本人的旅程規劃!");
            }
        }

        int maxDailyChats = stationChatDAO.getMaxDailyChatsByUserId(user.getId());
        LocalDateTime todayStart = LocalDateTime.now(ZoneOffset.UTC).toLocalDate().atStartOfDay();
        long todayMessageCount = stationChatDAO.countTodayMessagesByUserId(user.getId(), todayStart);

        if (todayMessageCount >= maxDailyChats) {
            throw new ChatDailyLimitExceededException("已達每日聊天則數上限 (" + maxDailyChats + " 則)，請明日再試!");
        }

        StationChatMessage message = new StationChatMessage();
        message.setStationId(stationId);
        message.setUserId(user.getId());
        message.setTripPlanId(sendMessageDTO.getTripPlanId());
        message.setChatType(chatType.getCode());
        message.setContent(sendMessageDTO.getContent());
        message.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));

        stationChatDAO.insertMessage(message);

        return stationChatDAO.getMessageVOById(message.getId())
                .orElseThrow(() -> new StationChatNotFoundException("找不到剛新增的 id:" + message.getId() + " 留言!"));
    }

    /**
     * 刪除指定的車站聊天留言，本人或 ADMIN 皆可刪除。
     *
     * @param stationId
     * @param messageId
     * @param email
     * @throws UserNotFoundException            找不到當前使用者。
     * @throws StationChatNotFoundException     找不到指定留言、該留言已被軟刪除，或該留言不屬於指定車站。
     * @throws ChatMessageAccessDeniedException 非本人且非 ADMIN，無權刪除此留言。
     */
    public void deleteMessage(Integer stationId, Integer messageId, String email) {
        logger.debug("開始呼叫 API 來刪除車站聊天留言，stationId: {}, messageId: {}, email: {}", stationId, messageId, email);

        User user = userDAO.getByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("找不到當前使用者!"));

        StationChatMessage message = stationChatDAO.getMessageById(messageId)
                .filter(existing -> existing.getDeletedAt() == null)
                .filter(existing -> existing.getStationId().equals(stationId))
                .orElseThrow(() -> new StationChatNotFoundException("找不到 id:" + messageId + " 的留言!"));

        boolean isOwner = message.getUserId().equals(user.getId());
        boolean isAdmin = UserRole.fromId(user.getRoleId()) == UserRole.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ChatMessageAccessDeniedException("僅本人或管理員可以刪除此留言!");
        }

        stationChatDAO.softDeleteMessageById(message.getId(), LocalDateTime.now(ZoneOffset.UTC));
    }

    /**
     * 從 Spring Security Context 中取得當前已驗證使用者的 email。
     *
     * @return email
     */
    private String getAuthenticatedEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
