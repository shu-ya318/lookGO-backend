package com.mli.lookgo.module.auth.service;

// import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
// import java.util.Base64;
// import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// import org.springframework.mail.SimpleMailMessage;
import org.springframework.beans.factory.annotation.Value;
// import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mli.lookgo.common.result.ApiResult;
import com.mli.lookgo.module.auth.dao.AuthDao;
import com.mli.lookgo.module.user.dao.UserDao;
import com.mli.lookgo.module.user.enums.MembershipTier;
import com.mli.lookgo.module.user.enums.UserRole;
import com.mli.lookgo.module.user.enums.UserStatus;
import com.mli.lookgo.module.auth.exceptions.InvalidCredentialsException;
import com.mli.lookgo.module.auth.exceptions.UserDuplicateException;
// import com.mli.lookgo.module.auth.model.dto.ForgetPasswordDTO;
import com.mli.lookgo.module.auth.model.dto.LoginDTO;
import com.mli.lookgo.module.auth.model.dto.ResetPasswordDTO;
import com.mli.lookgo.module.auth.model.dto.SignupDTO;
import com.mli.lookgo.module.user.model.entity.User;
import com.mli.lookgo.module.auth.model.vo.AuthVO;
import com.mli.lookgo.module.auth.security.JwtUtil;

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
    private final AuthDao authDao;
    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    // private final JavaMailSender javaMailSender;
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    // private static final SecureRandom secureRandom = new SecureRandom();
    // private static final Base64.Encoder base64Encoder =
    // Base64.getUrlEncoder().withoutPadding();

    @Value("${frontend.base-url}")
    private String frontendBaseUrl;

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入所需的依賴。
     *
     * @param redisService
     * @param authDao
     * @param passwordEncoder
     * @param jwtUtil
     */
    // , JavaMailSender javaMailSender
    public AuthService(RedisService redisService, AuthDao authDao, UserDao userDao, PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil) {
        this.redisService = redisService;
        this.authDao = authDao;
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        // this.javaMailSender = javaMailSender;
    }

    /**
     * 把傳入資訊寫入資料庫，建立一筆對應的使用者帳號，並回傳存取憑證。
     *
     * @param signupDTO
     * @param httpServletResponse
     * @return AuthVO
     * @throws UserDuplicateException
     */
    @Transactional
    public AuthVO signup(SignupDTO signupDTO, HttpServletResponse httpServletResponse) {
        if (authDao.existsByEmail(signupDTO.getEmail())) {
            throw new UserDuplicateException("Email: " + signupDTO.getEmail() + " 已被使用，請換一個 email!");
        }

        String hashedPassword = passwordEncoder.encode(signupDTO.getPassword());
        LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);

        User user = new User();

        user.setMembershipTierId(MembershipTier.BASIC.getId());
        user.setRoleId(UserRole.USER.getId());
        user.setEmail(signupDTO.getEmail());
        user.setPassword(hashedPassword);
        user.setUsername(signupDTO.getUsername());
        user.setBirthDate(signupDTO.getBirthDate());
        user.setStatus(UserStatus.ACTIVE.getCode());
        user.setCreatedAt(currentTime);
        user.setUpdatedAt(currentTime);
        user.setLastLoginAt(currentTime);

        logger.info("開始呼叫 API 來建立使用者帳號，寫入資料: {}, 建立時間: {}", user, currentTime);
        authDao.createUser(user);

        Integer userId = user.getId();

        return generateTokens(userId, user.getEmail());
    }

    /**
     * 驗證使用者帳號與密碼，通過後回傳存取憑證。
     *
     * @param loginDTO
     * @param httpServletResponse
     * @return AuthVO
     * @throws InvalidCredentialsException
     */
    public AuthVO login(LoginDTO loginDTO, HttpServletResponse httpServletResponse) {
        logger.info("開始呼叫 API 來驗證使用者身分，輸入內容: {}", loginDTO);
        User user = userDao.getByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("帳號或密碼錯誤!"));

        if (user.getStatus() != UserStatus.ACTIVE.getCode()) {
            throw new InvalidCredentialsException("該帳號已被停用!");
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("帳號或密碼錯誤!");
        }

        return generateTokens(user.getId(), user.getEmail());
    }

    /**
     * 驗證刷新憑證，通過後核發新的存取憑證與刷新憑證。
     *
     * @param refreshToken
     * @return AuthVO
     * @throws InvalidCredentialsException
     */
    public AuthVO refreshTokens(String refreshToken) {
        String email = jwtUtil.getEmailFromRefreshToken(refreshToken);
        logger.info("開始呼叫 API 來刷新憑證，email: {}", email);
        User user = userDao.getByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("刷新憑證無效或已過期!"));

        if (user.getStatus() != UserStatus.ACTIVE.getCode()) {
            throw new InvalidCredentialsException("該帳號已被停用!");
        }

        String storedRefreshTokenJti = redisService.getRefreshTokenJti(user.getId().toString());
        String refreshTokenJti = jwtUtil.getJtiFromToken(refreshToken);
        if (storedRefreshTokenJti == null || !storedRefreshTokenJti.equals(refreshTokenJti)) {
            throw new InvalidCredentialsException("刷新憑證無效或已過期!");
        }

        return generateTokens(user.getId(), user.getEmail());
    }

    /**
     * 驗證電子郵件，通過後發送請求重設密碼到電子郵件並返回成功請求訊息。
     *
     * @param forgetPasswordDTO
     * @return ResponseEntity<ApiResult>
     */
    // public ApiResult forgetPassword(ForgetPasswordDTO forgetPasswordDTO) {
    // logger.info("開始呼叫 API 來驗證使用者身分，輸入內容: {}", forgetPasswordDTO);
    // Optional<User> user = userDao.getByEmail(forgetPasswordDTO.getEmail());
    //
    // if (user.isEmpty()) {
    // logger.warn("請求重設密碼失敗，不存在此 Email!: {}", forgetPasswordDTO.getEmail());
    // return new ApiResult("重設密碼請求成功!");
    // }
    //
    // String resetPasswordToken =
    // generateResetPasswordToken(forgetPasswordDTO.getEmail());
    // logger.info(resetPasswordToken);
    // redisService.saveResetPasswordToken(
    // resetPasswordToken,
    // forgetPasswordDTO.getEmail(),
    // 15,
    // java.util.concurrent.TimeUnit.MINUTES);
    // logger.info("存入redis");
    // sendEmail(forgetPasswordDTO.getEmail(), resetPasswordToken);
    // logger.info("發送信件完成");
    // return new ApiResult("重設密碼請求成功!");
    // }

    /**
     * 驗證重設密碼憑證，通過後更新使用者密碼並使憑證失效。
     *
     * @param resetPasswordDTO
     * @return ApiResult
     * @throws InvalidCredentialsException
     */
    @Transactional
    public ApiResult resetPassword(ResetPasswordDTO resetPasswordDTO) {
        String resetPasswordToken = resetPasswordDTO.getResetPasswordToken();
        logger.info("開始呼叫 API 來重設使用者密碼");

        String email = redisService.getEmailByResetPasswordToken(resetPasswordToken);
        // 不拋出具體的使用者不存在錯誤，避免猜到使用者是否存在遭到攻擊
        if (email == null) {
            throw new InvalidCredentialsException("重設密碼憑證無效或已過期!");
        }

        String newPassword = passwordEncoder.encode(resetPasswordDTO.getNewPassword());
        authDao.updatePasswordByEmail(email, newPassword, LocalDateTime.now(ZoneOffset.UTC));

        redisService.deleteResetPasswordToken(resetPasswordToken);

        return new ApiResult("密碼重設成功!");
    }

    /**
     * 生成新的存取憑證與刷新憑證，並將刷新憑證 JTI 存入 Redis。
     * 
     * @param userId
     * @param email
     * @return
     */
    private AuthVO generateTokens(Integer userId, String email) {
        String accessToken = jwtUtil.generateAccessToken(userId, UUID.randomUUID(), email, Set.of("ROLE_USER"));

        String refreshToken = jwtUtil.generateRefreshToken(email);

        String refreshTokenJti = jwtUtil.getJtiFromToken(refreshToken);
        redisService.saveRefreshTokenJti(
                userId.toString(),
                refreshTokenJti,
                jwtUtil.getRefreshTokenExpiration(),
                java.util.concurrent.TimeUnit.MILLISECONDS);

        return new AuthVO(accessToken, refreshToken);
    }

    /**
     * 生成重設密碼的憑證。
     * 
     * @param email
     * @return resetPasswordToken
     */
    // private String generateResetPasswordToken(String email) {
    // byte[] randomBytes = new byte[32];
    // secureRandom.nextBytes(randomBytes);
    //
    // return base64Encoder.encodeToString(randomBytes);
    // }

    /**
     * 發送重設密碼的電子郵件。
     * 
     * @param email
     * @param resetPasswordToken
     */
    // private void sendEmail(String email, String resetPasswordToken) {
    // logger.info("開始呼叫 API 來發送重設密碼的電子郵件，email: {}", email);
    //
    // String resetPasswordUrl = frontendBaseUrl + "/auth/reset-password?token=" +
    // resetPasswordToken;
    // String body = "請點擊以下連結以重設密碼:\n" + resetPasswordUrl;
    //
    // SimpleMailMessage message = new SimpleMailMessage();
    //
    // message.setTo(email);
    // message.setSubject("重設密碼");
    // message.setText(body);
    //
    // javaMailSender.send(message);
    // }
}
