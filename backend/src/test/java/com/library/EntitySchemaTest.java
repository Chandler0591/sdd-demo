package com.library;

import com.easy.query.api.proxy.client.EasyEntityQuery;
import com.easy.query.core.exception.EasyQuerySQLCommandException;
import com.library.entity.Book;
import com.library.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 任务 2.1 验证：建表脚本创建五张表及唯一约束，新增记录主键为雪花 ID。
 */
class EntitySchemaTest extends BaseIntegrationTest {

    @Autowired
    private EasyEntityQuery entityQuery;

    @Autowired
    private SnowflakeIdGenerator idGenerator;

    @Test
    void insertBook_shouldAssignSnowflakeId() {
        Book book = new Book();
        book.setId(idGenerator.nextId());
        book.setIsbn("9787111111111");
        book.setTitle("测试图书");
        book.setAuthor("测试作者");
        book.setTotalCopies(3);
        book.setAvailableCopies(3);
        book.setPendingShelving(0);
        entityQuery.insertable(book).executeRows();

        Book loaded = entityQuery.queryable(Book.class)
                .where(b -> b.isbn().eq("9787111111111"))
                .firstOrNull();
        assertNotNull(loaded);
        assertEquals(19, loaded.getId().length());
        assertTrue(loaded.getId().matches("\\d{19}"), "主键应为 19 位数字雪花 ID");
    }

    @Test
    void insertDuplicateIsbn_shouldBeRejectedByUniqueConstraint() {
        Book first = new Book();
        first.setId(idGenerator.nextId());
        first.setIsbn("9787222222222");
        first.setTitle("第一本");
        first.setTotalCopies(1);
        first.setAvailableCopies(1);
        first.setPendingShelving(0);
        entityQuery.insertable(first).executeRows();

        Book duplicate = new Book();
        duplicate.setId(idGenerator.nextId());
        duplicate.setIsbn("9787222222222");
        duplicate.setTitle("第二本");
        duplicate.setTotalCopies(1);
        duplicate.setAvailableCopies(1);
        duplicate.setPendingShelving(0);
        assertThrows(EasyQuerySQLCommandException.class, () -> entityQuery.insertable(duplicate).executeRows(),
                "重复 ISBN 应触发唯一约束异常");
    }

    @Test
    void allFiveTables_shouldExist() {
        Integer count = schemaJdbcTemplate().queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() " +
                        "AND table_name IN ('book','reader','loan','shelving_record','dict')", Integer.class);
        assertEquals(5, count, "建表脚本执行后应存在五张表");
    }
}
