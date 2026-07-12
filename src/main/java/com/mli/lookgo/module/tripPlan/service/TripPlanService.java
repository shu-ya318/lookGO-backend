package com.mli.lookgo.module.tripPlan.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.mli.lookgo.core.result.MessageVO;
import com.mli.lookgo.core.result.PaginatedVO;
import com.mli.lookgo.module.metro.exceptions.StationNotFoundException;
import com.mli.lookgo.module.metro.service.MetroService;
import com.mli.lookgo.module.tripPlan.dao.TripPlanDAO;
import com.mli.lookgo.module.tripPlan.exceptions.TripPlanAccessDeniedException;
import com.mli.lookgo.module.tripPlan.exceptions.TripPlanExportExcelFailedException;
import com.mli.lookgo.module.tripPlan.exceptions.TripPlanLimitExceededException;
import com.mli.lookgo.module.tripPlan.exceptions.TripPlanNameDuplicationException;
import com.mli.lookgo.module.tripPlan.exceptions.TripPlanNotFoundException;
import com.mli.lookgo.module.tripPlan.model.dto.CreateTripPlanDTO;
import com.mli.lookgo.module.tripPlan.model.dto.UpdateTripPlanDTO;
import com.mli.lookgo.module.tripPlan.model.entity.UserTripPlan;
import com.mli.lookgo.module.tripPlan.model.vo.TripPlanVO;
import com.mli.lookgo.module.user.dao.UserDAO;
import com.mli.lookgo.module.user.exceptions.UserNotFoundException;
import com.mli.lookgo.module.user.model.entity.User;

/**
 * 處理使用者旅程規劃管理相關業務邏輯。
 *
 * @author D5042101
 * @since 2026.07.06
 */
@Service
public class TripPlanService {

    private final TripPlanDAO tripPlanDAO;
    private final MetroService metroService;
    private final UserDAO userDAO;


    private static final Logger logger = LoggerFactory.getLogger(TripPlanService.class);

    private static final DateTimeFormatter EXCEL_DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 有效票種代碼，比照 {@code MetroService.VALID_FARE_TYPES}：1=全票, 4=學生, 5=兒童, 7=愛心。 */
    private static final Set<Integer> VALID_FARE_TYPES = Set.of(1, 4, 5, 7);
    /**
     * 有效路線規劃策略代碼。
     * 比照 {@code MetroService.VALID_ROUTING_STRATEGIES}：1=最少轉乘次數, 2=最短車程時間。
     */
    private static final Set<Integer> VALID_ROUTING_STRATEGIES = Set.of(1, 2);

    private static final String FARE_TYPE_ROUTING_STRATEGY_HINT = "，有效票種為 1(全票)、4(學生)、5(兒童)、7(愛心)，"
            + "有效路線策略為 1(最少轉乘次數)、2(最短車程時間)";

    /** 有效的排序方向，白名單驗證以避免任意字串進入 SQL。 */
    private static final Set<String> VALID_SORT_DIRECTIONS = Set.of("ASC", "DESC");

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入所需的依賴。
     *
     * @param tripPlanDAO
     * @param metroService
     * @param userDAO
     */
    public TripPlanService(TripPlanDAO tripPlanDAO, MetroService metroService, UserDAO userDAO) {
        this.tripPlanDAO = tripPlanDAO;
        this.metroService = metroService;
        this.userDAO = userDAO;
    }

    /**
     * 新增一筆旅程規劃。
     *
     * @param createTripPlanDTO
     * @return TripPlanVO
     * @throws UserNotFoundException            找不到當前使用者。
     * @throws StationNotFoundException         找不到指定起站或訖站。
     * @throws IllegalArgumentException         票種或路線規劃策略代碼不合法。
     * @throws TripPlanNameDuplicationException 已有同名的有效旅程規劃。
     * @throws TripPlanLimitExceededException   已達會員等級旅程規劃數量上限。
     */
    public TripPlanVO createTripPlan(CreateTripPlanDTO createTripPlanDTO) {
        String email = getAuthenticatedEmail();
        logger.debug("開始呼叫 API 來新增旅程規劃，email: {}, createTripPlanDTO: {}", email, createTripPlanDTO);

        User user = userDAO.getByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("找不到當前使用者!"));

