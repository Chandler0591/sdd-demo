package com.library.entity;

import com.easy.query.core.annotation.Column;
import com.easy.query.core.annotation.EntityProxy;
import com.easy.query.core.annotation.Table;
import com.easy.query.core.proxy.ProxyEntityAvailable;
import com.library.entity.proxy.BookProxy;
import lombok.Data;

/**
 * 图书实体。
 */
@Data
@Table("book")
@EntityProxy
public class Book implements ProxyEntityAvailable<Book, BookProxy> {

    /** 雪花 ID 主键 */
    @Column(primaryKey = true)
    private String id;

    /** ISBN（唯一） */
    private String isbn;

    /** 书名 */
    private String title;

    /** 作者 */
    private String author;

    /** 出版社 */
    private String publisher;

    /** 出版年份 */
    private Integer publishYear;

    /** 分类字典 code */
    private String categoryCode;

    /** 馆藏总量 */
    private Integer totalCopies;

    /** 可借数量（已上架） */
    private Integer availableCopies;

    /** 待上架数量（已归还） */
    private Integer pendingShelving;

    /** 当前位置（最近一次上架记录的位置） */
    private String location;
}
