package com.library.repository;

import com.easy.query.api.proxy.client.EasyEntityQuery;
import com.easy.query.core.api.pagination.EasyPageResult;
import com.library.entity.ShelvingRecord;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 上架记录数据访问接口：按图书查询最近记录、分页检索、新增。
 */
@Repository
public class ShelvingRecordRepository {

    private final EasyEntityQuery entityQuery;

    public ShelvingRecordRepository(EasyEntityQuery entityQuery) {
        this.entityQuery = entityQuery;
    }

    /**
     * 查询图书最近一次已通过审核的上架记录（按上架时间倒序取第一条）；
     * 待审核 / 已驳回记录不算已上架，不参与当前位置计算。
     */
    public ShelvingRecord latestByBook(String bookId) {
        return entityQuery.queryable(ShelvingRecord.class)
                .where(s -> s.bookId().eq(bookId))
                .where(s -> s.reviewStatus().eq("APPROVED"))
                .orderBy(s -> s.shelvedAt().desc())
                .limit(1)
                .firstOrNull();
    }

    /**
     * 上架记录分页检索：按书名匹配的图书限定（可空）、按操作人模糊匹配，上架时间倒序。
     */
    public EasyPageResult<ShelvingRecord> search(List<String> bookIds, String operator, long page, long size) {
        return entityQuery.queryable(ShelvingRecord.class)
                .where(s -> {
                    if (bookIds != null && !bookIds.isEmpty()) {
                        s.bookId().in(bookIds);
                    }
                    if (cn.hutool.core.util.StrUtil.isNotBlank(operator)) {
                        s.operator().like(operator.trim());
                    }
                })
                .orderBy(s -> s.shelvedAt().desc())
                .toPageResult(page, size);
    }

    /**
     * 待审核清单：review_status=PENDING 的上架记录，按上架时间倒序。
     */
    public List<ShelvingRecord> findPendingReviews() {
        return entityQuery.queryable(ShelvingRecord.class)
                .where(s -> s.reviewStatus().eq("PENDING"))
                .orderBy(s -> s.shelvedAt().desc())
                .toList();
    }

    /**
     * 按 ID 查询上架记录（审核操作定位记录）。
     */
    public ShelvingRecord findById(String id) {
        return entityQuery.queryable(ShelvingRecord.class)
                .where(s -> s.id().eq(id))
                .firstOrNull();
    }

    public void save(ShelvingRecord record) {
        entityQuery.insertable(record).executeRows();
    }

    public void update(ShelvingRecord record) {
        entityQuery.updatable(record).executeRows();
    }

    /**
     * 删除图书的全部上架记录（删除图书时位置记录随书移除）。
     */
    public void deleteByBook(String bookId) {
        entityQuery.deletable(ShelvingRecord.class)
                .where(s -> s.bookId().eq(bookId))
                .allowDeleteStatement(true)
                .executeRows();
    }
}
