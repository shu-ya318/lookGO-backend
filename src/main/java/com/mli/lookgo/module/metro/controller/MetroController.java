package com.mli.lookgo.module.metro.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mli.lookgo.core.result.MessageVO;
import com.mli.lookgo.core.result.PaginatedVO;
import com.mli.lookgo.module.metro.model.dto.StationIdDTO;
import com.mli.lookgo.module.metro.model.dto.StationRouteDTO;
import com.mli.lookgo.module.metro.model.dto.StationDetailsDTO;
import com.mli.lookgo.module.metro.model.dto.UpdateStationDTO;
import com.mli.lookgo.module.metro.model.entity.Line;
import com.mli.lookgo.module.metro.model.entity.LineStation;
import com.mli.lookgo.module.metro.model.entity.LineTransfer;
import com.mli.lookgo.module.metro.model.entity.Station;
import com.mli.lookgo.module.metro.model.vo.MapVO;
import com.mli.lookgo.module.metro.model.vo.OriginDestinationDetailVO;
import com.mli.lookgo.module.metro.model.vo.StationDetailVO;
import com.mli.lookgo.module.metro.model.vo.StationIdOptionVO;
import com.mli.lookgo.module.metro.model.vo.StationOptionVO;
import com.mli.lookgo.module.metro.model.vo.StationSummaryVO;
import com.mli.lookgo.module.metro.service.MetroService;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 處理前端查詢捷運資料的 HTTP 請求的介面層。負責把資料傳給業務層處理，最後封裝結果為 HTTP 回應回傳給客戶端。
 *
 * @author D5042101
 * @since 2026.06.24
 */
@RestController
@RequestMapping("/api/v1/metro")
@Tag(name = "Metro", description = "前端查詢捷運資料相關操作的 API")
public class MetroController {

        private final MetroService metroService;
        private static final Logger logger = LoggerFactory.getLogger(MetroController.class);

        /**
         * 讓 Spring 容器能在應用程式啟動時，自動注入查詢捷運資料相關的業務層 {@link MetroService}。
         *
         * @param metroService
         */
        public MetroController(MetroService metroService) {
                this.metroService = metroService;
        }

