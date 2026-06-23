package com.mli.lookgo.module.auth.service;

// import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
// import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// import com.sendgrid.Method;
// import com.sendgrid.Request;
// import com.sendgrid.SendGrid;
// import com.sendgrid.helpers.mail.Mail;
// import com.sendgrid.helpers.mail.objects.Content;
// import com.sendgrid.helpers.mail.objects.Email;

// import com.sendgrid.Response;

import com.mli.lookgo.common.result.ApiResult;
import com.mli.lookgo.module.auth.dao.AuthDao;
import com.mli.lookgo.module.user.dao.UserDao;
import com.mli.lookgo.module.user.enums.MembershipTier;
import com.mli.lookgo.module.user.enums.UserRole;
import com.mli.lookgo.module.user.enums.UserStatus;
// import com.mli.lookgo.module.auth.exceptions.EmailDeliveryException;
import com.mli.lookgo.module.auth.exceptions.InvalidCredentialsException;
import com.mli.lookgo.module.auth.exceptions.UserDuplicateException;
import com.mli.lookgo.module.auth.model.dto.ForgetPasswordDTO;
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
    // private final SendGrid sendGrid;
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    // private static final SecureRandom secureRandom = new SecureRandom();
    // private static final Base64.Encoder base64Encoder =
    // Base64.getUrlEncoder().withoutPadding();

    @Value("${frontend.base-url}")
    private String frontendBaseUrl;

    @Value("${app.sendgrid.api.key}")
    private String apiKey;

    @Value("${app.sendgrid.from.email}")
    private String fromEmail;

    @Value("${app.sendgrid.from.name}")
    private String fromName;

    @Value("${app.sendgrid.sandbox.mode}")
    private boolean sandboxMode;

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入所需的依賴。
     *
     * @param redisService
     * @param authDao
     * @param passwordEncoder
     * @param jwtUtil
     */
    // , JavaMailSender sendGrid (SMTP 連線) 或 , SendGrid sendGrid (HTTP 連線)
    public AuthService(RedisService redisService, AuthDao authDao, UserDao userDao, PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil) {
        this.redisService = redisService;
        this.authDao = authDao;
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 把傳入資訊寫入資料庫，建立一筆對應的使用者帳號，並回傳存取token。
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

        logger.debug("開始呼叫 API 來建立使用者帳號，寫入資料: {}, 建立時間: {}", user, currentTime);
        authDao.createUser(user);

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
     * 驗證刷新token，通過後核發新的存取token與刷新token。
     *
     * @param refreshToken
     * @return AuthVO
     * @throws InvalidCredentialsException
     */
    public AuthVO refreshTokens(String refreshToken) {
        String email = jwtUtil.getEmailFromRefreshToken(refreshToken);
        logger.debug("開始呼叫 API 來刷新token，email: {}", email);
        User user = userDao.getByEmail(email)
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
     * 驗證電子郵件，通過後發送請求重設密碼到電子郵件並返回成功請求訊息。
     *
     * @param forgetPasswordDTO
     * @return ResponseEntity<ApiResult>
     */
    public ApiResult forgetPassword(ForgetPasswordDTO forgetPasswordDTO) {
        logger.debug("開始呼叫 API 來驗證使用者身分，輸入內容: {}", forgetPasswordDTO);
        Optional<User> user = userDao.getByEmail(forgetPasswordDTO.getEmail());

        if (user.isEmpty()) {
            logger.warn("請求重設密碼失敗，不存在此 Email!: {}", forgetPasswordDTO.getEmail());
            return new ApiResult("重設密碼請求成功!");
        }

        // String resetPasswordToken =
        // generateResetPasswordToken(forgetPasswordDTO.getEmail());
        // logger.debug("重設密碼 token 建立成功: {}", resetPasswordToken);

        // redisService.saveResetPasswordToken(
        // resetPasswordToken,
        // forgetPasswordDTO.getEmail(),
        // 15,
        // java.util.concurrent.TimeUnit.MINUTES);
        // logger.debug("重設密碼 token 已存入 Redis");

        // sendResetPasswordEmail(forgetPasswordDTO.getEmail(),
        // frontendBaseUrl + "/auth/reset-password?token=" + resetPasswordToken);
        // logger.debug("發送重設密碼信件完成");

        return new ApiResult("重設密碼請求成功!");
    }

    /**
     * 生成新的存取token與刷新token，並將刷新token JTI 存入 Redis。
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
     * 生成重設密碼的token。
     * 
     * @param email
     * @return resetPasswordToken
     */
    // private String generateResetPasswordToken(String email) {
    // byte[] randomBytes = new byte[32];
    // secureRandom.nextBytes(randomBytes);

    // return base64Encoder.encodeToString(randomBytes);
    // }

    /**
     * 發送重設密碼的電子郵件。
     * 
     * @param toEmail
     * @param resetPasswordUrl
     * @param resetPasswordToken
     */
    // private void sendResetPasswordEmail(String toEmail, String resetPasswordUrl)
    // {
    // logger.debug("開始呼叫 API 來發送重設密碼的電子郵件，email: {}", toEmail);

    // // 組裝完整 mail 內容
    // Email from = new Email(fromEmail, fromName);
    // String subject = "重設密碼通知";
    // Email to = new Email(toEmail);
    // Content content = new Content("text/plain", "請點擊以下連結來重設密碼:\n" +
    // resetPasswordUrl);
    // Mail mail = new Mail(from, subject, to, content);

    // // 組裝完整 request 內容
    // Request request = new Request();

    // try {
    // request.setMethod(Method.POST);
    // request.setEndpoint("mail/send");
    // request.setBody(mail.build());

    // Response response = sendGrid.api(request);

    // if (response.getStatusCode() != 202) {
    // throw new EmailDeliveryException("SendGrid 回應錯誤，狀態碼: " +
    // response.getStatusCode());
    // }
    // // mail.build() 和 sendGrid.api(request) 要求處理具體的 java.io.IOException
    // } catch (java.io.IOException exception) {
    // logger.error("發送密碼重設信件錯誤: ", exception);
    // throw new EmailDeliveryException("發送密碼重設信件發生網路連線失敗!");
    // }
    // }

    /**
     * 驗證重設密碼token，通過後更新使用者密碼並使token失效。
     *
     * @param resetPasswordDTO
     * @return ApiResult
     * @throws InvalidCredentialsException
     */
    @Transactional
    public ApiResult resetPassword(ResetPasswordDTO resetPasswordDTO) {
        String resetPasswordToken = resetPasswordDTO.getResetPasswordToken();
        logger.debug("開始呼叫 API 來重設使用者密碼");

        String email = redisService.getEmailByResetPasswordToken(resetPasswordToken);
        // 不拋出具體的使用者不存在錯誤，避免猜到使用者是否存在遭到攻擊
        if (email == null) {
            throw new InvalidCredentialsException("重設密碼token無效或已過期!");
        }

        String newPassword = passwordEncoder.encode(resetPasswordDTO.getNewPassword());
        authDao.updatePasswordByEmail(email, newPassword, LocalDateTime.now(ZoneOffset.UTC));

        redisService.deleteResetPasswordToken(resetPasswordToken);

        return new ApiResult("密碼重設成功!");
    }
}
