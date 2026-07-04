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
import com.mli.lookgo.module.stationChat.exceptions.StationChatNotFoundException;
import com.mli.lookgo.module.stationChat.model.dto.CreateAnnouncementDTO;
import com.mli.lookgo.module.stationChat.model.dto.UpdateAnnouncementDTO;
import com.mli.lookgo.module.stationChat.model.entity.StationChatAnnouncement;
import com.mli.lookgo.module.stationChat.model.vo.StationChatAnnouncementVO;
import com.mli.lookgo.module.stationChat.model.vo.StationChatMessageVO;
import com.mli.lookgo.module.user.dao.UserDAO;
import com.mli.lookgo.module.user.exceptions.UserNotFoundException;
import com.mli.lookgo.module.user.model.entity.User;

/**
 * 處理站點聊天留言與公告相關業務邏輯。
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
     * 依車站 id 分頁取得該站點的歷史聊天留言，依建立時間新到舊排序。
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

        logger.debug("開始依車站 id 分頁查詢站點聊天留言，stationId: {}, page: {}, size: {}", stationId, page, size);

        List<StationChatMessageVO> messages = stationChatDAO.getMessagesByStationIdPaginated(stationId, page * size,
                size);
        long totalElements = stationChatDAO.countMessagesByStationId(stationId);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return new PaginatedVO<>(messages, page, size, totalElements, totalPages);
    }

    /**
     * 依車站 id 取得該站點的公告列表，依建立時間新到舊排序。
     *
     * @param stationId
     * @return List<StationChatAnnouncementVO>
     * @throws StationNotFoundException 找不到指定車站。
     */
    public List<StationChatAnnouncementVO> getAnnouncements(Integer stationId) {
        if (!metroService.existsStationById(stationId)) {
            throw new StationNotFoundException("找不到 id:" + stationId + " 的車站!");
        }

        logger.debug("開始依車站 id 查詢站點聊天公告，stationId: {}", stationId);

        return stationChatDAO.getAnnouncementsByStationId(stationId);
    }

    /**
     * 新增一筆站點聊天公告。
     *
     * @param createAnnouncementDTO
     * @return MessageVO
     * @throws UserNotFoundException     找不到當前使用者。
     * @throws StationNotFoundException 找不到指定車站。
     */
    public MessageVO createAnnouncement(CreateAnnouncementDTO createAnnouncementDTO) {
        String email = getAuthenticatedEmail();
        logger.debug("開始呼叫 API 來新增站點聊天公告，email: {}, createAnnouncementDTO: {}", email, createAnnouncementDTO);

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
        logger.debug("開始呼叫 API 來編輯站點聊天公告，updateAnnouncementDTO: {}", updateAnnouncementDTO);

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
        logger.debug("開始呼叫 API 來刪除站點聊天公告，announcementId: {}", announcementId);

        StationChatAnnouncement announcement = stationChatDAO.getAnnouncementById(announcementId)
                .filter(existing -> existing.getDeletedAt() == null)
                .orElseThrow(() -> new StationChatNotFoundException("找不到 id:" + announcementId + " 的公告!"));

        stationChatDAO.softDeleteAnnouncementById(announcement.getId(), LocalDateTime.now(ZoneOffset.UTC));

        return new MessageVO("公告刪除成功!");
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
