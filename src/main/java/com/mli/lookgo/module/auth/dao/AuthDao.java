package com.mli.lookgo.module.auth.dao;

import java.time.LocalDateTime;
import java.util.Optional;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import com.mli.lookgo.module.auth.model.dto.SignupDTO;
import com.mli.lookgo.module.auth.model.entity.User;

/**
 * 和資料庫交互，處理使用者身分驗證相關的操作。
 *
 * @author D5042101
 * @since 2026.5.30
 */
@Mapper
public interface AuthDao {

    /**
     * 建立一個使用者，預設 role_id = 1 (USER)，並將自動產生的 id 回填至 entity。
     *
     * @param signupDTO      前端傳入的註冊資料
     * @param hashedPassword 加密後的密碼
     * @param now            建立與更新時間
     * @return 影響筆數
     */
    @Insert("""
            INSERT INTO [dbo].[users]
                (membership_tier_id, role_id, email, password, username, created_at, updated_at)
            VALUES
                (#{signupDTO.membershipTierId}, 1, #{signupDTO.email}, #{hashedPassword},
                 #{signupDTO.username}, #{now}, #{now})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "signupDTO.id", keyColumn = "id")
    int createUser(@Param("signupDTO") SignupDTO signupDTO,
            @Param("hashedPassword") String hashedPassword,
            @Param("now") LocalDateTime now);

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
     * 用 email 查詢指定使用者的資料。
     *
     * @param email
     * @return Optional 包裝的 User entity
     */
    @Select("""
            SELECT id, membership_tier_id, role_id, email, password, username, created_at, updated_at
            FROM [dbo].[users]
            WHERE email = #{email}
            """)
    @Results(id = "userResultMap", value = {
            @Result(property = "id",               column = "id"),
            @Result(property = "membershipTierId", column = "membership_tier_id"),
            @Result(property = "roleId",           column = "role_id"),
            @Result(property = "email",            column = "email"),
            @Result(property = "password",         column = "password"),
            @Result(property = "username",         column = "username"),
            @Result(property = "createdAt",        column = "created_at"),
            @Result(property = "updatedAt",        column = "updated_at")
    })
    Optional<User> findByEmail(@Param("email") String email);
}