        if (!metroService.existsStationById(createTripPlanDTO.getFromStationId())) {
            throw new StationNotFoundException("找不到 id:" + createTripPlanDTO.getFromStationId() + " 的車站!");
        }
        if (!metroService.existsStationById(createTripPlanDTO.getToStationId())) {
            throw new StationNotFoundException("找不到 id:" + createTripPlanDTO.getToStationId() + " 的車站!");
        }

        validateFareTypeAndRoutingStrategy(createTripPlanDTO.getFareType(), createTripPlanDTO.getRoutingStrategy());

        // 名稱一律先 trim 再比對與寫入；SQL Server 預設 collation 不分大小寫
        String trimmedName = createTripPlanDTO.getName().trim();
        if (tripPlanDAO.existsActiveByUserIdAndName(user.getId(), trimmedName, null)) {
            throw new TripPlanNameDuplicationException("已有名稱為「" + trimmedName + "」的旅程規劃，請改用其他名稱!");
        }

        int maxTripPlans = tripPlanDAO.getMaxTripPlansByUserId(user.getId());
        int activeTripPlanCount = tripPlanDAO.countActiveByUserId(user.getId());

        if (activeTripPlanCount >= maxTripPlans) {
            throw new TripPlanLimitExceededException("已達會員等級旅程規劃數量上限 (" + maxTripPlans + " 筆)，請先刪除部分旅程規劃!");
        }

        LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);

        UserTripPlan tripPlan = new UserTripPlan();
        tripPlan.setUserId(user.getId());
        tripPlan.setFromStationId(createTripPlanDTO.getFromStationId());
        tripPlan.setToStationId(createTripPlanDTO.getToStationId());
        tripPlan.setName(trimmedName);
        tripPlan.setFareType(createTripPlanDTO.getFareType());
        tripPlan.setFarePrice(createTripPlanDTO.getFarePrice());
        tripPlan.setTransferCount(createTripPlanDTO.getTransferCount());
        tripPlan.setRoutingStrategy(createTripPlanDTO.getRoutingStrategy());
        tripPlan.setNotes(createTripPlanDTO.getNotes());
        tripPlan.setCreatedAt(currentTime);
        tripPlan.setUpdatedAt(currentTime);

        tripPlanDAO.insert(tripPlan);

        TripPlanVO createdTripPlan = tripPlanDAO.getById(tripPlan.getId())
                .orElseThrow(() -> new TripPlanNotFoundException("找不到剛新增的 id:" + tripPlan.getId() + " 旅程規劃!"));
        enrichTravelTime(createdTripPlan);

        return createdTripPlan;
    }

    /**
     * 取得目前使用者分頁與模糊搜尋（旅程名稱）後的旅程規劃列表，依更新時間排序。
     *
     * @param keyword
     * @param page
     * @param size
     * @param sortDirection 排序方向，DESC=新到舊（預設）、ASC=舊到新
     * @return PaginatedVO<TripPlanVO>
     * @throws IllegalArgumentException 排序方向不合法。
     * @throws UserNotFoundException    找不到當前使用者。
     * @throws StationNotFoundException 找不到旅程規劃起站或訖站對應的路線車站代碼。
     */
    public PaginatedVO<TripPlanVO> getAllTripPlan(String keyword, int page, int size, String sortDirection) {
        String email = getAuthenticatedEmail();
        logger.debug("開始分頁查詢旅程規劃資料，email: {}, keyword: {}, page: {}, size: {}, sortDirection: {}", email, keyword,
                page, size, sortDirection);

        String normalizedDirection = normalizeSortDirection(sortDirection);

        User user = userDAO.getByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("找不到當前使用者!"));

        List<TripPlanVO> tripPlans = tripPlanDAO.getAllPaginatedByUserId(user.getId(), keyword, page * size, size,
                normalizedDirection);
        tripPlans.forEach(this::enrichTravelTime);

        long totalElements = tripPlanDAO.countAllByUserId(user.getId(), keyword);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return new PaginatedVO<>(tripPlans, page, size, totalElements, totalPages);
    }

    /**
     * 取得當前使用者所有有效旅程規劃的名稱列表，依建立時間新到舊排序。
     *
     * @return List<String>
     * @throws UserNotFoundException 找不到當前使用者。
     */
    public List<String> getAllTripPlanName() {
        String email = getAuthenticatedEmail();
        logger.debug("開始查詢所有旅程規劃名稱，email: {}", email);

        User user = userDAO.getByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("找不到當前使用者!"));

        return tripPlanDAO.getAllNamesByUserId(user.getId());
    }

    /**
     * 用旅程名稱模糊搜尋，取得當前使用者符合條件中最新建立的一筆旅程規劃。
     *
     * @param keyword
     * @return TripPlanVO
     * @throws IllegalArgumentException  未輸入旅程名稱關鍵字。
     * @throws UserNotFoundException     找不到當前使用者。
     * @throws TripPlanNotFoundException 找不到名稱符合關鍵字的旅程規劃。
     * @throws StationNotFoundException  找不到旅程規劃起站或訖站對應的路線車站代碼。
     */
    public TripPlanVO getTripPlanByName(String keyword) {
        String email = getAuthenticatedEmail();
        logger.debug("開始以旅程名稱模糊搜尋單一旅程規劃，email: {}, keyword: {}", email, keyword);

        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("請輸入旅程名稱!");
        }

        User user = userDAO.getByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("找不到當前使用者!"));

        TripPlanVO tripPlan = tripPlanDAO.getLatestByUserIdAndKeyword(user.getId(), keyword)
                .orElseThrow(() -> new TripPlanNotFoundException("找不到名稱包含「" + keyword + "」的旅程規劃!"));
        enrichTravelTime(tripPlan);

        return tripPlan;
    }

    /**
     * 移除（軟刪除）指定的旅程規劃。
     *
     * @param tripPlanId
     * @return MessageVO
     * @throws UserNotFoundException         找不到當前使用者。
     * @throws TripPlanNotFoundException     找不到指定旅程規劃，或該旅程規劃已被軟刪除。
     * @throws TripPlanAccessDeniedException 嘗試操作非本人擁有的旅程規劃。
     */
    public MessageVO deleteTripPlan(Integer tripPlanId) {
        logger.debug("開始呼叫 API 來刪除旅程規劃，tripPlanId: {}", tripPlanId);

        Integer userId = getCurrentUserIdAndCheckOwnership(tripPlanId);
        logger.debug("使用者 id: {} 通過旅程規劃擁有權驗證", userId);

        tripPlanDAO.softDeleteById(tripPlanId, LocalDateTime.now(ZoneOffset.UTC));

        return new MessageVO("旅程規劃刪除成功!");
    }

    /**
     * 更新指定旅程規劃的名稱。
     *
     * @param tripPlanId
     * @param name
     * @return TripPlanVO
     * @throws UserNotFoundException            找不到當前使用者。
     * @throws TripPlanNotFoundException        找不到指定旅程規劃，或該旅程規劃已被軟刪除。
     * @throws TripPlanAccessDeniedException    嘗試操作非本人擁有的旅程規劃。
     * @throws TripPlanNameDuplicationException 已有同名的有效旅程規劃（排除自身，存回原名視為合法）。
     * @throws StationNotFoundException         找不到旅程規劃起站或訖站對應的路線車站代碼。
     */
    public TripPlanVO updateTripPlanName(Integer tripPlanId, String name) {
        logger.debug("開始呼叫 API 來更新旅程規劃名稱，tripPlanId: {}, name: {}", tripPlanId, name);

        Integer userId = getCurrentUserIdAndCheckOwnership(tripPlanId);

        // 名稱一律先 trim 再比對與寫入；排除自身，讓「存回原名」視為合法（等同未變更）
        String trimmedName = name.trim();
        if (tripPlanDAO.existsActiveByUserIdAndName(userId, trimmedName, tripPlanId)) {
            throw new TripPlanNameDuplicationException("已有名稱為「" + trimmedName + "」的旅程規劃，請改用其他名稱!");
        }

        tripPlanDAO.updateNameById(tripPlanId, trimmedName, LocalDateTime.now(ZoneOffset.UTC));

        TripPlanVO updatedTripPlan = tripPlanDAO.getById(tripPlanId)
                .orElseThrow(() -> new TripPlanNotFoundException("找不到 id:" + tripPlanId + " 的旅程規劃!"));
        enrichTravelTime(updatedTripPlan);

        return updatedTripPlan;
    }

    /**
     * 更新指定旅程規劃的資訊（起訖站以外）。
     *
     * @param updateTripPlanDTO
     * @return TripPlanVO
     * @throws UserNotFoundException         找不到當前使用者。
     * @throws TripPlanNotFoundException     找不到指定旅程規劃，或該旅程規劃已被軟刪除。
     * @throws TripPlanAccessDeniedException 嘗試操作非本人擁有的旅程規劃。
     * @throws IllegalArgumentException      票種或路線規劃策略代碼不合法。
     * @throws StationNotFoundException      找不到旅程規劃起站或訖站對應的路線車站代碼。
     */
    public TripPlanVO updateTripPlanInfo(UpdateTripPlanDTO updateTripPlanDTO) {
        logger.debug("開始呼叫 API 來更新旅程規劃資訊，updateTripPlanDTO: {}", updateTripPlanDTO);

        Integer tripPlanId = updateTripPlanDTO.getTripPlanId();
        getCurrentUserIdAndCheckOwnership(tripPlanId);

        validateFareTypeAndRoutingStrategy(updateTripPlanDTO.getFareType(), updateTripPlanDTO.getRoutingStrategy());

        tripPlanDAO.updateInfoById(tripPlanId, updateTripPlanDTO.getFareType(), updateTripPlanDTO.getFarePrice(),
                updateTripPlanDTO.getTransferCount(), updateTripPlanDTO.getRoutingStrategy(),
                updateTripPlanDTO.getNotes(), LocalDateTime.now(ZoneOffset.UTC));

        TripPlanVO updatedTripPlan = tripPlanDAO.getById(tripPlanId)
                .orElseThrow(() -> new TripPlanNotFoundException("找不到 id:" + tripPlanId + " 的旅程規劃!"));
        enrichTravelTime(updatedTripPlan);

        return updatedTripPlan;
    }

    /**
     * 匯出指定旅程規劃的 excel 檔。
     *
     * @param tripPlanId
     * @return byte[]
     * @throws UserNotFoundException              找不到當前使用者。
     * @throws TripPlanNotFoundException          找不到指定旅程規劃，或該旅程規劃已被軟刪除。
     * @throws TripPlanAccessDeniedException      嘗試操作非本人擁有的旅程規劃。
     * @throws TripPlanExportExcelFailedException 匯出 excel 檔發生錯誤。
     */
    public byte[] exportTripPlanExcel(Integer tripPlanId) {
        logger.debug("開始呼叫 API 來匯出旅程規劃 excel，tripPlanId: {}", tripPlanId);

        getCurrentUserIdAndCheckOwnership(tripPlanId);

        TripPlanVO tripPlan = tripPlanDAO.getById(tripPlanId)
                .orElseThrow(() -> new TripPlanNotFoundException("找不到 id:" + tripPlanId + " 的旅程規劃!"));

        return exportTripPlanToExcel(tripPlan);
    }

    // ----- Private Helpers -----

    /**
     * 將排序方向正規化為大寫，並以白名單驗證是否為合法值（避免任意字串進入 SQL）。
     *
     * @param sortDirection 排序方向，null 時視為預設值 DESC
     * @return 正規化後的排序方向（ASC 或 DESC）
     * @throws IllegalArgumentException 排序方向不合法。
     */
    private String normalizeSortDirection(String sortDirection) {
        String normalizedDirection = sortDirection == null ? "DESC" : sortDirection.toUpperCase();
        if (!VALID_SORT_DIRECTIONS.contains(normalizedDirection)) {
            throw new IllegalArgumentException("不支援的排序方向: " + sortDirection + "，有效值為 ASC、DESC");
        }
        return normalizedDirection;
    }

    /**
     * 驗證票種與路線規劃策略是否為合法值，不合法則拋出異常。
     *
     * @param fareType
     * @param routingStrategy
     * @throws IllegalArgumentException 票種或路線規劃策略代碼不合法。
     */
    private void validateFareTypeAndRoutingStrategy(Integer fareType, Integer routingStrategy) {
        if (!VALID_FARE_TYPES.contains(fareType)) {
            throw new IllegalArgumentException("不支援的票種: " + fareType + FARE_TYPE_ROUTING_STRATEGY_HINT);
        }
        if (!VALID_ROUTING_STRATEGIES.contains(routingStrategy)) {
            throw new IllegalArgumentException("不支援的路線策略: " + routingStrategy + FARE_TYPE_ROUTING_STRATEGY_HINT);
        }
    }

    /**
     * 取得當前使用者，並驗證其是否為指定旅程規劃的擁有者。
     *
     * @param tripPlanId
     * @return 當前使用者 id
     * @throws UserNotFoundException         找不到當前使用者。
     * @throws TripPlanNotFoundException     找不到指定旅程規劃，或該旅程規劃已被軟刪除。
     * @throws TripPlanAccessDeniedException 當前使用者非該旅程規劃的擁有者。
     */
    private Integer getCurrentUserIdAndCheckOwnership(Integer tripPlanId) {
        String email = getAuthenticatedEmail();

        User user = userDAO.getByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("找不到當前使用者!"));

        Integer ownerId = tripPlanDAO.getActiveOwnerId(tripPlanId)
                .orElseThrow(() -> new TripPlanNotFoundException("找不到 id:" + tripPlanId + " 的旅程規劃!"));

        if (!ownerId.equals(user.getId())) {
            throw new TripPlanAccessDeniedException("不得操作非本人的旅程規劃!");
        }

        return user.getId();
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
     * 依旅程規劃的起訖站與路線規劃策略，即時計算總車程時間並補進傳入的 {@link TripPlanVO}。
     *
     * @param tripPlan
     * @throws StationNotFoundException 找不到起站或訖站對應的路線車站代碼。
     */
    private void enrichTravelTime(TripPlanVO tripPlan) {
        Integer travelTimeSeconds = metroService.getTravelTimeSecondsByStationIds(
                tripPlan.getFromStationId(), tripPlan.getToStationId(), tripPlan.getRoutingStrategy());
        tripPlan.setTravelTimeSeconds(travelTimeSeconds);
    }

    /**
     * 將指定旅程規劃匯出為 excel 檔。
     *
     * @param tripPlan
     * @return byte[]
     * @throws TripPlanExportExcelFailedException
     */
    private byte[] exportTripPlanToExcel(TripPlanVO tripPlan) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                Workbook workbook = new XSSFWorkbook();) {

            Sheet sheet = workbook.createSheet("旅程規劃");

            Row headerRow = sheet.createRow(0);
            String[] headers = { "旅程名稱", "起站", "訖站", "票種", "票價", "轉乘次數", "路線策略", "備註", "建立時間(UTC)" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(tripPlan.getName());
            row.createCell(1).setCellValue(tripPlan.getFromStationNameZhTw());
            row.createCell(2).setCellValue(tripPlan.getToStationNameZhTw());
            row.createCell(3).setCellValue(describeFareType(tripPlan.getFareType()));
            row.createCell(4).setCellValue(tripPlan.getFarePrice().toPlainString());
            row.createCell(5).setCellValue(tripPlan.getTransferCount());
            row.createCell(6).setCellValue(describeRoutingStrategy(tripPlan.getRoutingStrategy()));
            row.createCell(7).setCellValue(tripPlan.getNotes() != null ? tripPlan.getNotes() : "");
            row.createCell(8).setCellValue(tripPlan.getCreatedAt().format(EXCEL_DATE_TIME_FORMATTER));

            workbook.write(byteArrayOutputStream);

            return byteArrayOutputStream.toByteArray();
        } catch (IOException error) {
            throw new TripPlanExportExcelFailedException("匯出旅程規劃 excel 檔發生錯誤!");
        }
    }

    /**
     * 將票種代碼轉換為中文說明。
     *
     * @param fareType
     * @return 中文說明
     */
    private String describeFareType(Integer fareType) {
        if (fareType == null) {
            return "";
        }
        return switch (fareType) {
            case 1 -> "全票";
            case 4 -> "學生";
            case 5 -> "兒童";
            case 7 -> "愛心";
            default -> String.valueOf(fareType);
        };
    }

    /**
     * 將路線規劃策略代碼轉換為中文說明。
     *
     * @param routingStrategy
     * @return 中文說明
     */
    private String describeRoutingStrategy(Integer routingStrategy) {
        if (routingStrategy == null) {
            return "";
        }
        return switch (routingStrategy) {
            case 1 -> "最少轉乘次數";
            case 2 -> "最短車程時間";
            default -> String.valueOf(routingStrategy);
        };
    }
}
