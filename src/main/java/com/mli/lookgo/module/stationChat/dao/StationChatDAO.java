package com.mli.lookgo.module.stationChat.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.mli.lookgo.module.stationChat.model.entity.StationChatAnnouncement;
import com.mli.lookgo.module.stationChat.model.entity.StationChatMessage;
import com.mli.lookgo.module.stationChat.model.vo.StationChatAnnouncementVO;
import com.mli.lookgo.module.stationChat.model.vo.StationChatMessageVO;

/**
 * 和資料庫交互，處理車站聊天留言與公告相關的操作。
 *
 * @author D5042101
 * @since 2026.07.03
 */
@Mapper
public interface StationChatDAO {

        /**
         * 依車站 id 分頁取得車站聊天留言，並依 chatType 組裝文字訊息或旅程分享內容。
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
         * 計算指定車站的車站聊天留言總筆數。
         *
         * @param stationId
         * @return 總筆數
         */
        long countMessagesByStationId(@Param("stationId") Integer stationId);

        /**
         * 依車站 id 取得該車站的公告列表，依建立時間新到舊排序。
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
         * 新增一筆車站聊天公告。
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

        /**
         * 用 id 查詢單筆車站聊天留言，並依 chatType 組裝文字訊息或旅程分享內容，供發送留言後即時廣播使用。
         *
         * @param id
         * @return Optional<StationChatMessageVO>
         */
        Optional<StationChatMessageVO> getMessageVOById(@Param("id") Integer id);

        /**
         * 用 id 查詢指定留言的原始資料。
         *
         * @param id
         * @return Optional<StationChatMessage>
         */
        Optional<StationChatMessage> getMessageById(@Param("id") Integer id);

        /**
         * 新增一筆車站聊天留言。
         *
         * @param message
         * @return 影響筆數
         */
        int insertMessage(@Param("message") StationChatMessage message);

        /**
         * 用 id 軟刪除指定留言。
         *
         * @param id
         * @param deletedAt
         * @return 影響筆數
         */
        int softDeleteMessageById(@Param("id") Integer id, @Param("deletedAt") LocalDateTime deletedAt);

        /**
         * 依使用者 id 取得其會員等級對應的每日聊天則數上限。
         *
         * @param userId
         * @return 每日則數上限
         */
        int getMaxDailyChatsByUserId(@Param("userId") Integer userId);

        /**
         * 計算指定使用者今日（自 todayStart 起）已發送的留言則數。
         *
         * @param userId
         * @param todayStart
         * @return 今日已發送則數
         */
        long countTodayMessagesByUserId(@Param("userId") Integer userId, @Param("todayStart") LocalDateTime todayStart);

        /**
         * 用 id 查詢指定旅程規劃目前是否存在且未被軟刪除，並回傳其擁有者 user id。
         *
         * @param tripPlanId
         * @return Optional<Integer> 旅程規劃擁有者的 user id，查無資料或已軟刪除則為空
         */
        Optional<Integer> getActiveTripPlanOwnerId(@Param("tripPlanId") Integer tripPlanId);
}
