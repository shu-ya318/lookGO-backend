package com.mli.lookgo.module.metro.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.mli.lookgo.module.auth.service.RedisService;
import com.mli.lookgo.module.metro.enums.TdxRailSystem;
import com.mli.lookgo.module.metro.model.vo.TdxLineStationVO;
import com.mli.lookgo.module.metro.model.vo.TdxLineTransferVO;
import com.mli.lookgo.module.metro.model.vo.TdxLineVO;
import com.mli.lookgo.module.metro.model.vo.TdxODFareVO;
import com.mli.lookgo.module.metro.model.vo.TdxS2STravelTimeVO;
import com.mli.lookgo.module.metro.model.vo.TdxStationVO;

// 考量: 建立獨立於 Dao 和 Service 層之外的 Client 層，專門負責呼叫第三方服務。
/**
 * 負責呼叫 TDX (運輸資料流通服務) API 的客戶端。不需要驗證即可呼叫。
 *
 * @author D5042101
 * @since 2026.06.25
 */
@Component
public class TdxApiClient {

    private static final String BASE_URL = "https://tdx.transportdata.tw/api/basic";
    private static final String TOKEN_URL = "https://tdx.transportdata.tw/auth/realms/TDXConnect/protocol/openid-connect/token";
    private static final TdxRailSystem RAIL_SYSTEM = TdxRailSystem.TRTC;

    @Value("${rail.api.tdx.client.id}")
    private String clientId;

    @Value("${rail.api.tdx.client.secret}")
    private String clientSecret;

    private static final Logger logger = LoggerFactory.getLogger(TdxApiClient.class);
    // 基礎會員限制 5 次/分鐘；頁間 20 秒 → 每分鐘最多 3 頁，任意 60 秒視窗最多 4 次請求，遠低於上限
    private static final int PAGE_INTERVAL_MS = 20_000;
    // ODFare 前等待 60 秒，確保前序 4 個 sync 操作離開 60 秒滑動視窗後再開始分頁
    private static final int ODFAR_INITIAL_WAIT_MS = 60_000;
    // 429 重試等待 90 秒，為外部 retry 機制的短間隔重試留緩衝後再確認視窗清空
    private static final int RATE_LIMIT_WAIT_MS = 90_000;

    private final RestTemplate railRestTemplate;
    private final RedisService redisService;

    public TdxApiClient(RestTemplate railRestTemplate, RedisService redisService) {
        this.railRestTemplate = railRestTemplate;
        this.redisService = redisService;
    }

    // ----- 具體的各 API 請求定義 -----
    // $top值為數字字串，因 queryParams() 參數只接受 MultiValueMap<String, String>，無法同時處理 多種型別值
    public List<TdxLineVO> getAllLine() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.setAll(Map.of("$format", "JSON", "$top", "2000"));

