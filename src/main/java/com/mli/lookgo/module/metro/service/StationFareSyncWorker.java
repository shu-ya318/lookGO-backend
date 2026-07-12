package com.mli.lookgo.module.metro.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.mli.lookgo.module.metro.config.TDXApiClientConfig;
import com.mli.lookgo.module.metro.dao.MetroDAO;
import com.mli.lookgo.module.metro.model.entity.LineStation;
import com.mli.lookgo.module.metro.model.entity.StationFare;
import com.mli.lookgo.module.metro.model.vo.StationFareVO;

/**
 * 於獨立執行緒背景執行票價同步作業，並將進度回報至 {@link StationFareSyncStateHolder}。
 * <p>
 * 抽出為獨立 {@code @Component} 是因為 Spring 的 {@code @Async} 代理對「同類別內部呼叫」無效
 * （self-invocation 不經代理，註解會失效）；由 {@link MetroSyncService} 跨 bean 呼叫，代理必然生效。
 * <p>
 * 刻意「不」加方法級 {@code @Transactional}：原本整段交易會讓數十萬筆票價落在單一長交易中；
 * 移除後每批 {@code upsertAllStationFare} 各自自動提交，進度真實反映已提交資料，
 * 失敗時已完成批次不回滾（upsert 冪等、可重跑）。
 *
 * @author D5042101
 * @since 2026.07.12
 */
@Component
public class StationFareSyncWorker {

    private final TDXApiClientConfig tdxApiClientConfig;
    private final MetroDAO metroDAO;
    private final StationFareSyncStateHolder stateHolder;

    private static final Logger logger = LoggerFactory.getLogger(StationFareSyncWorker.class);

    /*
     * 資料來源 TDX: 基礎會員限制請求頻率 5 次/分鐘；頁間 20 秒 → 每分鐘最多 3 頁。
     * 票價 API 資料較多，先等待 60 秒清空前幾個 sync 請求的速率限制再開始分頁請求。
     */
    private static final int PAGE_INTERVAL_MS = 20_000;
    private static final int STATION_FARE_INITIAL_WAIT_MS = 60_000;

    // 進度分段：階段一（向 TDX 取得）佔 0–70%，階段二（批次寫入 DB）佔 70–100%
    private static final int FETCH_PHASE_MAX = 70;
    /*
     * 分頁請求無法預先得知總頁數，故以漸近函數回報階段一進度：
     * progress = FETCH_PHASE_MAX * pages / (pages + HALF_LIFE)，永遠單調遞增且不越界（上限 clamp 為 69），
     * 待抓取完成再進入階段二的 70%。此常數控制曲線陡緩。
     */
    private static final int FETCH_PROGRESS_HALF_LIFE = 8;

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入所需的依賴。
     *
     * @param tdxApiClientConfig
     * @param metroDAO
     * @param stateHolder
     */
    public StationFareSyncWorker(TDXApiClientConfig tdxApiClientConfig, MetroDAO metroDAO,
            StationFareSyncStateHolder stateHolder) {
        this.tdxApiClientConfig = tdxApiClientConfig;
        this.metroDAO = metroDAO;
        this.stateHolder = stateHolder;
    }

    /**
     * 於 {@code metroSyncExecutor} 執行緒池非同步執行票價同步。
     * 背景執行緒的例外不會傳遞到任何 HTTP 回應，故此處全數捕捉並回報狀態：
     * 成功呼叫 {@link StationFareSyncStateHolder#markSuccess()}，任何例外則 {@code markFailed} 並記錄 log。
     */
    @Async("metroSyncExecutor")
    public void doSyncAllStationFare() {
        logger.debug("背景執行緒開始同步票價資料");
        try {
            runSync();
            stateHolder.markSuccess();
            logger.debug("票價資料背景同步完成");
        } catch (Exception exception) {
            logger.error("票價資料背景同步失敗", exception);
            stateHolder.markFailed("票價資料同步失敗: " + exception.getMessage());
        }
    }

    /**
     * 0. 需先同步路線車站資料，以確保 station_code 存在。
     * 1. 從 TDX 票價 (StationFare) API 取得任意兩站間票價，再同步寫入資料庫。
     * 2. 暫時跳過 CitizenCode 城市優惠票 (FareClass=3)。
     */
    private void runSync() {
        logger.debug("開始從 TDX 票價 (StationFare) 同步票價資料");
        stateHolder.updateProgress(0, "正在準備票價同步作業...");

        // 1. 取得資料庫中「車站代碼 -> 車站 id」的對照 Map。例如:{"R28" -> 101, "BL12" -> 105}
        Map<String, Integer> stationCodeToIdMap = metroDAO.getAllLineStation().stream()
                .filter(ls -> ls.getStationCode() != null && ls.getStationId() != null)
                .collect(Collectors.toMap(LineStation::getStationCode, LineStation::getStationId,
                        (existingValue, newValue) -> existingValue));

        // 2. 從 TDX API 取得所有車站配對的票價資料（階段一：0–70%）
        List<StationFareVO> stationFareVOs = fetchAllStationFare();
        LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);

