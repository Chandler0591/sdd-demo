package com.library.dto.book;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图书当前位置：取最近一次上架记录的书架位置，无记录 = 待上架。
 */
@Data
public class BookLocationVO {

    private String bookId;

    /** 书架位置（null = 待上架） */
    private String location;

    /** 最近一次上架时间（null = 尚未上架） */
    private LocalDateTime shelvedAt;
}
