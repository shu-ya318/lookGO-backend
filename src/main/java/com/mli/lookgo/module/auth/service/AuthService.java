package com.mli.lookgo.module.auth.service;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mli.lookgo.module.auth.dao.AuthDao;
import com.mli.lookgo.module.auth.exceptions.InvalidCredentialsException;
import com.mli.lookgo.module.auth.exceptions.UserDuplicateException;
import com.mli.lookgo.module.auth.model.dto.LoginDTO;
import com.mli.lookgo.module.auth.model.dto.SignupDTO;
import com.mli.lookgo.module.auth.model.entity.User;
import com.mli.lookgo.module.auth.model.vo.AuthVO;
import com.mli.lookgo.module.auth.security.CookieUtil;
import com.mli.lookgo.module.auth.security.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 處理使用者身分驗證相關的業務邏輯。
 *
 * @author D5042101
 * @since 2026.06.06
 */
@Service
public class AuthService {

    private final AuthDao authDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入所需的依賴。
     *
     * @param authDao
     * @param passwordEncoder
     * @param jwtUtil
     * @param cookieUtil
     */
    public AuthService(AuthDao authDao, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, CookieUtil cookieUtil) {
        this.authDao = authDao;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.cookieUtil = cookieUtil;
    }

    /**
     * 把傳入資訊寫入資料庫，建立一筆對應的使用者帳號，並回傳存取憑證。
     *
     * @param signupDTO
     * @param response
     * @return AuthVO
     * @throws UserDuplicateException Email 已被使用的錯誤。
     */
    @Transactional
    public AuthVO signup(SignupDTO signupDTO, HttpServletResponse response) {
        if (authDao.existsByEmail(signupDTO.getEmail())) {
            throw new UserDuplicateException("Email: " + signupDTO.getEmail() + " 已被使用，請換一個 email!");
        }

        String hashedPassword = passwordEncoder.encode(signupDTO.getPassword());
        LocalDate createdAt = LocalDate.now();
        logger.info("開始呼叫 API 來建立使用者帳號，輸入內容: {}, 建立時間: {}", signupDTO, createdAt);
        Long generatedId = authDao.createUser(signupDTO, hashedPassword, createdAt);

        String accessToken = jwtUtil.generateAccessToken(generatedId, UUID.randomUUID(), signupDTO.getEmail(),
                Set.of("ROLE_USER"));
        String refreshToken = jwtUtil.generateRefreshToken(signupDTO.getEmail());
        cookieUtil.addRefreshTokenCookie(response, refreshToken);

        return new AuthVO(accessToken, refreshToken);
    }

    /**
     * 驗證使用者帳號與密碼，通過後回傳存取憑證。
     *
     * @param loginDTO
     * @param response
     * @return AuthVO
     * @throws InvalidCredentialsException
     */
    public AuthVO signin(LoginDTO loginDTO, HttpServletResponse response) {
        logger.info("開始呼叫 API 來驗證使用者身分，輸入內容: {}", loginDTO);
        User user = authDao.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("帳號或密碼錯誤!"));

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("帳號或密碼錯誤!");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), UUID.randomUUID(), user.getEmail(),
                Set.of("ROLE_USER"));
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        cookieUtil.addRefreshTokenCookie(response, refreshToken);

        return new AuthVO(accessToken, refreshToken);
    }

    /**
     * 驗證 Cookie 中的刷新令牌，通過後核發新的存取憑證與刷新憑證。
     *
     * @param request
     * @param response
     * @return AuthVO
     * @throws InvalidCredentialsException 刷新令牌無效或已過期。
     */
    public AuthVO refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieUtil.getRefreshTokenFromCookie(request);

        if (refreshToken == null || !jwtUtil.validateRefreshToken(refreshToken)) {
            throw new InvalidCredentialsException("刷新令牌無效或已過期!");
        }

        String email = jwtUtil.getEmailFromRefreshToken(refreshToken);
        logger.info("開始呼叫 API 來刷新憑證，email: {}", email);
        User user = authDao.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("刷新令牌無效或已過期!"));

        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), UUID.randomUUID(), user.getEmail(),
                Set.of("ROLE_USER"));
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        cookieUtil.addRefreshTokenCookie(response, newRefreshToken);

        return new AuthVO(newAccessToken, newRefreshToken);
    }
}
