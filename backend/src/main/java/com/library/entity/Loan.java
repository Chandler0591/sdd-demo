package com.library.entity;

import com.easy.query.core.annotation.Column;
import com.easy.query.core.annotation.EntityProxy;
import com.easy.query.core.annotation.Table;
import com.easy.query.core.proxy.ProxyEntityAvailable;
import com.library.entity.proxy.LoanProxy;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 借阅记录实体。
 */
@Data
@Table("loan")
@EntityProxy
public class Loan implements ProxyEntityAvailable<Loan, LoanProxy> {

    /** 雪花 ID 主键 */
    @Column(primaryKey = true)
    private String id;

    /** 图书 ID */
    private String bookId;

    /** 读者 ID */
    private String readerId;

    /** 借出时间 */
    private LocalDateTime borrowedAt;

    /** 应还时间 */
    private LocalDateTime dueAt;

    /** 归还时间（null=未归还） */
    private LocalDateTime returnedAt;
}
