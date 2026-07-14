package com.mli.lookgo.module.metro.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mli.lookgo.core.result.PaginatedVO;
import com.mli.lookgo.module.metro.dao.MetroDAO;
import com.mli.lookgo.module.metro.enums.StationFacilities;
import com.mli.lookgo.module.metro.exceptions.StationNotFoundException;
import com.mli.lookgo.module.metro.model.dto.StationDetailsDTO;
import com.mli.lookgo.module.metro.model.dto.StationIdDTO;
import com.mli.lookgo.module.metro.model.dto.StationRouteDTO;
import com.mli.lookgo.module.metro.model.dto.UpdateStationDTO;
import com.mli.lookgo.module.metro.model.entity.Line;
import com.mli.lookgo.module.metro.model.entity.LineStation;
import com.mli.lookgo.module.metro.model.entity.LineTransfer;
import com.mli.lookgo.module.metro.model.entity.Station;
import com.mli.lookgo.module.metro.model.entity.StationFare;
import com.mli.lookgo.module.metro.model.graph.DijkstraResult;
import com.mli.lookgo.module.metro.model.graph.Edge;
import com.mli.lookgo.module.metro.model.vo.MapVO;
import com.mli.lookgo.module.metro.model.vo.OriginDestinationDetailVO;
import com.mli.lookgo.module.metro.model.vo.StationDetailVO;
import com.mli.lookgo.module.metro.model.vo.StationIdOptionVO;
import com.mli.lookgo.module.metro.model.vo.StationOptionVO;
import com.mli.lookgo.module.metro.model.vo.StationSummaryVO;
import com.mli.lookgo.module.metro.model.vo.UpdateStationVO;

/**
 * 處理前端查詢捷運資料相關的業務邏輯。
 *
 * @author D5042101
 * @since 2026.06.24
 */
@Service
public class MetroService {

        private final MetroDAO metroDAO;
        private final MetroRouteGraphService metroRouteGraphService;

        private static final Logger logger = LoggerFactory.getLogger(MetroService.class);

        private static final Set<Integer> VALID_FARE_TYPES = Set.of(1, 4, 5, 7);
        private static final Set<Integer> VALID_ROUTING_STRATEGIES = Set.of(1, 2);

        /**
         * 讓 Spring 容器能在應用程式啟動時，自動注入所需的依賴。
         *
         * @param metroDAO
         */
        public MetroService(MetroDAO metroDAO, MetroRouteGraphService metroRouteGraphService) {
                this.metroDAO = metroDAO;
                this.metroRouteGraphService = metroRouteGraphService;
        }

        /**
         * 取得所有路線資料。
         *
         * @return List<Line>
         */
        public List<Line> getAllLine() {
                logger.debug("開始查詢所有路線資料");
                return metroDAO.getAllLine();
        }

        /**
         * 取得所有車站資料。
         *
         * @return List<Station>
         */
        public List<Station> getAllStation() {
                logger.debug("開始查詢所有車站資料");
                return metroDAO.getAllStation();
        }

        /**
         * 用車站 id 查詢指定車站是否存在，供其他模組確認車站有效性使用。
         *
         * @param stationId
         * @return 存在為 true，否則 false
         */
        public boolean existsStationById(Integer stationId) {
                logger.debug("開始查詢車站 id: {} 是否存在", stationId);
                return metroDAO.existsById(stationId);
        }

        /**
         * 依車站 id 取得任一隸屬路線車站代碼，供其他模組以車站 id（而非路線車站代碼）查詢路線資料時轉換使用。
         * 轉乘站雖對應多筆不同路線代碼（例如民權西路同時是 "R13"、"O11"），但路徑演算法已將同站不同代碼視為等價起訖點，任取一筆代碼即可。
         *
         * @param stationId
         * @return Optional<String>
         */
        public Optional<String> getAnyStationCodeByStationId(Integer stationId) {
                logger.debug("開始依車站 id 查詢任一路線車站代碼，stationId: {}", stationId);
                return metroDAO.getAllLineStation().stream()
                                .filter(lineStation -> stationId.equals(lineStation.getStationId()))
                                .map(LineStation::getStationCode)
                                .filter(stationCode -> stationCode != null)
                                .findFirst();
        }

