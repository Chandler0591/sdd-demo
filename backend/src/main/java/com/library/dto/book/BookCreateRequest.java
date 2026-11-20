package com.library.dto.book;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 图书新增请求。
 */
@Data
public class BookCreateRequest {

    /** ISBN（留空则按馆内规则 978-7-5566-XXX 自动生成） */
    @Size(max = 32, message = "ISBN 过长")
    private String isbn;

    @NotBlank(message = "书名不能为空")
    @Size(max = 200, message = "书名过长")
    private String title;

    @Size(max = 100, message = "作者名过长")
    private String author;

    /** 出版社（同一本书判据之一：书名+出版社+作者+年份） */
    @Size(max = 100, message = "出版社名过长")
    private String publisher;

    /** 出版年份 */
    private Integer publishYear;

    /** 分类字典 code（取自系统字典下拉） */
    @NotBlank(message = "分类不能为空")
    @Size(max = 64, message = "分类编码过长")
    private String categoryCode;

    /** 馆藏库存数量 */
    @NotNull(message = "馆藏数量不能为空")
    @Min(value = 1, message = "馆藏数量至少为 1")
    private Integer totalCopies;
}
