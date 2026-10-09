package com.mli.lookgo.module.metro.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import com.mli.lookgo.module.metro.model.vo.StationFacilityVO;
import com.sun.net.httpserver.HttpServer;

class DataTaipeiApiClientConfigTest {

    @Test
    void sendGetCsvRequest_parsesBig5CsvEvenWhenCharsetHeaderIsUnknownToJava() throws Exception {
        byte[] csv;
        try (InputStream in = getClass().getResourceAsStream("/metro/station_facility_big5.csv")) {
            assertNotNull(in);
            csv = in.readAllBytes();
        }

        // 本機伺服器重現官方回應標頭 "charset=BIG-5"（Java 不認得，getForObject 會丟 InvalidMimeTypeException）
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "text/csv;charset=BIG-5");
            exchange.sendResponseHeaders(200, csv.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(csv);
            }
        });
        server.start();

        try {
            DataTaipeiApiClientConfig config = new DataTaipeiApiClientConfig(new RestTemplate(),
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/download");

            List<StationFacilityVO> result = config.sendGetCsvRequest("f69dfd66-3d8e-408a-9645-c02384bda5b8",
                    StationFacilityVO.class);

            assertFalse(result.isEmpty());

            StationFacilityVO songshan = result.stream()
                    .filter(vo -> "松山機場".equals(vo.getStationName()))
                    .findFirst()
                    .orElseThrow();
            // 多行、含逗號的引號欄位需完整保留
            assertTrue(songshan.getElevator().contains("月臺電梯"));
            assertEquals("近出口3", songshan.getTicketMachine());
            assertNotNull(songshan.getRestroom());
        } finally {
            server.stop(0);
        }
    }
}
