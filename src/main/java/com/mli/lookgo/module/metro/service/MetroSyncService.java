package com.mli.lookgo.module.metro.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mli.lookgo.common.result.ApiResult;
import com.mli.lookgo.module.metro.client.TdxApiClient;
import com.mli.lookgo.module.metro.client.TpeApiClient;
import com.mli.lookgo.module.metro.dao.MetroDao;
import com.mli.lookgo.module.metro.model.entity.Line;
import com.mli.lookgo.module.metro.model.entity.Station;
import com.mli.lookgo.module.metro.model.vo.LineVO;
import com.mli.lookgo.module.metro.model.vo.StationVO;
import com.mli.lookgo.module.metro.model.vo.TpeStationVO;

/**
 * 處理從外部 API 同步捷運資料到資料庫的業務邏輯。
 *
 * @author D5042101
 * @since 2026.06.24
 */
@Service
public class MetroSyncService {

    private final TdxApiClient tdxApiClient;
    private final TpeApiClient tpeApiClient;
    private final MetroDao metroDao;
    private static final Logger logger = LoggerFactory.getLogger(MetroSyncService.class);

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入所需的依賴。
     *
     * @param tdxApiClient
     * @param tpeApiClient
     * @param metroDao
     */
    public MetroSyncService(TdxApiClient tdxApiClient, TpeApiClient tpeApiClient, MetroDao metroDao) {
        this.tdxApiClient = tdxApiClient;
        this.tpeApiClient = tpeApiClient;
        this.metroDao = metroDao;
    }

    /**
     * 從 TDX API 取得路線資料，轉換後同步寫入資料庫。
     *
     * @return ApiResult
     */
    @Transactional
    public ApiResult syncAllLine() {
        logger.debug("開始從 TDX 同步路線資料");

        LineVO[] lineVOs = tdxApiClient.getAllLine();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<Line> lines = Arrays.stream(lineVOs)
                .map(vo -> toLineEntity(vo, now))
                .toList();

        metroDao.upsertAllLine(lines);
        logger.debug("路線資料同步完成，共 {} 筆", lines.size());

        return new ApiResult("路線資料同步成功!");
    }

    /**
     * 從 TDX API 取得車站名稱，從 TPE API 取得車站設施，合併後同步寫入資料庫。
     *
     * @return ApiResult
     */
    // @Transactional
    // public ApiResult syncAllStation() {
    // logger.debug("開始從 TDX + TPE 同步車站資料");

    // StationVO[] tdxStationVOs = tdxApiClient.getAllStation();
    // List<TpeStationVO> tpeStationVOs = tpeApiClient.getAllStation();

    // // 以車站中文名稱為 key，建立 TPE 設施資料的 Map
    // Map<String, TpeStationVO> tpeMap = tpeStationVOs.stream()
    // .collect(Collectors.toMap(TpeStationVO::getStationName, vo -> vo, (a, b) ->
    // a));

    // LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

    // List<Station> stations = Arrays.stream(tdxStationVOs)
    // .map(tdxVO -> toStationEntity(tdxVO, tpeMap.get(tdxVO.getNameZhTw()), now))
    // .toList();

    // metroDao.upsertAllStation(stations);
    // logger.debug("車站資料同步完成，共 {} 筆", stations.size());

    // return new ApiResult("車站資料同步成功!");
    // }

    /**
     * 將 TDX 回傳的 LineVO 轉換為資料庫的 Line 實體，補上 status 和 updatedAt。
     *
     * @param vo  TDX 回傳的路線資料
     * @param now 當下 UTC 時間
     * @return Line
     */
    private Line toLineEntity(LineVO vo, LocalDateTime now) {
        return new Line(
                vo.getLetter(),
                vo.getNameZhTw(),
                vo.getNameEn(),
                vo.getColor(),
                1,
                now);
    }

    /**
     * 合併 TDX 車站名稱和 TPE 車站設施資料，轉換為資料庫的 Station 實體。
     *
     * @param tdxVO TDX 回傳的車站名稱資料
     * @param tpeVO TPE 回傳的車站設施資料（可能為 null，表示該站無對應設施資料）
     * @param now   當下 UTC 時間
     * @return Station
     */
    // private Station toStationEntity(StationVO tdxVO, TpeStationVO tpeVO,
    // LocalDateTime now) {
    // return new Station(
    // tdxVO.getNameZhTw(),
    // tdxVO.getNameEn(),
    // 1,
    // tpeVO != null ? tpeVO.getAtm() : null,
    // tpeVO != null ? tpeVO.getNursingRoom() : null,
    // tpeVO != null ? tpeVO.getDiaperTable() : null,
    // tpeVO != null ? tpeVO.getChargingStation() : null,
    // tpeVO != null ? tpeVO.getTicketMachine() : null,
    // tpeVO != null ? tpeVO.getDrinkingWater() : null,
    // tpeVO != null ? tpeVO.getRestroom() : null,
    // now);
    // }
}
