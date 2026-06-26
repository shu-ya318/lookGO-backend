package com.mli.lookgo.module.metro.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.mli.lookgo.module.metro.model.vo.TpeStationResponse;
import com.mli.lookgo.module.metro.model.vo.TpeStationVO;

/**
 * 負責呼叫 TPE (台北市開放資料) API 的客戶端。不需要驗證即可呼叫。
 *
 * @author D5042101
 * @since 2026.06.25
 */
@Component
public class TpeApiClient {

    private static final Logger logger = LoggerFactory.getLogger(TpeApiClient.class);

    private static final String BASE_URL = "https://data.taipei/api/v1/dataset";
    private static final String STATION_DATASET_ID = "f69dfd66-3d8e-408a-9645-c02384bda5b8";

    private final RestTemplate railRestTemplate;

    public TpeApiClient(RestTemplate railRestTemplate) {
        this.railRestTemplate = railRestTemplate;
    }

    // ----- 具體的 API 請求定義 -----

    /**
     * 取得所有車站設施資料（含電梯、電扶梯）。
     *
     * @return List<TpeStationVO>
     */
    public List<TpeStationVO> getAllStation() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.setAll(Map.of("scope", "resourceAquire", "limit", "1000"));

        return sendGetRequest(STATION_DATASET_ID, params);
    }

    // ----- Private Helpers -----

    private List<TpeStationVO> sendGetRequest(String datasetId,
            MultiValueMap<String, String> queryParams) {
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

        logger.debug("準備呼叫 TpeApiClient API，URL: {}", url);

        TpeStationResponse response = railRestTemplate.getForObject(url, TpeStationResponse.class);

        logger.debug("TpeApiClient API 呼叫完成，response: {}", response);

        if (response == null) {
            return Collections.emptyList();
        }

        return response.getAllStation();
    }
}
