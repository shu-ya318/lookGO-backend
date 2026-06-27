package com.mli.lookgo.module.user.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.mli.lookgo.module.user.model.entity.User;

/**
 * 和資料庫交互，處理使用者資料查詢相關的操作。
 *
 * @author D5042101
 * @since 2026.06.06
 */
@Mapper
public interface UserDAO {

        /**
         * 用 email 查詢指定使用者的資料。
         *
         * @param email
         * @return Optional<User>
         */
        Optional<User> getByEmail(@Param("email") String email);

        /**
         * 取得所有使用者的資料。
         *
         * @return List<User>
         */
        List<User> getAll();

        /**
         * 取得分頁與模糊搜尋後的使用者資料列表。
         *
         * @param keyword
         * @param offset
         * @param limit
         * @return List<User>
         */
        List<User> getAllPaginated(@Param("keyword") String keyword,
                        @Param("offset") int offset,
                        @Param("limit") int limit);

        /**
         * 計算符合搜尋條件的使用者總筆數。
         *
         * @param keyword
         * @return 總筆數
         */
        long countAll(@Param("keyword") String keyword);

        /**
         * 用 email 更新指定使用者的名稱。
         *
         * @param email
         * @param username
         * @param updatedAt
         * @return 影響筆數
         */
        int updateUsernameByEmail(@Param("email") String email,
                        @Param("username") String username,
                        @Param("updatedAt") LocalDateTime updatedAt);

        /**
         * 用 email 更新指定使用者的密碼。
         *
         * @param email
         * @param newPassword
         * @param updatedAt
         * @return 影響筆數
         */
        int updatePasswordByEmail(@Param("email") String email,
                        @Param("newPassword") String newPassword,
                        @Param("updatedAt") LocalDateTime updatedAt);

        /**
         * 用 email 更新指定使用者的出生日期。
         *
         * @param email
         * @param birthDate
         * @param updatedAt
         * @return 影響筆數
         */
        int updateBirthDateByEmail(@Param("email") String email,
                        @Param("birthDate") LocalDate birthDate,
                        @Param("updatedAt") LocalDateTime updatedAt);

        /**
         * 用 id 查詢指定使用者的資料。
         *
         * @param id
         * @return Optional<User>
         */
        Optional<User> getById(@Param("id") Integer id);

        /**
         * 用 id 更新指定使用者的帳號狀態。
         *
         * @param id
         * @param status
         * @param updatedAt
         * @return 影響筆數
         */
        int updateStatusById(@Param("id") Integer id,
                        @Param("status") Integer status,
                        @Param("updatedAt") LocalDateTime updatedAt);
}
