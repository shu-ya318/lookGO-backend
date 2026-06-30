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

import com.mli.lookgo.module.user.dao.UserDAO;
import com.mli.lookgo.module.user.enums.MembershipTier;
import com.mli.lookgo.module.user.enums.UserRole;
import com.mli.lookgo.module.user.enums.UserStatus;
import com.mli.lookgo.module.user.exceptions.AdminStatusModificationException;
import com.mli.lookgo.module.user.exceptions.UserNotFoundException;
import com.mli.lookgo.module.user.model.dto.UpdateBirthDateDTO;
import com.mli.lookgo.module.user.model.dto.UpdateCellphoneDTO;
import com.mli.lookgo.module.user.model.dto.UpdatePasswordDTO;
import com.mli.lookgo.module.user.model.dto.UpdateUsernameDTO;
import com.mli.lookgo.module.user.model.dto.UpdateUserStatusDTO;
import com.mli.lookgo.module.user.model.entity.User;
import com.mli.lookgo.module.user.model.vo.UserVO;
import com.mli.lookgo.core.exceptions.InvalidCredentialsException;
import com.mli.lookgo.core.result.MessageVO;
import com.mli.lookgo.core.result.PaginatedVO;
import com.mli.lookgo.core.service.RedisService;

import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 處理使用者資料查詢相關的業務邏輯。
 *
 * @author D5042101
 * @since 2026.06.06
 */
@Service
public class UserService {

    private final UserDAO userDAO;
    private final PasswordEncoder passwordEncoder;
    private final RedisService redisService;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入所需的依賴。
     *
     * @param userDAO
     * @param passwordEncoder
     * @param redisService
     */
    public UserService(UserDAO userDAO, PasswordEncoder passwordEncoder, RedisService redisService) {
        this.userDAO = userDAO;
        this.passwordEncoder = passwordEncoder;
        this.redisService = redisService;
    }

    /**
     * 從 Spring Security Context 中取得當前已驗證使用者的資訊。
     *
     * @return UserVO
     * @throws UserNotFoundException
     */
    public UserVO getCurrentUser() {
        String email = getAuthenticatedEmail();
        logger.debug("開始呼叫 API 來查詢當前使用者資料，email: {}", email);
        User user = userDAO.getByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("找不到當前使用者!"));

