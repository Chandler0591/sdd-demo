package com.library;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 集成测试基类：使用 library_test 库，每次测试前重建表结构保证隔离。
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected DataSource dataSource;

    @BeforeEach
    void initSchema() throws SQLException {
        // 执行建表脚本（db/schema.sql 相对模块根目录，可重复执行：先删后建，表内数据随之清空）
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("db/schema.sql"));
        }
    }

    protected JdbcTemplate schemaJdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }
}
