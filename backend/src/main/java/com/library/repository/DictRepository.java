package com.library.repository;

import com.easy.query.api.proxy.client.EasyEntityQuery;
import com.library.entity.Dict;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 系统字典数据访问接口：树形查询、按 pcode 查询子树、增改。
 */
@Repository
public class DictRepository {

    private final EasyEntityQuery entityQuery;

    public DictRepository(EasyEntityQuery entityQuery) {
        this.entityQuery = entityQuery;
    }

    /**
     * 查询全部字典项（按同级排序升序），供树形展示。
     */
    public List<Dict> findAllOrdered() {
        return entityQuery.queryable(Dict.class)
                .orderBy(d -> d.sortOrder().asc())
                .toList();
    }

    /**
     * 按父编码查询直接子节点（业务表单下拉使用）。
     */
    public List<Dict> childrenByPcode(String pcode) {
        return entityQuery.queryable(Dict.class)
                .where(d -> d.pcode().eq(pcode))
                .orderBy(d -> d.sortOrder().asc())
                .toList();
    }

    public Dict findById(String id) {
        return entityQuery.queryable(Dict.class).where(d -> d.id().eq(id)).firstOrNull();
    }

    public Dict findByCode(String code) {
        return entityQuery.queryable(Dict.class).where(d -> d.code().eq(code)).firstOrNull();
    }

    /**
     * 按编码集合批量查询（图书检索列表的 code → name 映射）。
     */
    public List<Dict> findByCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        return entityQuery.queryable(Dict.class).where(d -> d.code().in(codes)).toList();
    }

    public void save(Dict dict) {
        entityQuery.insertable(dict).executeRows();
    }

    public void update(Dict dict) {
        entityQuery.updatable(dict).executeRows();
    }
}
