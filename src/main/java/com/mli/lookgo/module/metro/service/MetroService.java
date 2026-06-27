package com.mli.lookgo.module.metro.service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mli.lookgo.module.metro.dao.MetroDAO;
import com.mli.lookgo.module.metro.model.entity.Line;
import com.mli.lookgo.module.metro.model.entity.LineStation;
import com.mli.lookgo.module.metro.model.entity.LineTransfer;
import com.mli.lookgo.module.metro.model.entity.Station;
import com.mli.lookgo.module.metro.model.entity.StationFare;
import com.mli.lookgo.module.metro.model.vo.MetroMapVO;

/**
 * 處理前端查詢捷運資料相關的業務邏輯。
 *
 * @author D5042101
 * @since 2026.06.24
 */
@Service
public class MetroService {

    private final MetroDAO metroDAO;
    private static final Logger logger = LoggerFactory.getLogger(MetroService.class);

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入所需的依賴。
     *
     * @param metroDAO
     */
    public MetroService(MetroDAO metroDAO) {
        this.metroDAO = metroDAO;
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
     * 取得所有路線車站資料。
     *
     * @return List<LineStation>
     */
    public List<LineStation> getAllLineStation() {
        logger.debug("開始查詢所有路線車站資料");
        return metroDAO.getAllLineStation();
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
     * 取得所有路線換乘資料。
     *
     * @return List<LineTransfer>
     */
    public List<LineTransfer> getAllLineTransfer() {
        logger.debug("開始查詢所有路線換乘資料");
        return metroDAO.getAllLineTransfer();
    }

    /**
     * 整合路線、車站與換乘資料，組成供前端 D3.js 繪圖使用的捷運路網地圖資料。
     *
     * @return MetroMapVO
     */
    public MetroMapVO getMetroMap() {
        logger.debug("開始組合捷運路網地圖資料");

        List<Line> lines = metroDAO.getAllLine();
        List<Station> stations = metroDAO.getAllStation();
        List<LineStation> lineStations = metroDAO.getAllLineStation();
        List<LineTransfer> lineTransfers = metroDAO.getAllLineTransfer();

        // station_id → Station
        Map<Integer, Station> stationById = stations.stream()
                .collect(Collectors.toMap(Station::getId, s -> s, (a, b) -> a));

        // lines_stations.id → station_code (換乘對應用)
        Map<Integer, String> lineStationIdToCode = lineStations.stream()
                .filter(ls -> ls.getId() != null && ls.getStationCode() != null)
                .collect(Collectors.toMap(LineStation::getId, LineStation::getStationCode, (a, b) -> a));

        // line_id → List<LineStation>，依 station_sequence 升序
        Map<Short, List<LineStation>> lineStationsByLineId = lineStations.stream()
                .filter(ls -> ls.getLineId() != null)
                .collect(Collectors.groupingBy(LineStation::getLineId));

        // 組合 LineVO 清單
        List<MetroMapVO.LineVO> lineVOs = lines.stream()
                .map(line -> {
                    List<MetroMapVO.StationVO> stationVOs = lineStationsByLineId
                            .getOrDefault(line.getId(), Collections.emptyList())
                            .stream()
                            .sorted(Comparator.comparingInt(
                                    ls -> ls.getStationSequence() != null ? ls.getStationSequence() : 0))
                            .map(ls -> {
                                Station s = stationById.get(ls.getStationId());
                                return new MetroMapVO.StationVO(
                                        ls.getStationCode(),
                                        ls.getStationId(),
                                        s != null ? s.getNameZhTw() : null,
                                        s != null ? s.getNameEn() : null,
                                        ls.getStationSequence());
                            })
                            .collect(Collectors.toList());

                    return new MetroMapVO.LineVO(
                            line.getLetter(),
                            line.getColor(),
                            line.getNameZhTw(),
                            line.getNameEn(),
                            stationVOs);
                })
                .collect(Collectors.toList());

        // 組合 TransferVO 清單 (fromStationCode / toStationCode 任一對應不到時跳過)
        List<MetroMapVO.TransferVO> transferVOs = lineTransfers.stream()
                .map(lt -> new MetroMapVO.TransferVO(
                        lineStationIdToCode.get(lt.getFromLineStationId()),
                        lineStationIdToCode.get(lt.getToLineStationId()),
                        lt.getTransferTime()))
                .filter(tv -> tv.getFromStationCode() != null && tv.getToStationCode() != null)
                .collect(Collectors.toList());

        logger.debug("捷運路網地圖資料組合完成，共 {} 條路線、{} 個換乘連結",
                lineVOs.size(), transferVOs.size());

        return new MetroMapVO(lineVOs, transferVOs);
    }
}
