package com.library.dto.book;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 图书副本累加请求（重复 ISBN 录入确认后做数量叠加）。
 */
@Data
public class AddCopiesRequest {

    /** 本次新增的副本数量 */
    @NotNull(message = "累加数量不能为空")
    @Min(value = 1, message = "累加数量至少为 1")
    private Integer copies;
}
