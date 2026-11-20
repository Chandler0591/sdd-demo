package com.library.dto.book;

import com.library.entity.Book;
import lombok.Data;

/**
 * 图书视图对象：含分类名称与库存/位置展示信息。
 */
@Data
public class BookVO {

    private String id;

    private String isbn;

    private String title;

    private String author;

    /** 出版社 */
    private String publisher;

    /** 出版年份 */
    private Integer publishYear;

    /** 分类字典 code */
    private String categoryCode;

    /** 分类名称（按 code 从字典取 name，用于列表展示） */
    private String categoryName;

    /** 馆藏总量 */
    private Integer totalCopies;

    /** 可借数量（已上架） */
    private Integer availableCopies;

    /** 待上架数量（已归还） */
    private Integer pendingShelving;

    /** 当前位置（最近一次上架记录的书架位置，空 = 待上架） */
    private String location;

    public static BookVO from(Book book, String categoryName) {
        BookVO vo = new BookVO();
        vo.setId(book.getId());
        vo.setIsbn(book.getIsbn());
        vo.setTitle(book.getTitle());
        vo.setAuthor(book.getAuthor());
        vo.setPublisher(book.getPublisher());
        vo.setPublishYear(book.getPublishYear());
        vo.setCategoryCode(book.getCategoryCode());
        vo.setCategoryName(categoryName);
        vo.setTotalCopies(book.getTotalCopies());
        vo.setAvailableCopies(book.getAvailableCopies());
        vo.setPendingShelving(book.getPendingShelving());
        vo.setLocation(book.getLocation());
        return vo;
    }
}
