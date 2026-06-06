package com.mli.lookgo.module.auth.dao;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.mli.lookgo.module.auth.model.dto.SignupDTO;
import com.mli.lookgo.module.auth.model.entity.User;

/**
 * 和資料庫交互，處理使用者身分驗證相關的操作。
 *
 * @author D5042101
 * @since 2026.5.30
 */
@Repository
public class AuthDao {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 初始化使用者身分驗證的存取物件，並設定所需的 JDBC 操作依賴。
     *
     * @param jdbcTemplate {@link JdbcTemplate}
     */
    public AuthDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 建立一個使用者。
     *
     * @param signupDTO
     * @param hashedPassword
     * @param createdAt
     * @return id (資料庫自動產生)
     */
    public Long createUser(SignupDTO signupDTO, String hashedPassword, LocalDate createdAt) {
        String sql = """
                INSERT INTO users (email, username, password, department_id, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            preparedStatement.setString(1, signupDTO.getEmail());
            preparedStatement.setString(2, signupDTO.getUsername());
            preparedStatement.setString(3, hashedPassword);
            preparedStatement.setLong(4, signupDTO.getDepartmentId());
            preparedStatement.setObject(5, createdAt);

            return preparedStatement;
        }, keyHolder);

        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    /**
     * 用 email 查詢指定使用者的資料是否存在。
     *
     * @param email
     * @return 查詢使用者是否存在的結果
     */
    public boolean existsByEmail(String email) {
        String sql = """
                SELECT 1
                FROM users
                WHERE email = ?
                LIMIT 1
                """;

        List<Integer> rows = jdbcTemplate.queryForList(sql, Integer.class, email);

        return !rows.isEmpty();
    }

    /**
     * 用 email 查詢指定使用者的資料。
     *
     * @param email
     * @return Optional<User>
     */
    public Optional<User> findByEmail(String email) {
        String sql = """
                SELECT id, email, username, password, department_id, created_at
                FROM users
                WHERE email = ?
                """;

        try {
            User user = jdbcTemplate.queryForObject(sql, new DataClassRowMapper<>(User.class), email);

            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException error) {
            return Optional.empty();
        }
    }
}
