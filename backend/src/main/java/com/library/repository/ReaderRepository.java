package com.library.repository;

import cn.hutool.core.util.StrUtil;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import com.easy.query.core.api.pagination.EasyPageResult;
import com.library.entity.Reader;
import org.springframework.stereotype.Repository;

/**
 * 读者数据访问接口：检索、按借书证号查询、增改。
 */
@Repository
public class ReaderRepository {

    private final EasyEntityQuery entityQuery;

    public ReaderRepository(EasyEntityQuery entityQuery) {
        this.entityQuery = entityQuery;
    }

    /**
     * 按借书证号 / 姓名分页检索读者。
     */
    public EasyPageResult<Reader> search(String cardNo, String name, long page, long size) {
        return entityQuery.queryable(Reader.class)
                .where(r -> {
                    if (StrUtil.isNotBlank(cardNo)) {
                        r.cardNo().eq(cardNo.trim());
                    }
                    if (StrUtil.isNotBlank(name)) {
                        r.name().like(name.trim());
                    }
                })
                .orderBy(r -> r.id().desc())
                .toPageResult(page, size);
    }

    public Reader findById(String id) {
        return entityQuery.queryable(Reader.class).where(r -> r.id().eq(id)).firstOrNull();
    }

    public Reader findByCardNo(String cardNo) {
        return entityQuery.queryable(Reader.class).where(r -> r.cardNo().eq(cardNo)).firstOrNull();
    }

    public void save(Reader reader) {
        entityQuery.insertable(reader).executeRows();
    }

    public void update(Reader reader) {
        entityQuery.updatable(reader).executeRows();
    }
}