        /**
         * 依起訖車站 id 與路線規劃策略計算總車程時間，供其他模組（如旅程規劃）取得車程時間顯示使用，不含票價與詳細路線段資料。
         *
         * @param fromStationId
         * @param toStationId
         * @param routingStrategy
         * @return 總行駛時間（秒，含轉乘時間）
         * @throws StationNotFoundException 找不到起站或訖站對應的路線車站代碼。
         * @throws IllegalArgumentException 路線規劃策略代碼不合法。
         */
        public Integer getTravelTimeSecondsByStationIds(Integer fromStationId, Integer toStationId,
                        Integer routingStrategy) {
                logger.debug("開始依車站 id 計算總車程時間，fromStationId: {}, toStationId: {}, routingStrategy: {}",
                                fromStationId, toStationId, routingStrategy);

                String fromCode = getAnyStationCodeByStationId(fromStationId)
                                .orElseThrow(() -> new StationNotFoundException(
                                                "找不到 id:" + fromStationId + " 車站對應的路線資料!"));
                String toCode = getAnyStationCodeByStationId(toStationId)
                                .orElseThrow(() -> new StationNotFoundException(
                                                "找不到 id:" + toStationId + " 車站對應的路線資料!"));

                StationRouteDTO stationRouteDTO = new StationRouteDTO();

                stationRouteDTO.setFromStationCode(fromCode);
                stationRouteDTO.setToStationCode(toCode);
                stationRouteDTO.setRoutingStrategy(routingStrategy);

                return getOriginDestinationDetail(stationRouteDTO).getTotalTravelTimeSeconds();
        }

        /**
         * 用車站 id 查詢指定車站的中文名稱，供其他模組組裝匯出檔案等用途使用。
         *
         * @param stationId
         * @return Optional<String>
         */
        public Optional<String> getStationNameById(Integer stationId) {
                logger.debug("開始依車站 id 查詢車站中文名稱，stationId: {}", stationId);
                return metroDAO.getStationNameById(stationId);
        }

        /**
         * 取得所有路線車站資料。
         *
         * @return List<LineStation>
         */
        public List<LineStation> getAllLineStation() {
                logger.debug("開始查詢所有路線車站資料");
                return metroDAO.getAllLineStation();
        }

        /**
         * 取得所有路線車站的代碼與中文名稱，供前端下拉選單使用。
         *
         * @return List<StationOptionVO>
         */
        public List<StationOptionVO> getAllStationOption() {
                logger.debug("開始查詢所有車站選項資料");
                return metroDAO.getAllStationOption();
        }

        /**
         * 取得所有票價資料。
         *
         * @return List<StationFare>
         */
        public List<StationFare> getAllStationFare() {
                logger.debug("開始查詢所有票價資料");
                return metroDAO.getAllStationFare();
        }

        /**
         * 取得所有路線轉乘資料。
         *
         * @return List<LineTransfer>
         */
        public List<LineTransfer> getAllLineTransfer() {
                logger.debug("開始查詢所有路線轉乘資料");
                return metroDAO.getAllLineTransfer();
        }

        /**
         * 依車站代碼取得車站詳細資料。
         *
         * @param stationDetailsDTO
         * @return StationDetailVO
         */
        public StationDetailVO getStationByCode(StationDetailsDTO stationDetailsDTO) {
                if (!metroDAO.existsByStationCode(stationDetailsDTO.getStationCode())) {
                        throw new StationNotFoundException("找不到代碼:" + stationDetailsDTO.getStationCode() + "的車站!");
                }

                logger.debug("開始依車站代碼查詢車站詳細資料，stationCode: {}", stationDetailsDTO.getStationCode());
                return metroDAO.getStationByCode(stationDetailsDTO);
        }

