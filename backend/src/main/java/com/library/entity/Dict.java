package com.library.entity;

import com.easy.query.core.annotation.Column;
import com.easy.query.core.annotation.EntityProxy;
import com.easy.query.core.annotation.Table;
import com.easy.query.core.proxy.ProxyEntityAvailable;
import com.library.entity.proxy.DictProxy;
import lombok.Data;

/**
 * 系统字典实体（code / name / pcode 树形层级）。
 */
@Data
@Table("dict")
@EntityProxy
public class Dict implements ProxyEntityAvailable<Dict, DictProxy> {

    /** 雪花 ID 主键 */
    @Column(primaryKey = true)
    private String id;

    /** 字典编码（全局唯一，创建后不可修改） */
    private String code;

    /** 字典名称 */
    private String name;

    /** 父字典编码（空=顶层） */
    private String pcode;

    /** 同级排序 */
    private Integer sortOrder;
}
