package com.mli.lookgo.module.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mli.lookgo.module.auth.dao.AuthDao;
import com.mli.lookgo.module.auth.exceptions.InvalidCredentialsException;
import com.mli.lookgo.module.auth.exceptions.UserNotFoundException;
import com.mli.lookgo.module.auth.model.entity.User;
import com.mli.lookgo.module.auth.model.vo.UserVO;
import com.mli.lookgo.module.auth.security.CookieUtil;
import com.mli.lookgo.module.auth.security.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 處理使用者資料查詢相關的業務邏輯。
 *
 * @author D5042101
 * @since 2026.5.30
 */
@Service
public class UserService {

    private final AuthDao authDao;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入所需的依賴。
     *
     * @param authDao
     * @param jwtUtil
     * @param cookieUtil
     */
    public UserService(AuthDao authDao, JwtUtil jwtUtil, CookieUtil cookieUtil) {
        this.authDao = authDao;
        this.jwtUtil = jwtUtil;
        this.cookieUtil = cookieUtil;
    }

    /**
     * 解析 HttpOnly Cookie 中的刷新令牌，取得對應的使用者資訊。
     *
     * @param request
     * @return UserVO
     * @throws InvalidCredentialsException 刷新令牌無效或已過期。
     * @throws UserNotFoundException       找不到對應使用者。
     */
    public UserVO getCurrentUser(HttpServletRequest request) {
        String refreshToken = cookieUtil.getRefreshTokenFromCookie(request);

        if (refreshToken == null || !jwtUtil.validateRefreshToken(refreshToken)) {
            throw new InvalidCredentialsException("刷新令牌無效或已過期!");
        }

        String email = jwtUtil.getEmailFromRefreshToken(refreshToken);
        logger.info("開始呼叫 API 來查詢當前使用者資料，email: {}", email);
        User user = authDao.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("找不到當前使用者!"));

        return toVO(user);
    }

    /**
     * 定義一個私有輔助方法，把傳入的 entity 轉換為 VO。
     *
     * @param user
     * @return UserVO
     */
    private UserVO toVO(User user) {
        return new UserVO(user.getId(), user.getEmail(), user.getUsername(), user.getDepartmentId(),
                user.getCreatedAt());
    }
}
