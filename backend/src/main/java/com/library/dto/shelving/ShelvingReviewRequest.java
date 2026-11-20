package com.library.dto.shelving;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 上架审核请求：审核人姓名必填，驳回时可附理由。
 */
@Data
public class ShelvingReviewRequest {

    @NotBlank(message = "审核人不能为空")
    @Size(max = 50, message = "审核人姓名过长")
    private String reviewer;

    /** 驳回理由（仅驳回时填写） */
    @Size(max = 200, message = "驳回理由过长")
    private String reason;
}
