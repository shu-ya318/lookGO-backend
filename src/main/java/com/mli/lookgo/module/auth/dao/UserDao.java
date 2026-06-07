package com.mli.lookgo.module.auth.dao;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import com.mli.lookgo.module.auth.model.entity.User;

/**
 * 和資料庫交互，處理使用者資料查詢相關的操作。
 *
 * @author D5042101
 * @since 2026.06.06
 */
@Mapper
public interface UserDao {

    /**
     * 用 email 查詢指定使用者的資料。
     *
     * @param email
     * @return Optional<User>
     */
    @Select("""
            SELECT id, membership_tier_id, role_id, email, password, username, birth_date, status, created_at, updated_at, last_login_at
            FROM [dbo].[users]
            WHERE email = #{email}
            """)
    @Results(id = "userResultMap", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "membershipTierId", column = "membership_tier_id"),
            @Result(property = "roleId", column = "role_id"),
            @Result(property = "email", column = "email"),
            @Result(property = "password", column = "password"),
            @Result(property = "username", column = "username"),
            @Result(property = "birthDate", column = "birth_date"),
            @Result(property = "status", column = "status"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "lastLoginAt", column = "last_login_at")
    })
    Optional<User> getByEmail(@Param("email") String email);
}
