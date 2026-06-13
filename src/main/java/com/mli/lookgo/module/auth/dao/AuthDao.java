package com.mli.lookgo.module.auth.dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.mli.lookgo.module.auth.model.entity.User;

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
        @Insert("""
                        INSERT INTO [dbo].[users]
                            (membership_tier_id, role_id, email, password, username, birth_date, status, created_at, updated_at, last_login_at)
                        VALUES
                            (#{user.membershipTierId}, #{user.roleId}, #{user.email}, #{user.password},
                             #{user.username}, #{user.birthDate}, #{user.status}, #{user.createdAt}, #{user.updatedAt}, #{user.lastLoginAt})
                        """)
        @Options(useGeneratedKeys = true, keyProperty = "user.id", keyColumn = "id")
        int createUser(@Param("user") User user);

        /**
         * 用 email 查詢指定使用者是否存在。
         *
         * @param email
         * @return 存在為 true，否則 false
         */
        @Select("""
                        SELECT COUNT(1)
                        FROM [dbo].[users]
                        WHERE email = #{email}
                        """)
        boolean existsByEmail(@Param("email") String email);

        /**
         * 用 email 更新指定使用者的密碼。
         *
         * @param email
         * @param newPassword
         * @param updatedAt
         * @return 影響筆數
         */
        @Update("""
                        UPDATE [dbo].[users]
                        SET password = #{newPassword}, updated_at = #{updatedAt}
                        WHERE email = #{email}
                        """)
        int updatePasswordByEmail(@Param("email") String email,
                        @Param("newPassword") String newPassword,
                        @Param("updatedAt") java.time.LocalDateTime updatedAt);
}
