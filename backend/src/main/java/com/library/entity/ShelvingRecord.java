package com.library.entity;

import com.easy.query.core.annotation.Column;
import com.easy.query.core.annotation.EntityProxy;
import com.easy.query.core.annotation.Table;
import com.easy.query.core.proxy.ProxyEntityAvailable;
import com.library.entity.proxy.ShelvingRecordProxy;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 上架记录实体。
 */
@Data
@Table("shelving_record")
@EntityProxy
public class ShelvingRecord implements ProxyEntityAvailable<ShelvingRecord, ShelvingRecordProxy> {

    /** 雪花 ID 主键 */
    @Column(primaryKey = true)
    private String id;

    /** 图书 ID */
    private String bookId;

    /** 书架位置 */
    private String location;

    /** 现场照片存储路径 */
    private String photoPath;

    /** 上架时间（服务端生成） */
    private LocalDateTime shelvedAt;

    /** 操作人姓名 */
    private String operator;

    /** 本次上架册数 */
    private Integer shelvedCount;

    /** 审核状态（PENDING 待审核 / APPROVED 已通过 / REJECTED 已驳回） */
    private String reviewStatus;

    /** 审核人姓名 */
    private String reviewer;

    /** 审核时间（服务端生成） */
    private LocalDateTime reviewedAt;

    /** 驳回理由 */
    private String rejectReason;
}