        return sendGetRequest("/Line", new ParameterizedTypeReference<List<TdxLineVO>>() {
        }, params);
    }

    public List<TdxStationVO> getAllStation() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.setAll(Map.of("$format", "JSON", "$top", "2000"));

        return sendGetRequest("/Station", new ParameterizedTypeReference<List<TdxStationVO>>() {
        }, params);
    }

    public List<TdxLineStationVO> getAllLineStation() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.setAll(Map.of("$format", "JSON", "$top", "2000"));

        return sendGetRequest("/StationOfLine", new ParameterizedTypeReference<List<TdxLineStationVO>>() {
        }, params);
    }

    public List<TdxS2STravelTimeVO> getAllS2STravelTime() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.setAll(Map.of("$format", "JSON", "$top", "2000"));

        return sendGetRequest("/S2STravelTime", new ParameterizedTypeReference<List<TdxS2STravelTimeVO>>() {
        }, params);
    }

    public List<TdxLineTransferVO> getAllLineTransfer() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.setAll(Map.of("$format", "JSON", "$top", "1000"));

        return sendGetRequest("/LineTransfer", new ParameterizedTypeReference<List<TdxLineTransferVO>>() {
        }, params);
    }

    public List<TdxODFareVO> getAllODFare() {
        final int pageSize = 1000;
        int skip = 0;
        List<TdxODFareVO> allResults = new ArrayList<>();

        logger.debug("[ODFare] 開始初始等待 {} 秒，清空前序 sync 操作的速率限制視窗", ODFAR_INITIAL_WAIT_MS / 1000);
        try {
            Thread.sleep(ODFAR_INITIAL_WAIT_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logger.warn("[ODFare] 初始等待被中斷，提前結束");
            return allResults;
        }
        logger.debug("[ODFare] 初始等待完成，開始分頁請求 (pageSize={}, pageIntervalSec={})",
                pageSize, PAGE_INTERVAL_MS / 1000);

        while (true) {
            int pageNumber = (skip / pageSize) + 1;
            logger.debug("[ODFare] 發送第 {} 頁請求 ($skip={}, $top={})", pageNumber, skip, pageSize);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.setAll(Map.of("$format", "JSON", "$top", String.valueOf(pageSize), "$skip", String.valueOf(skip)));

            List<TdxODFareVO> page = sendGetRequest("/ODFare",
                    new ParameterizedTypeReference<List<TdxODFareVO>>() {
                    }, params);

            if (page == null || page.isEmpty()) {
                logger.debug("[ODFare] 第 {} 頁回傳空資料，結束分頁", pageNumber);
                break;
            }

            allResults.addAll(page);
            logger.debug("[ODFare] 第 {} 頁完成，本頁 {} 筆，累計 {} 筆", pageNumber, page.size(), allResults.size());

            if (page.size() < pageSize) {
                logger.debug("[ODFare] 第 {} 頁筆數 ({}) < pageSize ({})，已是最後一頁", pageNumber, page.size(), pageSize);
                break;
            }

            skip += pageSize;
            logger.debug("[ODFare] 等待 {} 秒後請求第 {} 頁", PAGE_INTERVAL_MS / 1000, pageNumber + 1);
            try {
                Thread.sleep(PAGE_INTERVAL_MS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                logger.warn("[ODFare] 頁間等待被中斷，提前結束，已累計 {} 筆", allResults.size());
                break;
            }
        }

        logger.debug("[ODFare] 分頁請求全部完成，共取得 {} 筆 OD 票價資料", allResults.size());
        return allResults;
    }

    // ----- 通用的所有 API 請求定義 -----
    private <T> T sendGetRequest(String subPath, ParameterizedTypeReference<T> responseType,
            MultiValueMap<String, String> queryParams) {
        // 考量: 傳入 queryParams 時建立新的 Map ，避免改變原始 Map 的值
        MultiValueMap<String, String> params;
        if (queryParams != null) {
            params = new LinkedMultiValueMap<>(queryParams);
        } else {
            params = new LinkedMultiValueMap<>();
        }

        // 建立 Request URL
        String url = UriComponentsBuilder.fromUriString(BASE_URL)
                .path("/v2/Rail/Metro" + subPath + "/" + RAIL_SYSTEM.getOperatorCode())
                .queryParams(params)
                .build()
                .toUriString();
        // 不用處理 gzip (因 HttpClient 預設自動加上 Accept-Encoding: gzip : 接收 gzip 解壓縮為 JSON 字串)

        // 實際發送 Request，包含完整參數。失敗時自動重試。
        try {
            return railRestTemplate.exchange(url, HttpMethod.GET, createHttpEntity(), responseType).getBody();
        } catch (HttpClientErrorException.Unauthorized exception) {
            // 當發生未授權錯誤時，移除 Redis 快取
            redisService.deleteTdxAccessToken();

            // 建立新的 token 後再次請求
            return railRestTemplate.exchange(url, HttpMethod.GET, createHttpEntity(), responseType)
                    .getBody();
        } catch (HttpClientErrorException.TooManyRequests exception) {
            // 當觸發速率限制時，等待後重試一次
            logger.warn("TDX API 速率限制 (status code: 429)，等待 {} 秒後重試: {}", RATE_LIMIT_WAIT_MS / 1000, url);
            try {
                Thread.sleep(RATE_LIMIT_WAIT_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw exception;
            }

            return railRestTemplate.exchange(url, HttpMethod.GET, createHttpEntity(), responseType)
                    .getBody();
        }
    }

    // ----- Private Helpers -----

    // 建立 HttpEntity: 在 Request Header 自動附加 Token + 指定資料格式
    private HttpEntity<Void> createHttpEntity() {
        HttpHeaders httpHeaders = new HttpHeaders();

        httpHeaders.setBearerAuth(getOrRefreshToken());
        httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        return new HttpEntity<>(httpHeaders);
    }

    // 使用 OIDC Client Credentials 進行身分驗證，取得 access token (省去手動輸入帳密的操作)
    private synchronized String getOrRefreshToken() {
        // 1.先檢查 Redis ，有存快取就直接返回
        String cachedToken = redisService.getTdxAccessToken();
        if (cachedToken != null) {
            return cachedToken;
        }

        // 2.沒快取時，實際發送請求

        // Form 表單參數 (考量: RestTemplate 傳入 MultiValueMap 時，自動將 Content-Type 設為 Form 表單)
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        formData.add("grant_type", "client_credentials");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);

        // 實際發送 Request + 用 JsonNode 接收 (考量: 省去手動轉換 ObjectMapper)
        JsonNode response = railRestTemplate.postForObject(TOKEN_URL, formData, JsonNode.class);

        // 把新的 token 存入 Redis
        if (response != null && response.has("access_token")) {
            String accessToken = response.get("access_token").asText();
            long expiresInSeconds = response.get("expires_in").asLong();

            redisService.saveTdxAccessToken(accessToken, Math.max(1, expiresInSeconds - 60), TimeUnit.SECONDS);

            return accessToken;
        }

        throw new IllegalStateException("TDX Token 取得失敗，請到官網檢查認證金鑰!");
    }
}
