package com.library.web.loan;

import com.library.dto.loan.BorrowRequest;
import com.library.dto.loan.LoanVO;
import com.library.service.loan.LoanService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 借阅流通接口：借出、归还、按读者查询在借记录。
 */
@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    /** 办理借出 */
    @PostMapping
    public LoanVO borrow(@Valid @RequestBody BorrowRequest request) {
        return loanService.borrow(request);
    }

    /** 办理归还 */
    @PostMapping("/{id}/return")
    public LoanVO returnBook(@PathVariable("id") String id) {
        return loanService.returnBook(id);
    }

    /** 按读者查询在借记录（含超期标记） */
    @GetMapping("/reader/{readerId}")
    public List<LoanVO> activeLoans(@PathVariable("readerId") String readerId) {
        return loanService.activeLoans(readerId);
    }
}
