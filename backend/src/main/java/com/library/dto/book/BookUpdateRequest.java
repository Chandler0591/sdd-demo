package com.library.dto.book;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 图书编辑请求（元数据修改；ISBN 可修正，但不得与其他图书重复）。
 */
@Data
public class BookUpdateRequest {

    /** 新 ISBN（空 = 保持不变） */
    @Size(max = 32, message = "ISBN 过长")
    private String isbn;

    @NotBlank(message = "书名不能为空")
    @Size(max = 200, message = "书名过长")
    private String title;

    @Size(max = 100, message = "作者名过长")
    private String author;

    /** 出版社（空 = 保持不变） */
    @Size(max = 100, message = "出版社名过长")
    private String publisher;

    /** 出版年份（空 = 保持不变） */
    private Integer publishYear;

    @NotBlank(message = "分类不能为空")
    @Size(max = 64, message = "分类编码过长")
    private String categoryCode;
}
