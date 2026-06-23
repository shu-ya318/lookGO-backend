package com.mli.lookgo.module.user.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;

import com.mli.lookgo.common.result.ApiResult;
import com.mli.lookgo.module.user.model.dto.UpdateBirthDateDTO;
import com.mli.lookgo.module.user.model.dto.UpdatePasswordDTO;
import com.mli.lookgo.module.user.model.dto.UpdateUsernameDTO;
import com.mli.lookgo.module.user.model.vo.UserVO;
import com.mli.lookgo.module.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

/**
 * 處理使用者相關的 HTTP 請求的介面層。負責驗證請求參數，把資料傳給業務層處理，最後封裝結果為 HTTP 回應回傳給客戶端。
 *
 * @author D5042101
 * @since 2026.06.06
 */
@RestController
@RequestMapping("/api/v1/user")
@Tag(name = "User", description = "處理使用者資料相關操作的 API")
public class UserController {

    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入使用者相關的業務層 {@link UserService}。
     *
     * @param userService
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 取得當前已驗證使用者的資訊。
     *
     * @return ResponseEntity<UserVO>
     */
    @Operation(summary = "取得當前使用者資訊", description = "依據存取token取得當前已驗證使用者的資訊")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功取得當前使用者的資訊", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserVO.class))),
            @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
            @ApiResponse(responseCode = "404", description = "找不到當前使用者", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "找不到當前使用者!"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PostMapping("/get-current-user")
    public ResponseEntity<UserVO> getCurrentUser() {
        logger.debug("收到查詢當前使用者資訊的請求");
        UserVO userVO = userService.getCurrentUser();

        return ResponseEntity.ok(userVO);
    }

    /**
     * 取得所有使用者的資訊，僅限 ADMIN 角色存取。
     *
     * @return ResponseEntity<List<UserVO>>
     */
    @Operation(summary = "取得所有使用者資訊", description = "取得所有使用者的資訊，僅限 ADMIN 角色存取")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功取得所有使用者的資訊", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserVO.class))),
            @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
            @ApiResponse(responseCode = "403", description = "權限不足", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "權限不足，無法操作!"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/get-all-user")
    public ResponseEntity<List<UserVO>> getAllUser() {
        logger.debug("收到查詢所有使用者資訊的請求");
        List<UserVO> userVOs = userService.getAllUser();

        return ResponseEntity.ok(userVOs);
    }

    /**
     * 更新當前已驗證使用者的名稱。
     *
     * @param updateUsernameDTO
     * @return ResponseEntity<ApiResult>
     */
    @Operation(summary = "更新使用者名稱", description = "更新當前已驗證使用者的名稱")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "使用者名稱更新成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "400", description = "請求參數錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "請輸入使用者名稱!"))),
            @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
            @ApiResponse(responseCode = "404", description = "找不到當前使用者", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "找不到當前使用者!"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PostMapping("/update-username")
    public ResponseEntity<ApiResult> updateUsername(@Valid @RequestBody UpdateUsernameDTO updateUsernameDTO) {
        logger.debug("收到更新使用者名稱的請求");
        ApiResult apiResult = userService.updateUsername(updateUsernameDTO);

        return ResponseEntity.ok(apiResult);
    }

    /**
     * 驗證舊密碼後更新當前已驗證使用者的密碼。
     *
     * @param updatePasswordDTO
     * @return ResponseEntity<ApiResult>
     */
    @Operation(summary = "更新使用者密碼", description = "驗證舊密碼後更新當前已驗證使用者的密碼")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "密碼更新成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "400", description = "請求參數錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "密碼長度必須為 8-20 個字元!"))),
            @ApiResponse(responseCode = "401", description = "存取token無效或已過期，或舊密碼錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "舊密碼錯誤!"))),
            @ApiResponse(responseCode = "404", description = "找不到當前使用者", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "找不到當前使用者!"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PostMapping("/update-password")
    public ResponseEntity<ApiResult> updatePassword(@Valid @RequestBody UpdatePasswordDTO updatePasswordDTO) {
        logger.debug("收到更新使用者密碼的請求");
        ApiResult apiResult = userService.updatePassword(updatePasswordDTO);

        return ResponseEntity.ok(apiResult);
    }

    /**
     * 更新當前已驗證使用者的出生日期。
     *
     * @param updateBirthDateDTO
     * @return ResponseEntity<ApiResult>
     */
    @Operation(summary = "更新出生日期", description = "更新當前已驗證使用者的出生日期")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "出生日期更新成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "400", description = "請求參數錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "出生日期不得大於今日!"))),
            @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
            @ApiResponse(responseCode = "404", description = "找不到當前使用者", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "找不到當前使用者!"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PostMapping("/update-birth-date")
    public ResponseEntity<ApiResult> updateBirthDate(@Valid @RequestBody UpdateBirthDateDTO updateBirthDateDTO) {
        logger.debug("收到更新出生日期的請求");
        ApiResult apiResult = userService.updateBirthDate(updateBirthDateDTO);

        return ResponseEntity.ok(apiResult);
    }
}