        List<StationFare> stationFares = new ArrayList<>();
        for (StationFareVO stationFareVO : stationFareVOs) {
            // 3. 依 API 起迄站代碼查找對應資料庫 id（例如：將起點 "R28" 對應至 101，終點 "BL12" 對應至 105）
            Integer fromStationId = stationCodeToIdMap.get(stationFareVO.getOriginStationId());
            Integer toStationId = stationCodeToIdMap.get(stationFareVO.getDestinationStationId());

            // 4. 若起迄站中任意一站找不到資料庫 id，則警告並略過該筆票價資料
            if (fromStationId == null || toStationId == null) {
                logger.warn("找不到車站 {} 或 {} 對應的資料表 id，跳過",
                        stationFareVO.getOriginStationId(), stationFareVO.getDestinationStationId());
                continue;
            }

            for (StationFareVO.FareDetail fareDetail : stationFareVO.getFares()) {
                // 5. 跳過含有 CitizenCode 的市民優惠票種
                if (fareDetail.getCitizenCode() != null) {
                    continue;
                }

                // 6. 將起迄站 id、票種與票價封裝成 StationFare Entity 並加入清單（例如：淡水至台北車站，普通票 50 元）
                stationFares.add(new StationFare(
                        fromStationId,
                        toStationId,
                        fareDetail.getFareClass(),
                        fareDetail.getPrice(),
                        currentTime));
            }
        }

        // 7. 批次寫入資料庫（階段二：70–100%）
        stateHolder.updateProgress(FETCH_PHASE_MAX, "票價資料取得完成，開始批次寫入資料庫...");
        if (stationFares.isEmpty()) {
            logger.warn("[StationFare] 未取得任何票價資料，略過寫入");
            return;
        }

        final int batchSize = 400;
        int totalInserted = 0;
        for (int i = 0; i < stationFares.size(); i += batchSize) {
            List<StationFare> batch = stationFares.subList(i, Math.min(i + batchSize, stationFares.size()));
            metroDAO.upsertAllStationFare(batch);
            totalInserted += batch.size();

            int progress = FETCH_PHASE_MAX
                    + (int) ((100L - FETCH_PHASE_MAX) * totalInserted / stationFares.size());
            stateHolder.updateProgress(progress, "票價資料更新中... (" + progress + "%)");
            logger.debug("票價資料批次寫入進度：{} / {} 筆", totalInserted, stationFares.size());
        }

        logger.debug("票價資料同步完成，共 {} 筆", stationFares.size());
    }

    /*
     * 資料量大，使用分頁請求:
     * 1. 初始等待
     * 2. 逐頁請求，每頁完成後更新階段一進度（0–70%）
     * 3. 結束分頁: 包含遇到空頁、不足一頁、中斷等情形
     * 4. 累加資料，且每頁請求之間等待固定間隔時間
     */
    private List<StationFareVO> fetchAllStationFare() {
        final int pageSize = 1000;
        int skip = 0;
        List<StationFareVO> allResult = new ArrayList<>();

        stateHolder.updateProgress(0, "正在取得票價資料...");
        logger.debug("[StationFare] 開始初始等待 {} 秒，清空前幾個 sync 請求的速率限制", STATION_FARE_INITIAL_WAIT_MS / 1000);
        try {
            Thread.sleep(STATION_FARE_INITIAL_WAIT_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logger.warn("[StationFare] 初始等待被中斷，提前結束");

            return allResult;
        }

        logger.debug("[StationFare] 初始等待完成，開始分頁請求 (pageSize={}, pageIntervalSec={})",
                pageSize, PAGE_INTERVAL_MS / 1000);

        while (true) {
            int pageNumber = (skip / pageSize) + 1;
            logger.debug("[StationFare] 發送第 {} 頁請求 ($skip={}, $top={})", pageNumber, skip, pageSize);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.setAll(Map.of("$format", "JSON", "$top", String.valueOf(pageSize), "$skip", String.valueOf(skip)));

            List<StationFareVO> page = tdxApiClientConfig.sendGetRequest("/ODFare",
                    new ParameterizedTypeReference<List<StationFareVO>>() {
                    }, params);

            if (page == null || page.isEmpty()) {
                logger.debug("[StationFare] 第 {} 頁回傳空資料，結束分頁", pageNumber);
                break;
            }

            allResult.addAll(page);
            logger.debug("[StationFare] 第 {} 頁完成，本頁 {} 筆，累計 {} 筆", pageNumber, page.size(), allResult.size());

            int progress = fetchProgress(pageNumber);
            stateHolder.updateProgress(progress, "正在同步票價資料... (" + progress + "%)");

            if (page.size() < pageSize) {
                logger.debug("[StationFare] 第 {} 頁筆數 ({}) < pageSize ({})，已是最後一頁", pageNumber, page.size(), pageSize);
                break;
            }

            skip += pageSize;
            logger.debug("[StationFare] 等待 {} 秒後請求第 {} 頁", PAGE_INTERVAL_MS / 1000, pageNumber + 1);

            try {
                Thread.sleep(PAGE_INTERVAL_MS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                logger.warn("[StationFare] 頁間等待被中斷，提前結束，已累計 {} 筆", allResult.size());
                break;
            }
        }

        logger.debug("[StationFare] 分頁請求全部完成，共取得 {} 筆票價資料", allResult.size());
        return allResult;
    }

    /**
     * 依已抓取頁數計算階段一進度百分比（漸近函數，上限 clamp 為 69，避免在抓取完成前顯示 70%）。
     *
     * @param pagesFetched 已抓取的頁數
     * @return 階段一進度百分比 (0-69)
     */
    private int fetchProgress(int pagesFetched) {
        int progress = (int) Math.round(
                (double) FETCH_PHASE_MAX * pagesFetched / (pagesFetched + FETCH_PROGRESS_HALF_LIFE));

        return Math.min(FETCH_PHASE_MAX - 1, progress);
    }
}