        return toVO(user);
    }

    /**
     * 取得分頁與模糊搜尋後的所有使用者資料。角色權限由 Controller 層的 @PreAuthorize 控制。
     *
     * @param keyword
     * @param page
     * @param size
     * @return PaginatedVO<UserVO>
     */
    public PaginatedVO<UserVO> getAllUser(String keyword, int page, int size) {
        logger.debug("開始呼叫 API 來分頁查詢所有使用者資料，keyword: {}, page: {}, size: {}", keyword, page, size);
        List<User> users = userDAO.getAllPaginated(keyword, page * size, size);
        long totalElements = userDAO.countAll(keyword);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        List<UserVO> userVOs = users.stream().map(this::toVO).toList();

        return new PaginatedVO<>(
                userVOs,
                page,
                size,
                totalElements,
                totalPages);
    }

    /**
     * 更新當前已驗證使用者的名稱。
     *
     * @param updateUsernameDTO
     * @return MessageVO
     * @throws UserNotFoundException 找不到對應使用者。
     */
    @Transactional
    public MessageVO updateUsername(UpdateUsernameDTO updateUsernameDTO) {
        String email = getAuthenticatedEmail();
        logger.debug("開始呼叫 API 來更新使用者名稱，email: {}", email);

        userDAO.getByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("找不到當前使用者!"));

        userDAO.updateUsernameByEmail(email, updateUsernameDTO.getUsername(), LocalDateTime.now(ZoneOffset.UTC));

        return new MessageVO("使用者名稱更新成功!");
    }

    /**
     * 驗證舊密碼後更新當前已驗證使用者的密碼。
     *
     * @param updatePasswordDTO
     * @return MessageVO
     * @throws InvalidCredentialsException 舊密碼錯誤。
     * @throws UserNotFoundException       找不到對應使用者。
     */
    @Transactional
    public MessageVO updatePassword(UpdatePasswordDTO updatePasswordDTO) {
        String email = getAuthenticatedEmail();
        logger.debug("開始呼叫 API 來更新使用者密碼，email: {}", email);

        User user = userDAO.getByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("找不到當前使用者!"));

        if (!passwordEncoder.matches(updatePasswordDTO.getOldPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("舊密碼錯誤!");
        }

        String hashedPassword = passwordEncoder.encode(updatePasswordDTO.getNewPassword());
        userDAO.updatePasswordByEmail(email, hashedPassword, LocalDateTime.now(ZoneOffset.UTC));

        return new MessageVO("密碼更新成功!");
    }

    /**
     * 更新當前已驗證使用者的出生日期。
     *
     * @param updateBirthDateDTO
     * @return MessageVO
     * @throws UserNotFoundException 找不到對應使用者。
     */
    @Transactional
    public MessageVO updateBirthDate(UpdateBirthDateDTO updateBirthDateDTO) {
        String email = getAuthenticatedEmail();
        logger.debug("開始呼叫 API 來更新使用者出生日期，email: {}", email);

        userDAO.getByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("找不到當前使用者!"));

        userDAO.updateBirthDateByEmail(email, updateBirthDateDTO.getBirthDate(), LocalDateTime.now(ZoneOffset.UTC));

        return new MessageVO("出生日期更新成功!");
    }

    /**
     * 更新當前已驗證使用者的電話號碼。
     *
     * @param updateCellphoneDTO
     * @return MessageVO
     * @throws UserNotFoundException 找不到對應使用者。
     */
    @Transactional
    public MessageVO updateCellphone(UpdateCellphoneDTO updateCellphoneDTO) {
        String email = getAuthenticatedEmail();
        logger.debug("開始呼叫 API 來更新使用者電話號碼，email: {}", email);

        userDAO.getByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("找不到當前使用者!"));

        userDAO.updateCellphoneByEmail(email, updateCellphoneDTO.getCellphone(), LocalDateTime.now(ZoneOffset.UTC));

        return new MessageVO("電話號碼更新成功!");
    }

    /**
     * 更新指定使用者的帳號狀態。若狀態被修改為 DISABLED，會強制將其 Redis 中的 refresh token 移除。
     *
     * @param updateUserStatusDTO
     * @return MessageVO
     * @throws UserNotFoundException              找不到對應使用者。
     * @throws AdminStatusModificationException   目標使用者為管理員，不允許變更其帳號狀態。
     */
    @Transactional
    public MessageVO updateStatus(UpdateUserStatusDTO updateUserStatusDTO) {
        logger.debug("開始呼叫 API 來更新使用者狀態，userId: {}, status: {}",
                updateUserStatusDTO.getUserId(), updateUserStatusDTO.getStatus());

        User user = userDAO.getById(updateUserStatusDTO.getUserId())
                .orElseThrow(() -> new UserNotFoundException("找不到指定使用者!"));

        if (UserRole.fromId(user.getRoleId()) == UserRole.ADMIN) {
            throw new AdminStatusModificationException("不得變更管理員帳號狀態!");
        }

        UserStatus targetStatus = updateUserStatusDTO.getStatus();

        if (user.getStatus().equals(targetStatus.getCode())) {
            return new MessageVO("使用者已經是指定的狀態，不需更新!");
        }

        userDAO.updateStatusById(user.getId(), targetStatus.getCode(), LocalDateTime.now(ZoneOffset.UTC));

        if (targetStatus == UserStatus.DISABLED) {
            redisService.deleteRefreshTokenJti(user.getId().toString());
            logger.debug("使用者 id: {} 已被禁用，移除其 refresh token", user.getId());
        }

        return new MessageVO("更新使用者狀態成功!");
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
                user.getCellphone(),
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
