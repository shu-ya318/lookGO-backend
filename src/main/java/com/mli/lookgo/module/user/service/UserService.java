package com.mli.lookgo.module.user.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mli.lookgo.common.result.ApiResult;
import com.mli.lookgo.module.user.dao.UserDao;
import com.mli.lookgo.module.user.enums.MembershipTier;
import com.mli.lookgo.module.user.enums.UserRole;
import com.mli.lookgo.module.user.enums.UserStatus;
import com.mli.lookgo.module.user.exceptions.UserNotFoundException;
import com.mli.lookgo.module.user.model.dto.UpdateBirthDateDTO;
import com.mli.lookgo.module.user.model.dto.UpdatePasswordDTO;
import com.mli.lookgo.module.user.model.dto.UpdateUsernameDTO;
import com.mli.lookgo.module.user.model.entity.User;
import com.mli.lookgo.module.user.model.vo.UserVO;
import com.mli.lookgo.module.auth.exceptions.InvalidCredentialsException;

import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 處理使用者資料查詢相關的業務邏輯。
 *
 * @author D5042101
 * @since 2026.06.06
 */
@Service
public class UserService {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入所需的依賴。
     *
     * @param userDao
     * @param passwordEncoder
     */
    public UserService(UserDao userDao, PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 從 Spring Security Context 中取得當前已驗證使用者的資訊。
     *
     * @return UserVO
     * @throws UserNotFoundException 找不到對應使用者。
     */
    public UserVO getCurrentUser() {
        String email = getAuthenticatedEmail();
        logger.info("開始呼叫 API 來查詢當前使用者資料，email: {}", email);
        User user = userDao.getByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("找不到當前使用者!"));

        return toVO(user);
    }

    /**
     * 取得所有使用者的資訊。角色權限由 Controller 層的 @PreAuthorize 控制。
     *
     * @return List<UserVO>
     */
    public List<UserVO> getAllUser() {
        logger.info("開始呼叫 API 來查詢所有使用者資料");
        List<User> users = userDao.getAll();

        return users.stream().map(this::toVO).toList();
    }

    /**
     * 更新當前已驗證使用者的名稱。
     *
     * @param updateUsernameDTO
     * @return ApiResult
     * @throws UserNotFoundException 找不到對應使用者。
     */
    @Transactional
    public ApiResult updateUsername(UpdateUsernameDTO updateUsernameDTO) {
        String email = getAuthenticatedEmail();
        logger.info("開始呼叫 API 來更新使用者名稱，email: {}", email);

        userDao.getByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("找不到當前使用者!"));

        userDao.updateUsernameByEmail(email, updateUsernameDTO.getUsername(), LocalDateTime.now(ZoneOffset.UTC));

        return new ApiResult("使用者名稱更新成功!");
    }

    /**
     * 驗證舊密碼後更新當前已驗證使用者的密碼。
     *
     * @param updatePasswordDTO
     * @return ApiResult
     * @throws InvalidCredentialsException 舊密碼錯誤。
     * @throws UserNotFoundException       找不到對應使用者。
     */
    @Transactional
    public ApiResult updatePassword(UpdatePasswordDTO updatePasswordDTO) {
        String email = getAuthenticatedEmail();
        logger.info("開始呼叫 API 來更新使用者密碼，email: {}", email);

        User user = userDao.getByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("找不到當前使用者!"));

        if (!passwordEncoder.matches(updatePasswordDTO.getOldPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("舊密碼錯誤!");
        }

        String hashedPassword = passwordEncoder.encode(updatePasswordDTO.getNewPassword());
        userDao.updatePasswordByEmail(email, hashedPassword, LocalDateTime.now(ZoneOffset.UTC));

        return new ApiResult("密碼更新成功!");
    }

    /**
     * 更新當前已驗證使用者的出生日期。
     *
     * @param updateBirthDateDTO
     * @return ApiResult
     * @throws UserNotFoundException 找不到對應使用者。
     */
    @Transactional
    public ApiResult updateBirthDate(UpdateBirthDateDTO updateBirthDateDTO) {
        String email = getAuthenticatedEmail();
        logger.info("開始呼叫 API 來更新使用者出生日期，email: {}", email);

        userDao.getByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("找不到當前使用者!"));

        userDao.updateBirthDateByEmail(email, updateBirthDateDTO.getBirthDate(), LocalDateTime.now(ZoneOffset.UTC));

        return new ApiResult("出生日期更新成功!");
    }

    /**
     * 從 Spring Security Context 中取得當前已驗證使用者的 email。
     *
     * @return email
     */
    private String getAuthenticatedEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /**
     * 定義一個私有輔助方法，把傳入的 entity 轉換為 VO。
     * 資料庫檢存 UTC 的 LocalDateTime，轉換時明確附加 UTC 時區。
     *
     * @param user
     * @return UserVO
     */
    private UserVO toVO(User user) {
        return new UserVO(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                MembershipTier.fromId(user.getMembershipTierId()),
                UserRole.fromId(user.getRoleId()),
                user.getBirthDate(),
                UserStatus.fromCode(user.getStatus()),
                toUTC(user.getCreatedAt()),
                toUTC(user.getUpdatedAt()),
                toUTC(user.getLastLoginAt()));
    }

    /**
     * 將資料庫取出的 LocalDateTime（視為 UTC）轉換為含時區的 ZonedDateTime(UTC)。
     *
     * @param ldt 資料庫取出的 LocalDateTime，可能為 null
     * @return ZonedDateTime(UTC)，若輸入為 null 則回傳 null
     */
    private ZonedDateTime toUTC(LocalDateTime ldt) {
        return ldt == null ? null : ldt.atZone(ZoneOffset.UTC);
    }
}
