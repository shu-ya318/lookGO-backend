package com.mli.lookgo.module.metro.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 負責呼叫 DataTaipei (台北市開放資料) API 的客戶端。不需要驗證即可呼叫。
 *
 * @author D5042101
 * @since 2026.06.25
 */
@Component
public class DataTaipeiApiClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(DataTaipeiApiClientConfig.class);

    private static final String BASE_URL = "https://data.taipei/api/v1/dataset";

    private final RestTemplate railRestTemplate;

    public DataTaipeiApiClientConfig(RestTemplate railRestTemplate) {
        this.railRestTemplate = railRestTemplate;
    }

    // ----- 通用的所有 API 請求定義 -----

    public <T> T sendGetRequest(String datasetId, Class<T> responseType,
            MultiValueMap<String, String> queryParams) {
        // 傳入 queryParams 時建立新的 Map ，避免改變原始 Map 的值
        MultiValueMap<String, String> params;
        if (queryParams != null) {
            params = new LinkedMultiValueMap<>(queryParams);
        } else {
            params = new LinkedMultiValueMap<>();
        }

        String url = UriComponentsBuilder.fromUriString(BASE_URL)
                .path("/" + datasetId)
                .queryParams(params)
                .build()
                .toUriString();

        logger.debug("準備呼叫 DataTaipei API，URL: {}", url);

        T response = railRestTemplate.getForObject(url, responseType);

        logger.debug("DataTaipei API 呼叫完成，datasetId: {}", datasetId);

        return response;
    }
}
