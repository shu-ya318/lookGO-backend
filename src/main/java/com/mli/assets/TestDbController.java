package com.mli.assets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestDbController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/db-time")
    public ResponseEntity<String> getDbTime() {
        try {
            // 查詢 SQL Server 的系統時間
            String dbTime = jdbcTemplate.queryForObject("SELECT CAST(GETDATE() AS VARCHAR)", String.class);
            return ResponseEntity.ok("連線成功！SQL Server 目前時間: " + dbTime);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("連線失敗: " + e.getMessage());
        }
    }
}
