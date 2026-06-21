package com.mli.lookgo.module.auth.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.mli.lookgo.module.user.model.entity.User;

/**
 * 和資料庫交互，處理使用者身分驗證相關的操作。
 *
 * @author D5042101
 * @since 2026.06.06
 */
@Mapper
public interface AuthDao {

        /**
         * 建立一個使用者。
         *
         * @param signupDTO
         * @return 影響筆數
         */
        int createUser(@Param("user") User user);

        /**
         * 用 email 查詢指定使用者是否存在。
         *
         * @param email
         * @return 存在為 true，否則 false
         */
        boolean existsByEmail(@Param("email") String email);

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
                        @Param("updatedAt") java.time.LocalDateTime updatedAt);
}