        /**
         * 取得所有車站的 id 與中文名稱，供車站管理頁面下拉選單使用。
         *
         * @return List<StationIdOptionVO>
         */
        public List<StationIdOptionVO> getAllStationIdOption() {
                logger.debug("開始查詢所有車站 id 選項資料");
                return metroDAO.getAllStationIdOption();
        }

        /**
         * 取得分頁與模糊搜尋後的車站資料，僅限 ADMIN 角色存取，角色權限由 Controller 層的 @PreAuthorize 控制。
         *
         * @param keyword
         * @param page
         * @param size
         * @return PaginatedVO<StationSummaryVO>
         */
        public PaginatedVO<StationSummaryVO> getAllStation(String keyword, int page, int size) {
                logger.debug("開始分頁查詢車站資料，keyword: {}, page: {}, size: {}", keyword, page, size);
                List<Station> stations = metroDAO.getAllStationPaginated(keyword, page * size, size);
                long totalElements = metroDAO.countAllStation(keyword);
                int totalPages = (int) Math.ceil((double) totalElements / size);

                List<StationSummaryVO> stationSummaryVOs = stations.stream()
                                .map(station -> new StationSummaryVO(station.getId(), station.getNameZhTw(),
                                                station.getNameEn(), station.getUpdatedAt()))
                                .toList();

                return new PaginatedVO<>(
                                stationSummaryVOs,
                                page,
                                size,
                                totalElements,
                                totalPages);
        }

        /**
         * 依車站 id 查詢車站詳細資料，僅限 ADMIN 角色存取，供車站管理頁面編輯前帶出目前資料使用。
         *
         * @param stationIdDTO
         * @return Station
         */
        public Station getStationById(StationIdDTO stationIdDTO) {
                logger.debug("開始依車站 id 查詢車站詳細資料，id: {}", stationIdDTO.getId());
                return metroDAO.getById(stationIdDTO.getId())
                                .orElseThrow(() -> new StationNotFoundException(
                                                "找不到id:" + stationIdDTO.getId() + "的車站!"));
        }

        /**
         * 更新指定車站的資料，僅限 ADMIN 角色存取，僅會更新有帶值的欄位；同步比對鍵 original_name_zh_tw 不受影響。
         *
         * @param updateStationDTO
         * @return UpdateStationVO 僅包含本次實際異動的欄位
         * @throws StationNotFoundException 找不到指定車站，或更新時已被併發刪除。
         */
        public UpdateStationVO updateStation(UpdateStationDTO updateStationDTO) {
                if (!metroDAO.existsById(updateStationDTO.getId())) {
                        throw new StationNotFoundException("找不到id:" + updateStationDTO.getId() + "的車站!");
                }

                if (updateStationDTO.hasNoUpdatableField()) {
                        throw new IllegalArgumentException("請至少提供一個要修改的欄位!");
                }

                logger.debug("開始更新車站資料，updateStationDTO: {}", updateStationDTO);
                LocalDateTime updatedAt = LocalDateTime.now(ZoneOffset.UTC);
                int affectedRows = metroDAO.updateStationById(updateStationDTO, updatedAt);
                if (affectedRows == 0) {
                        throw new StationNotFoundException("找不到id:" + updateStationDTO.getId() + "的車站!");
                }

                UpdateStationVO updateStationVO = new UpdateStationVO();
                updateStationVO.setId(updateStationDTO.getId());
                updateStationVO.setNameZhTw(updateStationDTO.getNameZhTw());
                updateStationVO.setNameEn(updateStationDTO.getNameEn());
                updateStationVO.setAtm(updateStationDTO.getAtm());
                updateStationVO.setNursingRoom(updateStationDTO.getNursingRoom());
                updateStationVO.setDiaperTable(updateStationDTO.getDiaperTable());
                updateStationVO.setChargingStation(updateStationDTO.getChargingStation());
                updateStationVO.setTicketMachine(updateStationDTO.getTicketMachine());
                updateStationVO.setLocker(updateStationDTO.getLocker());
                updateStationVO.setDrinkingWater(updateStationDTO.getDrinkingWater());
                updateStationVO.setRestroom(updateStationDTO.getRestroom());
                updateStationVO.setElevator(updateStationDTO.getElevator());
                updateStationVO.setEscalator(updateStationDTO.getEscalator());
                updateStationVO.setUpdatedAt(updatedAt);

                return updateStationVO;
        }