        /**
         * 取得所有路線資料。
         *
         * @return ResponseEntity<List<Line>>
         */
        @Operation(summary = "取得所有路線資料", description = "從資料庫取得所有路線資料")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "成功取得所有路線資料", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Line.class))),
                        @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
        @PostMapping("/get-all-line")
        public ResponseEntity<List<Line>> getAllLine() {
                logger.debug("收到查詢所有路線資料的請求");
                List<Line> lines = metroService.getAllLine();

                return ResponseEntity.ok(lines);
        }

        /**
         * 取得所有車站資料。
         *
         * @return ResponseEntity<List<Station>>
         */
        @Operation(summary = "取得所有車站資料", description = "從資料庫取得所有車站資料")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "成功取得所有車站資料", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Station.class))),
                        @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
        @PostMapping("/get-all-station")
        public ResponseEntity<List<Station>> getAllStation() {
                logger.debug("收到查詢所有車站資料的請求");
                List<Station> stations = metroService.getAllStation();

                return ResponseEntity.ok(stations);
        }

        /**
         * 取得所有路線車站資料。
         *
         * @return ResponseEntity<List<LineStation>>
         */
        @Operation(summary = "取得所有路線車站資料", description = "從資料庫取得所有路線車站資料")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "成功取得所有路線車站資料", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LineStation.class))),
                        @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
        @PostMapping("/get-all-line-station")
        public ResponseEntity<List<LineStation>> getAllLineStation() {
                logger.debug("收到查詢所有路線車站資料的請求");
                List<LineStation> lineStations = metroService.getAllLineStation();

                return ResponseEntity.ok(lineStations);
        }

        /**
         * 取得所有路線車站的代碼與中文名稱，供前端下拉選單使用。
         *
         * @return ResponseEntity<List<StationOptionVO>>
         */
        @Operation(summary = "取得所有車站選項", description = "取得所有路線車站的代碼與中文名稱，不含車站設施等詳細資料，供前端下拉選單使用")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "成功取得所有車站選項", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StationOptionVO.class))),
                        @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
        @PostMapping("/get-all-station-option")
        public ResponseEntity<List<StationOptionVO>> getAllStationOption() {
                logger.debug("收到查詢所有車站選項資料的請求");
                List<StationOptionVO> stationOptions = metroService.getAllStationOption();

                return ResponseEntity.ok(stationOptions);
        }

        /**
         * 取得所有路線換乘資料。
         *
         * @return ResponseEntity<List<LineTransfer>>
         */
        @Operation(summary = "取得所有路線換乘資料", description = "從資料庫取得所有路線換乘車站與換乘時間資料")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "成功取得所有路線換乘資料", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LineTransfer.class))),
                        @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
        @PostMapping("/get-all-line-transfer")
        public ResponseEntity<List<LineTransfer>> getAllLineTransfer() {
                logger.debug("收到查詢所有路線換乘資料的請求");
                List<LineTransfer> lineTransfers = metroService.getAllLineTransfer();

                return ResponseEntity.ok(lineTransfers);
        }

        /**
         * 依車站代碼取得車站詳細資料。
         *
         * @param stationDetailsDTO
         * @return ResponseEntity<StationDetailVO>
         */
        @Operation(summary = "依車站代碼取得車站詳細資料", description = "依傳入的車站代碼從資料庫取得該車站的詳細資料")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "成功取得車站詳細資料", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StationDetailVO.class))),
                        @ApiResponse(responseCode = "400", description = "請求參數錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "請輸入車站代碼!"))),
                        @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
        @PostMapping("/get-station-by-code")
        public ResponseEntity<StationDetailVO> getStationByCode(
                        @Valid @RequestBody StationDetailsDTO stationDetailsDTO) {
                logger.debug("收到依車站代碼查詢車站詳細資料的請求，stationCode: {}", stationDetailsDTO.getStationCode());
                StationDetailVO stationDetail = metroService.getStationByCode(stationDetailsDTO);

                return ResponseEntity.ok(stationDetail);
        }

        /**
         * 取得所有車站的 id 與中文名稱，供車站管理頁面下拉選單使用，僅限 ADMIN 角色存取。
         *
         * @return ResponseEntity<List<StationIdOptionVO>>
         */
        @Operation(summary = "取得所有車站 id 選項", description = "取得所有車站的 id 與中文名稱，僅限 ADMIN 角色存取，供車站管理頁面下拉選單使用")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "成功取得所有車站 id 選項", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StationIdOptionVO.class))),
                        @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "403", description = "權限不足", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "權限不足，無法操作!"))),
                        @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
        @PreAuthorize("hasRole('ADMIN')")
        @PostMapping("/get-all-station-id-option")
        public ResponseEntity<List<StationIdOptionVO>> getAllStationIdOption() {
                logger.debug("收到查詢所有車站 id 選項資料的請求");
                List<StationIdOptionVO> stationIdOptions = metroService.getAllStationIdOption();

                return ResponseEntity.ok(stationIdOptions);
        }

        /**
         * 取得分頁與模糊搜尋後的車站資料，僅限 ADMIN 角色存取。
         *
         * @param keyword
         * @param page
         * @param size
         * @return ResponseEntity<PaginatedVO<StationSummaryVO>>
         */
        @Operation(summary = "取得所有車站資訊", description = "取得所有車站的資訊，僅限 ADMIN 角色存取，支援分頁與模糊搜尋")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "成功取得所有車站的資訊", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginatedVO.class))),
                        @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "403", description = "權限不足", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "權限不足，無法操作!"))),
                        @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
        @PreAuthorize("hasRole('ADMIN')")
        @PostMapping("/get-all-station-paginated")
        public ResponseEntity<PaginatedVO<StationSummaryVO>> getAllStation(
                        @Parameter(description = "搜尋關鍵字") @RequestParam(name = "keyword", required = false) String keyword,
                        @Parameter(description = "頁碼 (從 0 起算)") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "每頁筆數") @RequestParam(defaultValue = "8") int size) {
                logger.debug("收到分頁查詢車站資料的請求，keyword: {}, page: {}, size: {}", keyword, page, size);
                PaginatedVO<StationSummaryVO> paginatedStations = metroService.getAllStation(keyword, page, size);

                return ResponseEntity.ok(paginatedStations);
        }

        /**
         * 依車站 id 查詢車站詳細資料，僅限 ADMIN 角色存取，供車站管理頁面編輯前帶出目前資料使用。
         *
         * @param stationIdDTO
         * @return ResponseEntity<Station>
         */
        @Operation(summary = "依車站 id 查詢車站詳細資料", description = "依傳入的車站 id 從資料庫取得該車站的詳細資料，僅限 ADMIN 角色存取")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "成功取得車站詳細資料", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Station.class))),
                        @ApiResponse(responseCode = "400", description = "請求參數錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "請輸入車站id!"))),
                        @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "403", description = "權限不足", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "權限不足，無法操作!"))),
                        @ApiResponse(responseCode = "404", description = "找不到指定車站", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "找不到id:1的車站!"))),
                        @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
        @PreAuthorize("hasRole('ADMIN')")
        @PostMapping("/get-station-by-id")
        public ResponseEntity<Station> getStationById(@Valid @RequestBody StationIdDTO stationIdDTO) {
                logger.debug("收到依車站 id 查詢車站詳細資料的請求，id: {}", stationIdDTO.getId());
                Station station = metroService.getStationById(stationIdDTO);

                return ResponseEntity.ok(station);
        }

        /**
         * 更新指定車站的資料，僅限 ADMIN 角色存取，僅會更新有帶值的欄位（不含車站中文名稱）。
         *
         * @param updateStationDTO
         * @return ResponseEntity<MessageVO>
         */
        @Operation(summary = "更新車站資料", description = "更新指定車站的資料，僅限 ADMIN 角色存取，除 id 外皆為選填，僅會更新有帶值的欄位，車站中文名稱不開放更新")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "車站資料更新成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageVO.class))),
                        @ApiResponse(responseCode = "400", description = "請求參數錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "請至少提供一個要修改的欄位!"))),
                        @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "403", description = "權限不足", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "權限不足，無法操作!"))),
                        @ApiResponse(responseCode = "404", description = "找不到指定車站", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "找不到id:1的車站!"))),
                        @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
        @PreAuthorize("hasRole('ADMIN')")
        @PostMapping("/update-station")
        public ResponseEntity<MessageVO> updateStation(@Valid @RequestBody UpdateStationDTO updateStationDTO) {
                logger.debug("收到更新車站資料的請求，updateStationDTO: {}", updateStationDTO);
                MessageVO apiResult = metroService.updateStation(updateStationDTO);

                return ResponseEntity.ok(apiResult);
        }

        /**
         * 取得捷運路網地圖資料。
         *
         * @return ResponseEntity<MapVO>
         */
        @Operation(summary = "取得捷運路網地圖資料", description = "整合路線顏色、各路線有序車站清單與換乘連結，一次回傳供前端 D3.js 繪圖使用")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "成功取得捷運路網地圖資料", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MapVO.class))),
                        @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
        @PostMapping("/get-metro-map")
        public ResponseEntity<MapVO> getMetroMap() {
                logger.debug("收到查詢捷運路網地圖資料的請求");
                MapVO metroMap = metroService.getMetroMap();

                return ResponseEntity.ok(metroMap);
        }

        /**
         * 依起始、終點車站代碼取得兩站間詳細資料，可選擇性指定票種與路線策略。
         *
         * @param stationRouteDTO
         * @return ResponseEntity<OriginDestinationDetailVO>
         */
        @Operation(summary = "取得起終點站詳細資料", description = "依起終點車站代碼取得兩站間路線段、轉乘次數、行駛時間與票價，可選擇性指定票種（全票/優惠票等）與路線策略（最少轉乘次數/最短車程時間）")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "成功取得起終點站詳細資料", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OriginDestinationDetailVO.class))),
                        @ApiResponse(responseCode = "400", description = "請求參數錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "請輸入起始車站代碼!"))),
                        @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
        @PostMapping("/get-origin-destination-detail")
        public ResponseEntity<OriginDestinationDetailVO> getOriginDestinationDetail(
                        @Valid @RequestBody StationRouteDTO stationRouteDTO) {
                logger.debug("收到查詢起終點站詳細資料的請求，fromStationCode: {}，toStationCode: {}",
                                stationRouteDTO.getFromStationCode(), stationRouteDTO.getToStationCode());
                OriginDestinationDetailVO originDestinationDetail = metroService
                                .getOriginDestinationDetail(stationRouteDTO);

                return ResponseEntity.ok(originDestinationDetail);
        }
}
