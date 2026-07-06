package com.mli.lookgo.module.stationBookmark.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mli.lookgo.core.result.MessageVO;
import com.mli.lookgo.core.result.PaginatedVO;
import com.mli.lookgo.module.stationBookmark.dao.StationBookmarkDAO;
import com.mli.lookgo.module.stationBookmark.exceptions.BookmarkNotFoundException;
import com.mli.lookgo.module.stationBookmark.exceptions.StationBookmarkExportExcelFailedException;
import com.mli.lookgo.module.stationBookmark.model.entity.UserStationBookmark;
import com.mli.lookgo.module.stationBookmark.model.vo.StationBookmarkVO;

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

    private final StationBookmarkDAO stationBookmarkDAO;

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入車站書籤相關的資料存取層 {@link StationBookmarkDAO}。
     *
     * @param stationBookmarkDAO
     */
    public StationBookmarkService(StationBookmarkDAO stationBookmarkDAO) {
        this.stationBookmarkDAO = stationBookmarkDAO;
    }

    /**
     * 取得分頁與模糊搜尋後的車站書籤列表。
     *
     * @param keyword
     * @param page
     * @param size
     * @return PaginatedVO<StationBookmarkVO>
     */
    public PaginatedVO<StationBookmarkVO> getAllBookmark(String keyword, int page, int size) {
        logger.debug("開始分頁查詢車站書籤資料，keyword: {}, page: {}, size: {}", keyword, page, size);

        List<StationBookmarkVO> bookmarks = stationBookmarkDAO.getAllPaginated(keyword, page * size, size);
        long totalElements = stationBookmarkDAO.countAll(keyword);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return new PaginatedVO<>(bookmarks, page, size, totalElements, totalPages);
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
     * 匯出所有有效（未軟刪除）的車站書籤 excel 檔。
     *
     * @return byte[]
     * @throws StationBookmarkExportExcelFailedException 匯出 excel 檔發生錯誤。
     */
    public byte[] exportBookmarkExcel() {
        logger.debug("開始呼叫 API 來匯出車站書籤 excel");

        List<StationBookmarkVO> bookmarks = stationBookmarkDAO.getAllActive();

        return exportBookmarksToExcel(bookmarks);
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
