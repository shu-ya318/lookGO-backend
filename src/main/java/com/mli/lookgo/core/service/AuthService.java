package com.mli.lookgo.core.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mli.lookgo.core.dao.AuthDAO;
import com.mli.lookgo.core.exceptions.ForgetPasswordVerificationException;
import com.mli.lookgo.core.exceptions.InvalidCredentialsException;
import com.mli.lookgo.core.exceptions.UserDuplicateException;
import com.mli.lookgo.core.model.dto.ForgetPasswordDTO;
import com.mli.lookgo.core.model.dto.LoginDTO;
import com.mli.lookgo.core.model.dto.ResetPasswordDTO;
import com.mli.lookgo.core.model.dto.SignupDTO;
import com.mli.lookgo.core.model.vo.AuthVO;
import com.mli.lookgo.core.model.vo.ForgetPasswordVO;
import com.mli.lookgo.core.result.UpdatePasswordVO;
import com.mli.lookgo.core.security.JwtUtil;
import com.mli.lookgo.module.user.dao.UserDAO;
import com.mli.lookgo.module.user.enums.MembershipTier;
import com.mli.lookgo.module.user.enums.UserRole;
import com.mli.lookgo.module.user.enums.UserStatus;
import com.mli.lookgo.module.user.exceptions.UserNotFoundException;
import com.mli.lookgo.module.user.model.entity.User;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 處理使用者身分驗證相關的業務邏輯。
 *
 * @author D5042101
 * @since 2026.06.06
 */
@Service
public class AuthService {

    private final RedisService redisService;
    private final AuthDAO authDAO;
    private final UserDAO userDAO;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    @Value("${frontend.base-url}")
    private String frontendBaseUrl;

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入所需的依賴。
     *
     * @param redisService
     * @param authDAO
     * @param userDAO
     * @param passwordEncoder
     * @param jwtUtil
     */
    public AuthService(RedisService redisService, AuthDAO authDAO, UserDAO userDAO, PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil) {
        this.redisService = redisService;
        this.authDAO = authDAO;
        this.userDAO = userDAO;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 把傳入資訊寫入資料庫，建立一筆對應的使用者帳號，並回傳存取token。
     * 若註冊時已填寫出生日期，會員等級直接以 PREMIUM 建立，否則為 BASIC。
     *
     * @param signupDTO
     * @param httpServletResponse
     * @return AuthVO
     * @throws UserDuplicateException
     */
    @Transactional
    public AuthVO signup(SignupDTO signupDTO, HttpServletResponse httpServletResponse) {
        if (authDAO.existsByEmail(signupDTO.getEmail())) {
            throw new UserDuplicateException("Email: " + signupDTO.getEmail() + " 已被使用，請換一個 email!");
        }

        String hashedPassword = passwordEncoder.encode(signupDTO.getPassword());
        LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);

        MembershipTier initialTier = signupDTO.getBirthDate() != null
                ? MembershipTier.PREMIUM
                : MembershipTier.BASIC;

        User user = new User();
        user.setMembershipTierId(initialTier.getId());
        user.setRoleId(UserRole.USER.getId());
        user.setEmail(signupDTO.getEmail());
        user.setPassword(hashedPassword);
        user.setUsername(signupDTO.getUsername());
        user.setBirthDate(signupDTO.getBirthDate());
        user.setCellphone(signupDTO.getCellphone());
        user.setStatus(UserStatus.ACTIVE.getCode());
        user.setCreatedAt(currentTime);
        user.setUpdatedAt(currentTime);
        user.setLastLoginAt(currentTime);

        logger.debug("開始呼叫 API 來建立使用者帳號，寫入資料: {}, 建立時間: {}", user, currentTime);
        authDAO.createUser(user);

        Integer userId = user.getId();

        return generateTokens(userId, user.getEmail());
    }

    /**
     * 驗證使用者帳號與密碼，通過後回傳存取token。
     *
     * @param loginDTO
     * @param httpServletResponse
     * @return AuthVO
     * @throws InvalidCredentialsException
     */
    public AuthVO login(LoginDTO loginDTO, HttpServletResponse httpServletResponse) {
        logger.debug("開始呼叫 API 來驗證使用者身分，輸入內容: {}", loginDTO);
        User user = userDAO.getByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("帳號或密碼錯誤!"));

        if (user.getStatus() != UserStatus.ACTIVE.getCode()) {
            throw new InvalidCredentialsException("您的帳號已被停用，如有問題請聯繫管理員!");
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("帳號或密碼錯誤!");
        }

