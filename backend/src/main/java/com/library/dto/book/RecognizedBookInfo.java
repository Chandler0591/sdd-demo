package com.library.dto.book;

import lombok.Data;

/**
 * 书籍照片识别结果：书名、作者、出版社、年份、ISBN，供工作人员确认/修正后录入馆藏。
 */
@Data
public class RecognizedBookInfo {

    /** 书名 */
    private String title;

    /** 作者 */
    private String author;

    /** 出版社 */
    private String publisher;

    /** 出版年份 */
    private Integer publishYear;

    /** ISBN */
    private String isbn;
}
