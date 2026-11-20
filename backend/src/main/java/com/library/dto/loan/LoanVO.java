package com.library.dto.loan;

import com.library.entity.Book;
import com.library.entity.Loan;
import com.library.entity.Reader;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 借阅记录视图对象：含图书/读者展示信息与超期标记。
 */
@Data
public class LoanVO {

    private String id;

    private String bookId;

    private String readerId;

    /** 借出时间 */
    private LocalDateTime borrowedAt;

    /** 应还时间 */
    private LocalDateTime dueAt;

    /** 归还时间（null = 未归还） */
    private LocalDateTime returnedAt;

    /** 是否超期（应还日期早于当前且未归还，查询时动态判定） */
    private Boolean overdue;

    /** 图书展示信息 */
    private String bookTitle;

    private String bookIsbn;

    private String bookAuthor;

    /** 读者展示信息 */
    private String readerName;

    private String readerCardNo;

    public static LoanVO from(Loan loan, Book book, Reader reader) {
        LoanVO vo = new LoanVO();
        vo.setId(loan.getId());
        vo.setBookId(loan.getBookId());
        vo.setReaderId(loan.getReaderId());
        vo.setBorrowedAt(loan.getBorrowedAt());
        vo.setDueAt(loan.getDueAt());
        vo.setReturnedAt(loan.getReturnedAt());
        vo.setOverdue(loan.getReturnedAt() == null
                && loan.getDueAt() != null
                && loan.getDueAt().isBefore(LocalDateTime.now()));
        if (book != null) {
            vo.setBookTitle(book.getTitle());
            vo.setBookIsbn(book.getIsbn());
            vo.setBookAuthor(book.getAuthor());
        }
        if (reader != null) {
            vo.setReaderName(reader.getName());
            vo.setReaderCardNo(reader.getCardNo());
        }
        return vo;
    }
}