        /**
         * 整合路線、車站與轉乘資料，組成供前端繪製路網地圖的資料。
         *
         * @return MapVO
         */
        public MapVO getMetroMap() {
                logger.debug("開始組合捷運路網地圖資料");

                // 1. 從資料庫讀取所有捷運路線、車站、路線車站關聯及轉乘步行時間等基礎資料
                List<Line> lines = metroDAO.getAllLine();
                List<Station> stations = metroDAO.getAllStation();
                List<LineStation> lineStations = metroDAO.getAllLineStation();
                List<LineTransfer> lineTransfers = metroDAO.getAllLineTransfer();

                // 2. 建立「車站 ID -> 車站實體」對照 Map，以利後續透過 ID 查找車站的中英文名稱。例如: {101 -> 淡水車站}
                Map<Integer, Station> stationById = stations.stream()
                                .collect(Collectors.toMap(Station::getId, station -> station,
                                                (existingValue, newValue) -> existingValue));

                // 3. 建立「路線車站關聯表 ID -> 車站代碼」的對照 Map，供轉乘起迄點代碼轉換使用。例如: {30 -> "BL12"}
                Map<Integer, String> lineStationIdToCode = lineStations.stream()
                                .filter(lineStation -> lineStation.getId() != null
                                                && lineStation.getStationCode() != null)
                                .collect(Collectors.toMap(LineStation::getId, LineStation::getStationCode,
                                                (existingValue, newValue) -> existingValue));

                // 4. 將所有路線車站關聯依路線 ID 進行分組，以便按路線快速查出其底下的所有車站。例如: {2 -> [淡水, 紅樹林]}
                Map<Short, List<LineStation>> lineStationsByLineId = lineStations.stream()
                                .filter(lineStation -> lineStation.getLineId() != null)
                                .collect(Collectors.groupingBy(LineStation::getLineId));

                // 5. 遍歷並整合各路線與其依車站順序排序後的車站清單，轉換為前端地圖所需的 LineVO 結構（例如：紅線包含排序好的淡水、紅樹林等車站）
                List<MapVO.LineVO> lineVOs = lines.stream()
                                .map(line -> {
                                        List<MapVO.StationVO> stationVOs = lineStationsByLineId
                                                        .getOrDefault(line.getId(), Collections.emptyList())
                                                        .stream()
                                                        .sorted(Comparator.comparingInt(
                                                                        lineStation -> lineStation
                                                                                        .getStationSequence() != null
                                                                                                        ? lineStation.getStationSequence()
                                                                                                        : 0))
                                                        .map(lineStation -> {
                                                                Station station = stationById
                                                                                .get(lineStation.getStationId());
                                                                return new MapVO.StationVO(
                                                                                lineStation.getStationCode(),
                                                                                lineStation.getStationId(),
                                                                                station != null ? station.getNameZhTw()
                                                                                                : null,
                                                                                station != null ? station.getNameEn()
                                                                                                : null,
                                                                                lineStation.getStationSequence());
                                                        })
                                                        .collect(Collectors.toList());

                                        return new MapVO.LineVO(
                                                        line.getLetter(),
                                                        line.getColor(),
                                                        line.getNameZhTw(),
                                                        line.getNameEn(),
                                                        stationVOs);
                                })
                                .collect(Collectors.toList());

                // 6. 整合轉乘對照資料，將資料庫關聯表 ID 轉換為起迄車站代碼與步行時間（例如：台北車站板南線 "BL12" 到淡水信義線 "R10" 步行需 180
                // 秒）
                List<MapVO.TransferVO> transferVOs = lineTransfers.stream()
                                .map(lineTransfer -> new MapVO.TransferVO(
                                                lineStationIdToCode.get(lineTransfer.getFromLineStationId()),
                                                lineStationIdToCode.get(lineTransfer.getToLineStationId()),
                                                lineTransfer.getTransferTime()))
                                .filter(transferVO -> transferVO.getFromStationCode() != null
                                                && transferVO.getToStationCode() != null)
                                .collect(Collectors.toList());

                logger.debug("捷運路網地圖資料組合完成，共 {} 條路線、{} 個轉乘連結",
                                lineVOs.size(), transferVOs.size());

                return new MapVO(lineVOs, transferVOs);
        }

