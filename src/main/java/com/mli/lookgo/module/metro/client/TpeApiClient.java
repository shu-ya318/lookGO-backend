package com.mli.lookgo.module.metro.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private static final String BASE_URL = "https://data.taipei";
    private static final String STATION_DATASET_ID = "f69dfd66-3d8e-408a-9645-c02384bda5b8";

    private final RestTemplate railRestTemplate;

    public TpeApiClient(RestTemplate railRestTemplate) {
        this.railRestTemplate = railRestTemplate;
    }

    /**
     * 取得所有車站設施資料。
     *
     * @return List<TpeStationVO>
     */
    public List<TpeStationVO> getAllStation() {
        String url = UriComponentsBuilder.fromUriString(BASE_URL)
                .path("/api/v1/dataset/" + STATION_DATASET_ID)
                .queryParam("limit", 1000)
                .queryParam("scope", "resourceAquire")
                .build()
                .toUriString();

        TpeStationResponse response = railRestTemplate.getForObject(url,
                TpeStationResponse.class);

        if (response == null || !response.isSuccess()) {
            return Collections.emptyList();
        }

        return response.getAllStation();
    }
}
