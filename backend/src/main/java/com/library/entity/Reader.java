package com.library.entity;

import com.easy.query.core.annotation.Column;
import com.easy.query.core.annotation.EntityProxy;
import com.easy.query.core.annotation.Table;
import com.easy.query.core.proxy.ProxyEntityAvailable;
import com.library.entity.proxy.ReaderProxy;
import lombok.Data;

/**
 * 读者实体。
 */
@Data
@Table("reader")
@EntityProxy
public class Reader implements ProxyEntityAvailable<Reader, ReaderProxy> {

    /** 雪花 ID 主键 */
    @Column(primaryKey = true)
    private String id;

    /** 借书证号（唯一） */
    private String cardNo;

    /** 姓名 */
    private String name;

    /** 联系方式 */
    private String phone;

    /** 是否有效（true=正常，false=已注销） */
    private Boolean active;
}
