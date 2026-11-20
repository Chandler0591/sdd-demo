package com.library.dto.shelving;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 上架确认请求：位置与照片必填，操作人姓名手工填写，本次上架数量可选（缺省 1 册）。
 */
@Data
public class ShelvingConfirmRequest {

    @NotBlank(message = "图书不能为空")
    private String bookId;

    @NotBlank(message = "书架位置不能为空")
    @Size(max = 50, message = "书架位置过长")
    private String location;

    @NotBlank(message = "请上传现场照片")
    private String photoPath;

    @NotBlank(message = "操作人不能为空")
    @Size(max = 50, message = "操作人姓名过长")
    private String operator;

    /** 本次上架册数（1 ~ 待上架数量，缺省为 1） */
    @Min(value = 1, message = "本次上架数量至少为 1")
    private Integer count;
}
