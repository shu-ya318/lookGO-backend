package com.mli.lookgo.module.tripPlan.dao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.mli.lookgo.module.tripPlan.model.entity.UserTripPlan;
import com.mli.lookgo.module.tripPlan.model.vo.TripPlanVO;

/**
 * 和資料庫交互，處理使用者旅程規劃相關的操作。
 *
 * @author D5042101
 * @since 2026.07.06
 */
@Mapper
public interface TripPlanDAO {

    /**
     * 取得指定使用者分頁與模糊搜尋（旅程名稱）後的旅程規劃列表，依建立時間新到舊排序。
     *
     * @param userId
     * @param keyword
     * @param offset
     * @param limit
     * @return List<TripPlanVO>
     */
    List<TripPlanVO> getAllPaginatedByUserId(@Param("userId") Integer userId,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("limit") int limit);

    /**
     * 計算指定使用者符合搜尋條件的旅程規劃總筆數。
     *
     * @param userId
     * @param keyword
     * @return 總筆數
     */
    long countAllByUserId(@Param("userId") Integer userId, @Param("keyword") String keyword);

    /**
     * 用 id 查詢指定旅程規劃的顯示用資料（含起訖站名稱），僅限有效（未軟刪除）的資料。
     *
     * @param id
     * @return Optional<TripPlanVO>
     */
    Optional<TripPlanVO> getById(@Param("id") Integer id);

    /**
     * 用 id 查詢指定旅程規劃目前是否存在且未被軟刪除，並回傳其擁有者 user id。
     *
     * @param id
     * @return Optional<Integer> 旅程規劃擁有者的 user id，查無資料或已軟刪除則為空
     */
    Optional<Integer> getActiveOwnerId(@Param("id") Integer id);

    /**
     * 計算指定使用者目前有效（未軟刪除）的旅程規劃筆數，供新增前檢查會員等級數量上限使用。
     *
     * @param userId
     * @return 有效旅程規劃筆數
     */
    int countActiveByUserId(@Param("userId") Integer userId);

    /**
     * 依使用者 id 取得其會員等級對應的旅程規劃數量上限。
     *
     * @param userId
     * @return 旅程規劃數量上限
     */
    int getMaxTripPlansByUserId(@Param("userId") Integer userId);

    /**
     * 新增一筆旅程規劃。
     *
     * @param tripPlan
     * @return 影響筆數
     */
    int insert(@Param("tripPlan") UserTripPlan tripPlan);

    /**
     * 用 id 軟刪除指定旅程規劃。
     *
     * @param id
     * @param deletedAt
     * @return 影響筆數
     */
    int softDeleteById(@Param("id") Integer id, @Param("deletedAt") LocalDateTime deletedAt);

    /**
     * 用 id 更新指定旅程規劃的名稱。
     *
     * @param id
     * @param name
     * @param updatedAt
     * @return 影響筆數
     */
    int updateNameById(@Param("id") Integer id, @Param("name") String name, @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 用 id 更新指定旅程規劃的資訊（起訖站以外）。
     *
     * @param id
     * @param fareType
     * @param farePrice
     * @param transferCount
     * @param routingStrategy
     * @param notes
     * @param updatedAt
     * @return 影響筆數
     */
    int updateInfoById(@Param("id") Integer id,
            @Param("fareType") Integer fareType,
            @Param("farePrice") BigDecimal farePrice,
            @Param("transferCount") Integer transferCount,
            @Param("routingStrategy") Integer routingStrategy,
            @Param("notes") String notes,
            @Param("updatedAt") LocalDateTime updatedAt);
}
