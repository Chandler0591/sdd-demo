package com.library.repository;

import com.easy.query.api.proxy.client.EasyEntityQuery;
import com.library.entity.Loan;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 借阅记录数据访问接口：在借查询、在借计数、借还增改。
 */
@Repository
public class LoanRepository {

    private final EasyEntityQuery entityQuery;

    public LoanRepository(EasyEntityQuery entityQuery) {
        this.entityQuery = entityQuery;
    }

    /**
     * 查询读者全部未归还借阅记录（按借出时间倒序）。
     */
    public List<Loan> activeLoansByReader(String readerId) {
        return entityQuery.queryable(Loan.class)
                .where(l -> {
                    l.readerId().eq(readerId);
                    l.returnedAt().isNull();
                })
                .orderBy(l -> l.borrowedAt().desc())
                .toList();
    }

    /**
     * 统计读者在借数量。
     */
    public long countActiveByReader(String readerId) {
        Long count = entityQuery.queryable(Loan.class)
                .where(l -> {
                    l.readerId().eq(readerId);
                    l.returnedAt().isNull();
                })
                .selectCount()
                .firstOrNull();
        return count == null ? 0L : count;
    }

    /**
     * 判断图书是否存在未归还借阅记录。
     */
    public boolean existsActiveByBook(String bookId) {
        Long count = entityQuery.queryable(Loan.class)
                .where(l -> {
                    l.bookId().eq(bookId);
                    l.returnedAt().isNull();
                })
                .selectCount()
                .firstOrNull();
        return count != null && count > 0;
    }

    public Loan findById(String id) {
        return entityQuery.queryable(Loan.class).where(l -> l.id().eq(id)).firstOrNull();
    }

    public void save(Loan loan) {
        entityQuery.insertable(loan).executeRows();
    }

    public void update(Loan loan) {
        entityQuery.updatable(loan).executeRows();
    }
}
