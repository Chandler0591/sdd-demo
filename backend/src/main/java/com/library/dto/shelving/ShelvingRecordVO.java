package com.library.dto.shelving;

import com.library.entity.ShelvingRecord;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 上架记录视图对象。
 */
@Data
public class ShelvingRecordVO {

    private String id;

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

    /** 图书书名（记录检索时组装） */
    private String bookTitle;

    /** 图书 ISBN（记录检索时组装） */
    private String bookIsbn;

    /** 审核状态（PENDING 待审核 / APPROVED 已通过 / REJECTED 已驳回） */
    private String reviewStatus;

    /** 审核人姓名 */
    private String reviewer;

    /** 审核时间（服务端生成） */
    private LocalDateTime reviewedAt;

    /** 驳回理由 */
    private String rejectReason;

    public static ShelvingRecordVO from(ShelvingRecord record) {
        ShelvingRecordVO vo = new ShelvingRecordVO();
        vo.setId(record.getId());
        vo.setBookId(record.getBookId());
        vo.setLocation(record.getLocation());
        vo.setPhotoPath(record.getPhotoPath());
        vo.setShelvedAt(record.getShelvedAt());
        vo.setOperator(record.getOperator());
        vo.setShelvedCount(record.getShelvedCount());
        vo.setReviewStatus(record.getReviewStatus());
        vo.setReviewer(record.getReviewer());
        vo.setReviewedAt(record.getReviewedAt());
        vo.setRejectReason(record.getRejectReason());
        return vo;
    }
}