        return generateTokens(user.getId(), user.getEmail());
    }

    /**
     * 驗證刷新token，通過後核發新的存取token與刷新token。
     *
     * @param refreshToken
     * @return AuthVO
     * @throws InvalidCredentialsException
     */
    public AuthVO refreshTokens(String refreshToken) {
        String email = jwtUtil.getEmailFromRefreshToken(refreshToken);
        logger.debug("開始呼叫 API 來刷新token，email: {}", email);
        User user = userDAO.getByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("刷新token無效或已過期!"));

        if (user.getStatus() != UserStatus.ACTIVE.getCode()) {
            throw new InvalidCredentialsException("該帳號已被停用!");
        }

        String storedRefreshTokenJti = redisService.getRefreshTokenJti(user.getId().toString());
        String refreshTokenJti = jwtUtil.getJtiFromToken(refreshToken);
        if (storedRefreshTokenJti == null || !storedRefreshTokenJti.equals(refreshTokenJti)) {
            throw new InvalidCredentialsException("刷新token無效或已過期!");
        }

        return generateTokens(user.getId(), user.getEmail());
    }

    /**
     * 驗證 email 與 cellphone 是否與資料庫一致，通過後生成重設密碼 token 存入 Redis 並回傳。
     * 使用相同錯誤訊息回應，避免洩漏 email 是否存在。
     *
     * @param forgetPasswordDTO
     * @return ForgetPasswordVO
     * @throws InvalidCredentialsException email 不存在或 cellphone 不吻合。
     */
    public ForgetPasswordVO forgetPassword(ForgetPasswordDTO forgetPasswordDTO) {
        logger.debug("開始呼叫 API 來驗證忘記密碼請求，email: {}", forgetPasswordDTO.getEmail());

        User user = userDAO.getByEmail(forgetPasswordDTO.getEmail())
                .orElseThrow(() -> new ForgetPasswordVerificationException("電子郵件或手機號碼驗證失敗!"));

        if (!forgetPasswordDTO.getCellphone().equals(user.getCellphone())) {
            throw new ForgetPasswordVerificationException("電子郵件或手機號碼驗證失敗!");
        }

        String resetPasswordToken = generateResetPasswordToken();
        redisService.saveResetPasswordToken(resetPasswordToken, forgetPasswordDTO.getEmail(), 15, TimeUnit.MINUTES);
        logger.debug("重設密碼 token 已生成並存入 Redis，email: {}", forgetPasswordDTO.getEmail());

        return new ForgetPasswordVO(resetPasswordToken);
    }

    /**
     * 驗證重設密碼 token，通過後以新密碼更新資料庫並使 token 失效。
     *
     * @param token            重設密碼 token（來自 forgetPassword 回應）
     * @param resetPasswordDTO
     * @return UpdatePasswordVO
     * @throws InvalidCredentialsException token 無效或已過期。
     * @throws UserNotFoundException        更新時找不到對應使用者（已被併發刪除）。
     */
    @Transactional
    public UpdatePasswordVO resetPassword(ResetPasswordDTO resetPasswordDTO) {
        logger.debug("開始呼叫 API 來重設使用者密碼");

        String email = redisService.getEmailByResetPasswordToken(resetPasswordDTO.getResetPasswordToken());
        if (email == null) {
            throw new InvalidCredentialsException("重設密碼 token 無效或已過期!");
        }

        String newPassword = passwordEncoder.encode(resetPasswordDTO.getNewPassword());
        LocalDateTime updatedAt = LocalDateTime.now(ZoneOffset.UTC);
        int affectedRows = authDAO.updatePasswordByEmail(email, newPassword, updatedAt);
        if (affectedRows == 0) {
            throw new UserNotFoundException("找不到對應使用者，密碼重設失敗!");
        }
        redisService.deleteResetPasswordToken(resetPasswordDTO.getResetPasswordToken());

        logger.debug("密碼重設成功，email: {}", email);

        return new UpdatePasswordVO(updatedAt.atZone(ZoneOffset.UTC));
    }

    /**
     * 生成新的存取token與刷新token，並將刷新token JTI 存入 Redis。
     *
     * @param userId
     * @param email
     * @return AuthVO
     */
    private AuthVO generateTokens(Integer userId, String email) {
        String accessToken = jwtUtil.generateAccessToken(userId, UUID.randomUUID(), email, Set.of("ROLE_USER"));
        String refreshToken = jwtUtil.generateRefreshToken(email);

        String refreshTokenJti = jwtUtil.getJtiFromToken(refreshToken);
        redisService.saveRefreshTokenJti(
                userId.toString(),
                refreshTokenJti,
                jwtUtil.getRefreshTokenExpiration(),
                TimeUnit.MILLISECONDS);

        return new AuthVO(accessToken, refreshToken);
    }

    /**
     * 以 SecureRandom 生成 URL-safe Base64 編碼的重設密碼 token。
     *
     * @return resetPasswordToken
     */
    private String generateResetPasswordToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        return base64Encoder.encodeToString(randomBytes);
    }
}
