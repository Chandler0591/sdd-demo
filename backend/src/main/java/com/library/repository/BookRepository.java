package com.library.repository;

import cn.hutool.core.util.StrUtil;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import com.easy.query.core.api.pagination.EasyPageResult;
import com.library.entity.Book;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 图书数据访问接口：多条件检索、按 ISBN 查询、增删改。
 */
@Repository
public class BookRepository {

    private final EasyEntityQuery entityQuery;

    public BookRepository(EasyEntityQuery entityQuery) {
        this.entityQuery = entityQuery;
    }

    /**
     * 按 ISBN / 书名 / 作者 / 分类多条件分页检索图书（ISBN、书名、作者均模糊匹配）。
     * keyword 为通用关键字：书名或 ISBN 任一模糊命中即返回（小程序查书页使用）。
     */
    public EasyPageResult<Book> search(String isbn, String title, String author, String categoryCode, String keyword, long page, long size) {
        return entityQuery.queryable(Book.class)
                .where(b -> {
                    if (StrUtil.isNotBlank(isbn)) {
                        b.isbn().like(isbn.trim());
                    }
                    if (StrUtil.isNotBlank(title)) {
                        b.title().like(title.trim());
                    }
                    if (StrUtil.isNotBlank(author)) {
                        b.author().like(author.trim());
                    }
                    if (StrUtil.isNotBlank(categoryCode)) {
                        b.categoryCode().eq(categoryCode.trim());
                    }
                    if (StrUtil.isNotBlank(keyword)) {
                        // 书名 OR ISBN 任一命中，与其余条件 AND
                        b.or(() -> {
                            b.title().like(keyword.trim());
                            b.isbn().like(keyword.trim());
                        });
                    }
                })
                .orderBy(b -> b.id().desc())
                .toPageResult(page, size);
    }

    public Book findById(String id) {
        return entityQuery.queryable(Book.class).where(b -> b.id().eq(id)).firstOrNull();
    }

    /**
     * 按 ID 集合批量查询（在借记录的图书信息映射）。
     */
    public List<Book> findByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return entityQuery.queryable(Book.class).where(b -> b.id().in(ids)).toList();
    }

    /**
     * 查询待上架图书清单（pendingShelving > 0）。
     */
    public List<Book> findPendingShelving() {
        return entityQuery.queryable(Book.class)
                .where(b -> b.pendingShelving().gt(0))
                .orderBy(b -> b.id().desc())
                .toList();
    }

    public Book findByIsbn(String isbn) {
        return entityQuery.queryable(Book.class).where(b -> b.isbn().eq(isbn)).firstOrNull();
    }

    /**
     * 同一本书判据：书名 + 出版社 + 作者 + 年份全部一致（生成 ISBN 前查重，命中则复用现有 ISBN 累加副本）。
     */
    public Book findFirstMatch(String title, String publisher, String author, Integer publishYear) {
        return entityQuery.queryable(Book.class)
                .where(b -> {
                    b.title().eq(title);
                    b.publisher().eq(publisher);
                    b.author().eq(author);
                    b.publishYear().eq(publishYear);
                })
                .orderBy(b -> b.id().asc())
                .firstOrNull();
    }

    /**
     * 按 ISBN 前缀查询编号最大的图书（自动生成馆内 ISBN 时取下一序号）。
     */
    public Book findMaxByIsbnPrefix(String prefix) {
        return entityQuery.queryable(Book.class)
                .where(b -> b.isbn().like(prefix + "%"))
                .orderBy(b -> b.isbn().desc())
                .firstOrNull();
    }

    /**
     * 按书名模糊查询（上架记录按书名筛选时定位图书）。
     */
    public List<Book> findByTitleLike(String title) {
        return entityQuery.queryable(Book.class).where(b -> b.title().like(title)).toList();
    }

    public void save(Book book) {
        entityQuery.insertable(book).executeRows();
    }

    public void update(Book book) {
        entityQuery.updatable(book).executeRows();
    }

    public void delete(Book book) {
        entityQuery.deletable(book).allowDeleteStatement(true).executeRows();
    }
}
