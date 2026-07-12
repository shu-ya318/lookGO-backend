package com.mli.lookgo.module.stationBookmark.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.mli.lookgo.module.stationBookmark.model.entity.UserStationBookmark;
import com.mli.lookgo.module.stationBookmark.model.vo.StationBookmarkVO;

/**
 * 和資料庫交互，處理車站書籤相關的操作。
 *
 * @author D5042101
 * @since 2026.07.06
 */
@Mapper
public interface StationBookmarkDAO {

    /**
     * 取得指定使用者分頁與模糊搜尋後的車站書籤列表，依收藏時間排序。
     *
     * @param userId
     * @param keyword
     * @param offset
     * @param limit
     * @param sortDirection 排序方向（ASC 或 DESC），須由呼叫端先以白名單驗證
     * @return List<StationBookmarkVO>
     */
    List<StationBookmarkVO> getAllPaginated(@Param("userId") Integer userId,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("sortDirection") String sortDirection);

    /**
     * 計算指定使用者符合搜尋條件的車站書籤總筆數。
     *
     * @param userId
     * @param keyword
     * @return 總筆數
     */
    long countAll(@Param("userId") Integer userId, @Param("keyword") String keyword);

    /**
     * 取得指定使用者所有有效（未軟刪除）的車站書籤，依收藏時間新到舊排序，供匯出 excel 使用。
     *
     * @param userId
     * @return List<StationBookmarkVO>
     */
    List<StationBookmarkVO> getAllActive(@Param("userId") Integer userId);

    /**
     * 用 id 查詢指定書籤的原始資料。
     *
     * @param id
     * @return Optional<UserStationBookmark>
     */
    Optional<UserStationBookmark> getById(@Param("id") Integer id);

    /**
     * 用 id 查詢指定書籤的顯示用資料（含車站與使用者資訊），僅限有效（未軟刪除）的資料，供新增書籤後組裝回應使用。
     *
     * @param id
     * @return Optional<StationBookmarkVO>
     */
    Optional<StationBookmarkVO> getVOById(@Param("id") Integer id);

    /**
     * 依使用者 id 與車站中文名稱模糊搜尋，取得該使用者單一有效（未軟刪除）的車站書籤，若比對到多筆則取收藏時間最新的一筆。
     *
     * @param userId
     * @param stationName
     * @return Optional<StationBookmarkVO>
     */
    Optional<StationBookmarkVO> getActiveVOByUserIdAndStationNameLike(@Param("userId") Integer userId,
            @Param("stationName") String stationName);

    /**
     * 查詢指定使用者對指定車站是否已存在有效（未軟刪除）的書籤。
     *
     * @param userId
     * @param stationId
     * @return Optional<Integer> 已存在則為該書籤 id，否則為空
     */
    Optional<Integer> getActiveBookmarkIdByUserIdAndStationId(@Param("userId") Integer userId,
            @Param("stationId") Integer stationId);

    /**
     * 計算指定使用者目前有效（未軟刪除）的車站書籤筆數，供新增前檢查會員等級數量上限使用。
     *
     * @param userId
     * @return 有效書籤筆數
     */
    int countActiveByUserId(@Param("userId") Integer userId);

    /**
     * 依使用者 id 取得其會員等級對應的車站書籤數量上限。
     *
     * @param userId
     * @return 書籤數量上限
     */
    int getMaxBookmarksByUserId(@Param("userId") Integer userId);

    /**
     * 新增一筆車站書籤。
     *
     * @param bookmark
     * @return 影響筆數
     */
    int insert(@Param("bookmark") UserStationBookmark bookmark);

    /**
     * 用 id 軟刪除指定書籤。
     *
     * @param id
     * @param deletedAt
     * @return 影響筆數
     */
    int softDeleteById(@Param("id") Integer id, @Param("deletedAt") LocalDateTime deletedAt);
}
