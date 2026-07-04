package com.mli.lookgo.module.stationChat.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.mli.lookgo.module.stationChat.model.entity.StationChatAnnouncement;
import com.mli.lookgo.module.stationChat.model.vo.StationChatAnnouncementVO;
import com.mli.lookgo.module.stationChat.model.vo.StationChatMessageVO;

/**
 * 和資料庫交互，處理站點聊天留言與公告相關的操作。
 *
 * @author D5042101
 * @since 2026.07.03
 */
@Mapper
public interface StationChatDAO {

    /**
     * 依車站 id 分頁取得站點聊天留言，並依 chatType 組裝文字訊息或旅程分享內容。
     *
     * @param stationId
     * @param offset
     * @param limit
     * @return List<StationChatMessageVO>
     */
    List<StationChatMessageVO> getMessagesByStationIdPaginated(@Param("stationId") Integer stationId,
            @Param("offset") int offset,
            @Param("limit") int limit);

    /**
     * 計算指定車站的站點聊天留言總筆數。
     *
     * @param stationId
     * @return 總筆數
     */
    long countMessagesByStationId(@Param("stationId") Integer stationId);

    /**
     * 依車站 id 取得該站點的公告列表，依建立時間新到舊排序。
     *
     * @param stationId
     * @return List<StationChatAnnouncementVO>
     */
    List<StationChatAnnouncementVO> getAnnouncementsByStationId(@Param("stationId") Integer stationId);

    /**
     * 用 id 查詢指定公告的資料。
     *
     * @param id
     * @return Optional<StationChatAnnouncement>
     */
    Optional<StationChatAnnouncement> getAnnouncementById(@Param("id") Integer id);

    /**
     * 新增一筆站點聊天公告。
     *
     * @param announcement
     * @return 影響筆數
     */
    int insertAnnouncement(@Param("announcement") StationChatAnnouncement announcement);

    /**
     * 用 id 更新指定公告的內容。
     *
     * @param id
     * @param content
     * @param updatedAt
     * @return 影響筆數
     */
    int updateAnnouncementContentById(@Param("id") Integer id,
            @Param("content") String content,
            @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 用 id 軟刪除指定公告。
     *
     * @param id
     * @param deletedAt
     * @return 影響筆數
     */
    int softDeleteAnnouncementById(@Param("id") Integer id, @Param("deletedAt") LocalDateTime deletedAt);
}
