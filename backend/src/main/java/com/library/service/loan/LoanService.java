package com.library.service.loan;

import com.library.dto.loan.BorrowRequest;
import com.library.dto.loan.LoanVO;
import com.library.entity.Book;
import com.library.entity.Loan;
import com.library.entity.Reader;
import com.library.exception.BusinessException;
import com.library.id.SnowflakeIdGenerator;
import com.library.repository.BookRepository;
import com.library.repository.LoanRepository;
import com.library.repository.ReaderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 借阅流通业务：借出、归还、按读者查询在借记录与超期判定。
 * 借出/归还为单事务：校验、库存/状态变更与借阅记录写入任一步失败整体回滚。
 */
@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final ReaderRepository readerRepository;
    private final SnowflakeIdGenerator idGenerator;

    /** 借期天数（配置项 library.loan-days，默认 60） */
    @Value("${library.loan-days:60}")
    private int loanDays;

    /** 读者同时在借上限（配置项 library.max-active-loans，默认 10） */
    @Value("${library.max-active-loans:10}")
    private int maxActiveLoans;

    public LoanService(LoanRepository loanRepository,
                       BookRepository bookRepository,
                       ReaderRepository readerRepository,
                       SnowflakeIdGenerator idGenerator) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.readerRepository = readerRepository;
        this.idGenerator = idGenerator;
    }

    /**
     * 借出：校验读者未注销、可借库存大于 0、在借未达上限 → 库存减 1 → 创建借阅记录。
     */
    @Transactional
    public LoanVO borrow(BorrowRequest request) {
        Reader reader = readerRepository.findById(request.getReaderId());
        if (reader == null) {
            throw new BusinessException("读者不存在");
        }
        if (!Boolean.TRUE.equals(reader.getActive())) {
            throw new BusinessException("该读者已注销，无法办理借阅");
        }
        Book book = bookRepository.findById(request.getBookId());
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        if (book.getAvailableCopies() == null || book.getAvailableCopies() <= 0) {
            throw new BusinessException("该图书暂无可借库存");
        }
        if (loanRepository.countActiveByReader(reader.getId()) >= maxActiveLoans) {
            throw new BusinessException("该读者在借数量已达上限（" + maxActiveLoans + " 册），无法继续借阅");
        }

        // 库存减 1
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.update(book);

        LocalDateTime now = LocalDateTime.now();
        Loan loan = new Loan();
        loan.setId(idGenerator.nextId());
        loan.setBookId(book.getId());
        loan.setReaderId(reader.getId());
        loan.setBorrowedAt(now);
        loan.setDueAt(now.plusDays(loanDays));
        loanRepository.save(loan);
        return LoanVO.from(loan, book, reader);
    }

    /**
     * 归还：标记归还并记录归还日期 → 图书转入待上架（pending_shelving +1，可借数量不变）。
     */
    @Transactional
    public LoanVO returnBook(String loanId) {
        Loan loan = loanRepository.findById(loanId);
        if (loan == null) {
            throw new BusinessException("借阅记录不存在");
        }
        if (loan.getReturnedAt() != null) {
            throw new BusinessException("该借阅记录已归还，请勿重复操作");
        }
        Book book = bookRepository.findById(loan.getBookId());
        if (book == null) {
            throw new BusinessException("图书不存在");
        }

        loan.setReturnedAt(LocalDateTime.now());
        loanRepository.update(loan);

        // 转入待上架：待上架数量 +1，可借数量不变
        book.setPendingShelving(book.getPendingShelving() == null ? 1 : book.getPendingShelving() + 1);
        bookRepository.update(book);

        Reader reader = readerRepository.findById(loan.getReaderId());
        return LoanVO.from(loan, book, reader);
    }

    /**
     * 按读者查询在借记录（含图书信息与超期标记）。
     */
    public List<LoanVO> activeLoans(String readerId) {
        if (readerRepository.findById(readerId) == null) {
            throw new BusinessException("读者不存在");
        }
        List<Loan> loans = loanRepository.activeLoansByReader(readerId);
        if (loans.isEmpty()) {
            return List.of();
        }
        Map<String, Book> books = bookRepository.findByIds(
                        loans.stream().map(Loan::getBookId).distinct().toList())
                .stream().collect(Collectors.toMap(Book::getId, Function.identity()));
        Reader reader = readerRepository.findById(readerId);
        return loans.stream()
                .map(loan -> LoanVO.from(loan, books.get(loan.getBookId()), reader))
                .toList();
    }
}
