package com.mli.lookgo.module.stationBookmark.service;

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
import com.mli.lookgo.module.stationBookmark.dao.StationBookmarkDAO;
import com.mli.lookgo.module.stationBookmark.exceptions.BookmarkDuplicateException;
import com.mli.lookgo.module.stationBookmark.exceptions.BookmarkLimitExceededException;
import com.mli.lookgo.module.stationBookmark.exceptions.BookmarkNotFoundException;
import com.mli.lookgo.module.stationBookmark.exceptions.StationBookmarkExportExcelFailedException;
import com.mli.lookgo.module.stationBookmark.model.dto.CreateBookmarkDTO;
import com.mli.lookgo.module.stationBookmark.model.entity.UserStationBookmark;
import com.mli.lookgo.module.stationBookmark.model.vo.StationBookmarkVO;
import com.mli.lookgo.module.user.dao.UserDAO;
import com.mli.lookgo.module.user.exceptions.UserNotFoundException;
import com.mli.lookgo.module.user.model.entity.User;

/**
 * 處理車站書籤管理相關業務邏輯。
 *
 * @author D5042101
 * @since 2026.07.06
 */
@Service
public class StationBookmarkService {

    private static final Logger logger = LoggerFactory.getLogger(StationBookmarkService.class);
    private static final DateTimeFormatter EXCEL_DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 有效的排序方向，白名單驗證以避免任意字串進入 SQL。 */
    private static final Set<String> VALID_SORT_DIRECTIONS = Set.of("ASC", "DESC");

    private final StationBookmarkDAO stationBookmarkDAO;
    private final MetroService metroService;
    private final UserDAO userDAO;

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入所需的依賴。
     *
     * @param stationBookmarkDAO
     * @param metroService
     * @param userDAO
     */
    public StationBookmarkService(StationBookmarkDAO stationBookmarkDAO, MetroService metroService,
            UserDAO userDAO) {
        this.stationBookmarkDAO = stationBookmarkDAO;
        this.metroService = metroService;
        this.userDAO = userDAO;
    }

    /**
     * 為當前使用者新增一筆車站書籤。
     *
     * @param createBookmarkDTO
     * @return StationBookmarkVO
     * @throws UserNotFoundException          找不到當前使用者。
     * @throws StationNotFoundException       找不到指定車站。
     * @throws BookmarkDuplicateException     當前使用者已對該車站建立過書籤。
     * @throws BookmarkLimitExceededException 已達會員等級車站書籤數量上限。
     */
    public StationBookmarkVO createBookmark(CreateBookmarkDTO createBookmarkDTO) {
        String email = getAuthenticatedEmail();
        logger.debug("開始呼叫 API 來新增車站書籤，email: {}, createBookmarkDTO: {}", email, createBookmarkDTO);

        User user = userDAO.getByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("找不到當前使用者!"));

        Integer stationId = createBookmarkDTO.getStationId();

        if (!metroService.existsStationById(stationId)) {
            throw new StationNotFoundException("找不到 id:" + stationId + " 的車站!");
        }

        if (stationBookmarkDAO.getActiveBookmarkIdByUserIdAndStationId(user.getId(), stationId).isPresent()) {
            throw new BookmarkDuplicateException("已對 id:" + stationId + " 的車站建立過書籤!");
        }

        int maxBookmarks = stationBookmarkDAO.getMaxBookmarksByUserId(user.getId());
        int activeBookmarkCount = stationBookmarkDAO.countActiveByUserId(user.getId());

        if (activeBookmarkCount >= maxBookmarks) {
            throw new BookmarkLimitExceededException("已達會員等級車站書籤數量上限 (" + maxBookmarks + " 筆)，請先刪除部分書籤!");
        }

