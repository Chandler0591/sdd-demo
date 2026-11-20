package com.library.web;

import com.easy.query.api.proxy.client.EasyEntityQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查接口：验证 Web 与 MySQL 连接（前端联调使用）。
 */
@RestController
public class HealthController {

    private final EasyEntityQuery entityQuery;

    public HealthController(EasyEntityQuery entityQuery) {
        this.entityQuery = entityQuery;
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        // 执行 SELECT 1 验证数据库连接可用
        entityQuery.sqlQuery("SELECT 1", Integer.class);
        return Map.of("status", "UP", "database", "UP");
    }
}
