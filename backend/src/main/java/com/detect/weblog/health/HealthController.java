package com.detect.weblog.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@Tag(name = "Health", description = "서버 상태 확인")
@RestController
@RequestMapping("/api")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record HealthResponse(String status, String database, OffsetDateTime timestamp) {}

    @Operation(summary = "서버 및 DB 상태 확인")
    @GetMapping("/health")
    public HealthResponse health() {
        String database;
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            database = "UP";
        } catch (Exception e) {
            database = "DOWN";
        }
        return new HealthResponse("UP", database, OffsetDateTime.now());
    }

}