        /**
         * 依起始、終點車站代碼取得兩站間詳細資料，並可選擇性指定票種與路線策略。
         * 因捷運路網為多路線多車站交織的圖狀結構，使用 Dijkstra (戴克斯特拉)演算法搜尋兩站間的最短路徑。
         * (1) 路線策略 1（最少轉乘次數）：轉乘邊權重為 1、同線邊權重為 0，搜尋結果為轉乘次數最少的路徑。
         * (2) 路線策略 2（最短車程時間）：各邊權重為實際秒數，搜尋結果為車程時間最短的路徑。
         *
         * @param stationRouteDTO
         * @return OriginDestinationDetailVO
         */
        public OriginDestinationDetailVO getOriginDestinationDetail(StationRouteDTO stationRouteDTO) {
                String fromCode = stationRouteDTO.getFromStationCode();
                String toCode = stationRouteDTO.getToStationCode();

                Integer fareType = stationRouteDTO.getFareType();
                Integer routingStrategy = stationRouteDTO.getRoutingStrategy();

                List<StationFacilities> stationFacilities = stationRouteDTO.getStationFacilities();

                // 1. 驗證參數合法性: 傳入的票種與路線規劃策略是否合法，不合法則拋出異常
                if (fareType != null && !VALID_FARE_TYPES.contains(fareType)) {
                        throw new IllegalArgumentException(
                                        "不支援的票種: " + fareType + "，有效值為 1(全票)、4(學生)、5(兒童)、7(愛心)");
                }
                if (routingStrategy != null && !VALID_ROUTING_STRATEGIES.contains(routingStrategy)) {
                        throw new IllegalArgumentException(
                                        "不支援的路線策略: " + routingStrategy + "，有效值為 1(最少轉乘次數)、2(最短車程時間)");
                }

                int strategy = routingStrategy != null ? routingStrategy : 1;

                logger.debug("開始查詢起終點站詳細資料，fromStationCode: {}，toStationCode: {}，strategy: {}",
                                fromCode, toCode, strategy);

                // 2. 取得資料並建立快速查找 Map 映射表: （例如：將 "R28" 代碼映射到淡水站實體）
                List<Line> lines = metroDAO.getAllLine();
                List<Station> stations = metroDAO.getAllStation();
                List<LineStation> lineStations = metroDAO.getAllLineStation();
                List<LineTransfer> lineTransfers = metroDAO.getAllLineTransfer();

                Map<Short, Line> lineById = lines.stream()
                                .filter(line -> line.getId() != null)
                                .collect(Collectors.toMap(Line::getId, line -> line));

                Map<Integer, Station> stationById = stations.stream()
                                .filter(station -> station.getId() != null)
                                .collect(Collectors.toMap(Station::getId, station -> station));

                Map<String, LineStation> lineStationByCode = lineStations.stream()
                                .filter(lineStation -> lineStation.getStationCode() != null)
                                .collect(Collectors.toMap(LineStation::getStationCode, lineStation -> lineStation));

                Map<Integer, LineStation> lineStationById = lineStations.stream()
                                .filter(lineStation -> lineStation.getId() != null)
                                .collect(Collectors.toMap(LineStation::getId, lineStation -> lineStation));

                // 3. 檢查起訖站代碼是否存在於路網 : 確認起點與終點代碼在捷運路網中是否存在，不存在則拋出車站找不到異常
                if (!lineStationByCode.containsKey(fromCode)) {
                        throw new StationNotFoundException("找不到代碼:" + fromCode + "的車站!");
                }
                if (!lineStationByCode.containsKey(toCode)) {
                        throw new StationNotFoundException("找不到代碼:" + toCode + "的車站!");
                }

                LineStation fromLineStation = lineStationByCode.get(fromCode);
                LineStation toLineStation = lineStationByCode.get(toCode);

                // 4. 判斷起迄站是否為同一車站 (實體車站 id 比對)：
                // 轉乘站在不同路線下各有一筆 LineStation 資料（例如民權西路同時是 "R13"、"O11"），
                // 須以實體車站 id 比對是否同站，而非直接比對代碼字串，否則誤判為不同站而多跑一次路徑搜尋
                if (fromLineStation.getStationId().equals(toLineStation.getStationId())) {
                        return metroRouteGraphService.buildSameStationResult(fromCode, fareType, strategy,
                                        lineStationByCode,
                                        lineById, stationById,
                                        stationFacilities);
                }

                // 5. 建立轉乘時間雙向查找 Map (Key: "fromCode:toCode", Value: 轉乘分鐘數)
                Map<String, Short> transferTimeMap = metroRouteGraphService.buildTransferTimeMap(lineTransfers,
                                lineStationById);
                // 建立圖的鄰接權重表 (Build Adjacency List)
                Map<String, List<Edge>> adjacencyList = metroRouteGraphService.buildAdjacencyList(lineStations,
                                lineTransfers,
                                lineStationById, lineStationByCode,
                                strategy);

                // 5a.收集起訖站的等價車站代碼，若起迄點是轉乘站，則收集其在所有路線下的代碼。
                //     再 執行 Dijkstra 演算法，尋找最短路徑
                // 避免使用者選到轉乘站的特定線別代碼（如 "O11"）時，被迫多算一段轉乘到該代碼才算抵達
                Set<String> fromCodes = metroRouteGraphService.collectStationCodesByStationId(
                                lineStationByCode, fromLineStation.getStationId());
                Set<String> toCodes = metroRouteGraphService.collectStationCodesByStationId(
                                lineStationByCode, toLineStation.getStationId());
                DijkstraResult dijkstraResult = metroRouteGraphService.findRoute(adjacencyList, fromCodes, toCodes);

                // 6.完整組裝要回傳資訊 
                // 切分路線段 (RouteSegmentVO)：依轉乘標記將連續同線車站分段。
                // 計算段車程：以段內相鄰站累計時間差逐一相加 (避免分岔站累計基準不同)。
                // 計算總時間與轉乘時間：加總各段車程與轉乘時間的實際秒數。
                // 計算轉乘次數：統計路徑中經過的轉乘邊數量。
                // 查詢票價：依起訖站代碼與指定票種向資料庫查詢對應票價。
                List<OriginDestinationDetailVO.RouteSegmentVO> route = metroRouteGraphService.buildRouteSegments(
                                dijkstraResult.getPath(), dijkstraResult.getPrevIsTransfer(), lineStationByCode,
                                lineById, stationById,
                                stationFacilities);
                int transferCount = (int) dijkstraResult.getPath().stream()
                                .filter(code -> Boolean.TRUE.equals(dijkstraResult.getPrevIsTransfer().get(code)))
                                .count();
                int totalTime = metroRouteGraphService.calculateTotalTime(
                                dijkstraResult.getPath(), dijkstraResult.getPrevIsTransfer(), lineStationByCode,
                                transferTimeMap);
                int transferTime = metroRouteGraphService.calculateTransferTime(
                                dijkstraResult.getPath(), dijkstraResult.getPrevIsTransfer(), transferTimeMap);
                BigDecimal farePrice = fareType != null
                                ? metroDAO.getFareByStationCodesAndType(fromCode, toCode, fareType)
                                : null;

                logger.debug("起終點站詳細資料查詢完成，轉乘次數: {}，總行駛時間: {} 秒，轉乘時間: {} 秒",
                                transferCount, totalTime, transferTime);

                return new OriginDestinationDetailVO(fromCode, toCode, fareType, strategy,
                                route, transferCount, totalTime, transferTime, farePrice);
        }
}
