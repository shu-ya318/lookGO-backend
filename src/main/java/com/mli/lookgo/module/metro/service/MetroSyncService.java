package com.mli.lookgo.module.metro.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
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
import com.mli.lookgo.module.metro.model.vo.TdxLineVO;
import com.mli.lookgo.module.metro.model.vo.TdxStationVO;
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

        List<TdxLineVO> tdxStationVOs = tdxApiClient.getAllLine();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        List<Line> lines = new ArrayList<>(tdxStationVOs.size());
        for (TdxLineVO tdxLineVO : tdxStationVOs) {
            lines.add(this.toLineEntity(tdxLineVO, now));
        }

        metroDao.upsertAllLine(lines);
        logger.debug("路線資料同步完成，共 {} 筆", lines.size());

        return new ApiResult("路線資料同步成功!");
    }

    /**
     * 從 TDX API 取得車站名稱，從 TPE API 取得車站設施，合併後同步寫入資料庫。
     *
     * @return ApiResult
     */
    @Transactional
    public ApiResult syncAllStation() {
        logger.debug("開始從 TDX + TPE 同步車站資料");

        List<TdxStationVO> tdxStationVOs = tdxApiClient.getAllStation();
        // List<TpeStationVO> tpeStationVOs = tpeApiClient.getAllStation();

        // 以車站中文名稱為 key，建立 TPE 設施資料的 Map
        // Map<String, TpeStationVO> tpeMap = tpeStationVOs.stream()
        // .collect(Collectors.toMap(TpeStationVO::getStationName, vo -> vo, (a, b) ->
        // a));

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        List<Station> stations = new ArrayList<>(tdxStationVOs.size());
        for (TdxStationVO tdxStationVO : tdxStationVOs) {
            stations.add(this.toStationEntity(tdxStationVO, now));
        }

        metroDao.upsertAllStation(stations);
        logger.debug("車站資料同步完成，共 {} 筆", stations.size());

        return new ApiResult("車站資料同步成功!");
    }

    /**
     * 將 TDX 回傳的 TdxLineVO 轉換為資料庫的 Line 實體，補上 status 和 updatedAt。
     *
     * @param vo  TDX 回傳的路線資料
     * @param now 當下 UTC 時間
     * @return Line
     */
    private Line toLineEntity(TdxLineVO tdxLineVO, LocalDateTime updatedAt) {
        return new Line(
                tdxLineVO.getLetter(),
                tdxLineVO.getNameZhTw(),
                tdxLineVO.getNameEn(),
                tdxLineVO.getColor(),
                1, // 考量: 預設有資料即為啟用狀態
                updatedAt);
    }

    /**
     * 合併 TDX 車站名稱和 TPE 車站設施資料，轉換為資料庫的 Station 實體。
     *
     * @param tdxStationVO TDX 回傳的車站名稱資料
     * @param tpeVO        TPE 回傳的車站設施資料（可能為 null，表示該站無對應設施資料）
     * @param now          當下 UTC 時間
     * @return Station
     */
    // private Station toStationEntity(TdxStationVO tdxStationVO, TpeStationVO
    // tpeVO,
    // LocalDateTime now) {
    // return new Station(
    // tdxStationVO.getNameZhTw(),
    // tdxStationVO.getNameEn(),
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
    private Station toStationEntity(TdxStationVO tdxStationVO,
            LocalDateTime updatedAt) {
        return new Station(
                tdxStationVO.getNameZhTw(),
                tdxStationVO.getNameEn(),
                1, // 考量: 預設有資料即為啟用狀態
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                updatedAt);
    }
}
