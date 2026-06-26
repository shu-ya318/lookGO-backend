package com.mli.lookgo.module.metro.model.vo;

import java.util.Collections;
import java.util.List;

/**
 * 對應 TPE 台北市開放資料 API 的外層回傳結構。
 *
 * @author D5042101
 * @since 2026.06.25
 */
public class TpeStationResponse {

    private TpeStationResult result;

    public List<TpeStationVO> getAllStation() {
        if (result == null || result.getResults() == null) {
            return Collections.emptyList();
        }

        return result.getResults();
    }

    public void setResult(TpeStationResult result) {
        this.result = result;
    }

    @Override
    public String toString() {
        int count = (result != null && result.getResults() != null)
                ? result.getResults().size()
                : 0;
        return "TpeStationResponse{stationCount=" + count + "}";
    }

    public static class TpeStationResult {

        private List<TpeStationVO> results;

        public List<TpeStationVO> getResults() {
            return results;
        }

        public void setResults(List<TpeStationVO> results) {
            this.results = results;
        }
    }
}