        UserStationBookmark bookmark = new UserStationBookmark();
        bookmark.setStationId(stationId);
        bookmark.setUserId(user.getId());
        bookmark.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));

        stationBookmarkDAO.insert(bookmark);

        return stationBookmarkDAO.getVOById(bookmark.getId())
                .orElseThrow(() -> new BookmarkNotFoundException("找不到剛新增的 id:" + bookmark.getId() + " 車站書籤!"));
    }

    /**
     * 依車站中文名稱模糊搜尋，取得當前使用者單一有效（未軟刪除）的車站書籤，若比對到多筆則取收藏時間最新的一筆。
     *
     * @param stationName
     * @return StationBookmarkVO
     * @throws UserNotFoundException     找不到當前使用者。
     * @throws BookmarkNotFoundException 找不到符合條件的車站書籤。
     */
    public StationBookmarkVO getBookmarkByStationName(String stationName) {
        String email = getAuthenticatedEmail();
        logger.debug("開始呼叫 API 依車站名稱查詢單一車站書籤，email: {}, stationName: {}", email, stationName);

        User user = userDAO.getByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("找不到當前使用者!"));

        return stationBookmarkDAO.getActiveVOByUserIdAndStationNameLike(user.getId(), stationName)
                .orElseThrow(() -> new BookmarkNotFoundException("找不到符合「" + stationName + "」的車站書籤!"));
    }

    /**
     * 取得當前使用者分頁與模糊搜尋後的車站書籤列表，依收藏時間排序。
     *
     * @param keyword
     * @param page
     * @param size
     * @param sortDirection 排序方向，DESC=新到舊（預設）、ASC=舊到新
     * @return PaginatedVO<StationBookmarkVO>
     * @throws IllegalArgumentException 排序方向不合法。
     * @throws UserNotFoundException    找不到當前使用者。
     */
    public PaginatedVO<StationBookmarkVO> getAllBookmark(String keyword, int page, int size, String sortDirection) {
        String email = getAuthenticatedEmail();
        logger.debug("開始分頁查詢車站書籤資料，email: {}, keyword: {}, page: {}, size: {}, sortDirection: {}", email, keyword,
                page, size, sortDirection);

        String normalizedDirection = normalizeSortDirection(sortDirection);

        User user = userDAO.getByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("找不到當前使用者!"));

        List<StationBookmarkVO> bookmarks = stationBookmarkDAO.getAllPaginated(user.getId(), keyword, page * size,
                size, normalizedDirection);
        long totalElements = stationBookmarkDAO.countAll(user.getId(), keyword);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return new PaginatedVO<>(bookmarks, page, size, totalElements, totalPages);
    }

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
     * 軟刪除指定的車站書籤。
     *
     * @param bookmarkId
     * @return MessageVO
     * @throws BookmarkNotFoundException 找不到指定書籤，或該書籤已被軟刪除。
     */
    public MessageVO deleteBookmark(Integer bookmarkId) {
        logger.debug("開始呼叫 API 來刪除車站書籤，bookmarkId: {}", bookmarkId);

        UserStationBookmark bookmark = stationBookmarkDAO.getById(bookmarkId)
                .filter(existing -> existing.getDeletedAt() == null)
                .orElseThrow(() -> new BookmarkNotFoundException("找不到 id:" + bookmarkId + " 的車站書籤!"));

        stationBookmarkDAO.softDeleteById(bookmark.getId(), LocalDateTime.now(ZoneOffset.UTC));

        return new MessageVO("書籤刪除成功!");
    }

    /**
     * 匯出當前使用者所有有效（未軟刪除）的車站書籤 excel 檔。
     *
     * @return byte[]
     * @throws UserNotFoundException                      找不到當前使用者。
     * @throws StationBookmarkExportExcelFailedException  匯出 excel 檔發生錯誤。
     */
    public byte[] exportBookmarkExcel() {
        String email = getAuthenticatedEmail();
        logger.debug("開始呼叫 API 來匯出車站書籤 excel，email: {}", email);

        User user = userDAO.getByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("找不到當前使用者!"));

        List<StationBookmarkVO> bookmarks = stationBookmarkDAO.getAllActive(user.getId());

        return exportBookmarksToExcel(bookmarks);
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
     * 將車站書籤列表匯出為 excel 檔。
     *
     * @param bookmarks
     * @return byte[]
     * @throws StationBookmarkExportExcelFailedException
     */
    private byte[] exportBookmarksToExcel(List<StationBookmarkVO> bookmarks) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                Workbook workbook = new XSSFWorkbook();) {

            Sheet sheet = workbook.createSheet("車站書籤清單");

            Row headerRow = sheet.createRow(0);
            String[] headers = { "書籤id", "使用者名稱", "使用者Email", "車站名稱", "收藏時間(UTC)" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowIndex = 1;
            for (StationBookmarkVO bookmark : bookmarks) {
                Row row = sheet.createRow(rowIndex++);

                row.createCell(0).setCellValue(bookmark.getId());
                row.createCell(1).setCellValue(bookmark.getUsername());
                row.createCell(2).setCellValue(bookmark.getEmail());
                row.createCell(3).setCellValue(bookmark.getStationNameZhTw());
                row.createCell(4).setCellValue(bookmark.getCreatedAt().format(EXCEL_DATE_TIME_FORMATTER));
            }

            Row totalRow = sheet.createRow(rowIndex);
            totalRow.createCell(0).setCellValue("【總計】");
            totalRow.createCell(1).setCellValue("共 " + bookmarks.size() + " 筆書籤");

            workbook.write(byteArrayOutputStream);

            return byteArrayOutputStream.toByteArray();
        } catch (IOException error) {
            throw new StationBookmarkExportExcelFailedException("匯出車站書籤 excel 報表發生錯誤!");
        }
    }
}
