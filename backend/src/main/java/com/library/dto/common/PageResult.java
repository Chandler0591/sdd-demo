package com.library.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 通用分页响应：{ total, data }。
 */
@Data
@AllArgsConstructor
public class PageResult<T> {

    /** 总记录数 */
    private long total;

    /** 当前页数据 */
    private List<T> data;
}
