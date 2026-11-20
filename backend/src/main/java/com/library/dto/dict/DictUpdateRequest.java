package com.library.dto.dict;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 字典项编辑请求（code 创建后只读，不可修改）。
 */
@Data
public class DictUpdateRequest {

    @NotBlank(message = "字典名称不能为空")
    @Size(max = 100, message = "字典名称过长")
    private String name;

    /** 父字典编码（空=顶层） */
    @Size(max = 64, message = "父编码过长")
    private String pcode;

    /** 同级排序 */
    private Integer sortOrder;
}
