package com.mli.lookgo.module.metro.config;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

/**
 * 負責呼叫 DataTaipei (台北市開放資料) 的客戶端。不需要驗證即可呼叫。
 * <p>
 * 新版 DataTaipei 只提供 CSV 檔案下載（Big5 編碼），這裡下載後以表頭欄位名稱解析成指定的 VO。
 *
 * @author D5042101
 * @since 2026.06.25
 */
@Component
public class DataTaipeiApiClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(DataTaipeiApiClientConfig.class);

    private static final String DEFAULT_DOWNLOAD_URL = "https://data.taipei/api/frontstage/tpeod/dataset/resource.download";

    private static final Charset BIG5 = Charset.forName("Big5");

    // 以第一列為表頭對應 VO 的 @JsonProperty；CsvMapper 為執行緒安全，可共用
    private static final CsvMapper CSV_MAPPER = new CsvMapper();

    private final RestTemplate railRestTemplate;

    private final String downloadUrl;

    @Autowired
    public DataTaipeiApiClientConfig(RestTemplate railRestTemplate) {
        this(railRestTemplate, DEFAULT_DOWNLOAD_URL);
    }

    // 供測試指向本機伺服器
    DataTaipeiApiClientConfig(RestTemplate railRestTemplate, String downloadUrl) {
        this.railRestTemplate = railRestTemplate;
        this.downloadUrl = downloadUrl;
    }

    // ----- 通用的 CSV 下載請求定義 -----

    /**
     * 下載指定資源的 CSV（Big5）並依表頭名稱解析為 VO 清單。因應官方資料格式更新，替換原本呼叫 API 寫法。
     *
     * @param resourceId   DataTaipei 的資源 id（網址中的 rid）
     * @param responseType 每一列要對應的 VO 類別，欄位以 @JsonProperty 對應 CSV 表頭
     * @return 解析後的清單；下載內容為空時回傳空清單
     */
    public <T> List<T> sendGetCsvRequest(String resourceId, Class<T> responseType) {
        String url = UriComponentsBuilder.fromUriString(downloadUrl)
                .queryParam("rid", resourceId)
                .build()
                .toUriString();

        logger.debug("準備下載 DataTaipei CSV，URL: {}", url);

        // 官方回應標頭為 "charset=BIG-5"（Java 不認得），getForObject 會因解析 Content-Type 失敗；
        // 改用 ResponseExtractor 直接讀取 body bytes，繞過 Content-Type 解析，編碼由下方自行處理
        byte[] body = railRestTemplate.execute(url, HttpMethod.GET, null,
                response -> StreamUtils.copyToByteArray(response.getBody()));
        if (body == null || body.length == 0) {
            logger.warn("DataTaipei CSV 內容為空，resourceId: {}", resourceId);
            return Collections.emptyList();
        }

        logger.debug("DataTaipei CSV 下載完成，resourceId: {}，大小: {} bytes", resourceId, body.length);

        // 先以 Big5 解碼成字串，避免解析器依預設 UTF-8 讀取造成亂碼
        String csv = new String(body, BIG5);

        // 第一列為表頭；表頭結尾有未命名的空欄位，交由 VO 的 ignoreUnknown 忽略
        CsvSchema schema = CsvSchema.emptySchema().withHeader();

        try (MappingIterator<T> iterator = CSV_MAPPER.readerFor(responseType).with(schema).readValues(csv)) {
            return iterator.readAll();
        } catch (IOException e) {
            throw new IllegalStateException("解析 DataTaipei CSV 失敗，resourceId: " + resourceId, e);
        }
    }
}
