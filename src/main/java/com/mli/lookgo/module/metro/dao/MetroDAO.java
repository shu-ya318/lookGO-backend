package com.mli.lookgo.module.metro.dao;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.mli.lookgo.module.metro.model.dto.StationDetailsDTO;
import com.mli.lookgo.module.metro.model.entity.Line;
import com.mli.lookgo.module.metro.model.entity.LineStation;
import com.mli.lookgo.module.metro.model.entity.LineTransfer;
import com.mli.lookgo.module.metro.model.entity.Station;
import com.mli.lookgo.module.metro.model.entity.StationFare;
import com.mli.lookgo.module.metro.model.vo.StationDetailVO;

/**
 * 和資料庫交互，處理捷運資料查詢相關的操作。
 *
 * @author D5042101
 * @since 2026.06.24
 */
@Mapper
public interface MetroDAO {

    // ----- 路線 -----

    /**
     * 批次新增或更新路線資料。
     *
     * @param lines
     */
    void upsertAllLine(@Param("lines") List<Line> lines);

    /**
     * 取得所有路線資料。
     *
     * @return List<Line>
     */
    List<Line> getAllLine();

    // ----- 車站 (去重) -----

    /**
     * 批次新增或更新車站資料。
     *
     * @param stations
     */
    void upsertAllStation(@Param("stations") List<Station> stations);

    /**
     * 用 stationCode 查詢指定車站是否存在。
     *
     * @param stationCode
     * @return 存在為 true，否則 false
     */
    boolean existsByStationCode(@Param("stationCode") String stationCode);

    /**
     * 取得所有車站資料。
     *
     * @return List<Station>
     */
    List<Station> getAllStation();

    /**
     * 依車站代碼取得車站詳細資料，可選擇性依設備過濾。
     *
     * @param dto
     * @return StationDetailVO
     */
    StationDetailVO getStationByCode(@Param("dto") StationDetailsDTO dto);

    // ----- 所有路線的車站 (同車站保留重複資料，因為 station_sequence 不同) -----
    /**
     * 批次新增或更新所有路線的車站資料。
     *
     * @param linesStations
     */
    void upsertAllLineStation(@Param("linesStations") List<LineStation> linesStations);

    /**
     * 取得所有路線的車站資料。
     *
     * @return List<LineStation>
     */
    List<LineStation> getAllLineStation();

    /**
     * 批次更新路線車站的累計行駛時間。以 line_id + station_code 定位更新目標。
     *
     * @param lineStations
     */
    void updateAllLineStationCumulativeTime(@Param("lineStations") List<LineStation> lineStations);

    // ----- 票價 -----

    /**
     * 批次新增或更新票價資料。
     *
     * @param stationFares
     */
    void upsertAllStationFare(@Param("stationFares") List<StationFare> stationFares);

    /**
     * 取得所有票價資料。
     *
     * @return List<StationFare>
     */
    List<StationFare> getAllStationFare();

    /**
     * 依起始、終點車站代碼和票種查詢票價。
     *
     * @param fromStationCode
     * @param toStationCode
     * @param fareType
     * @return BigDecimal
     */
    BigDecimal getFareByStationCodesAndType(@Param("fromStationCode") String fromStationCode,
            @Param("toStationCode") String toStationCode,
            @Param("fareType") Integer fareType);

    // ----- 路線換乘 -----

    /**
     * 批次新增或更新路線換乘資料。
     *
     * @param lineTransfers
     */
    void upsertAllLineTransfer(@Param("lineTransfers") List<LineTransfer> lineTransfers);

    /**
     * 取得所有路線換乘資料。
     *
     * @return List<LineTransfer>
     */
    List<LineTransfer> getAllLineTransfer();
}
