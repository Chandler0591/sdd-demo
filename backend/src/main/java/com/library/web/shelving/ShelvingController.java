package com.library.web.shelving;

import com.library.dto.book.BookVO;
import com.library.dto.common.PageResult;
import com.library.dto.shelving.ShelvingConfirmRequest;
import com.library.dto.shelving.ShelvingRecordVO;
import com.library.dto.shelving.ShelvingReviewRequest;
import com.library.service.shelving.ShelvingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 上架接口：待上架清单查询、上架确认、待审核清单与审核、上架记录检索。
 */
@RestController
@RequestMapping("/api/shelvings")
public class ShelvingController {

    private final ShelvingService shelvingService;

    public ShelvingController(ShelvingService shelvingService) {
        this.shelvingService = shelvingService;
    }

    /** 待上架清单 */
    @GetMapping("/pending")
    public List<BookVO> pending() {
        return shelvingService.pendingList();
    }

    /** 上架确认（位置与照片必填，可一次处理多册；确认后进入待审核） */
    @PostMapping("/confirm")
    public ShelvingRecordVO confirm(@Valid @RequestBody ShelvingConfirmRequest request) {
        return shelvingService.confirm(request);
    }

    /** 待审核清单（上架时间倒序，含书名与 ISBN） */
    @GetMapping("/reviews")
    public List<ShelvingRecordVO> reviews() {
        return shelvingService.reviews();
    }

    /** 审核通过：可借数量按本次上架册数恢复，位置更新为记录位置 */
    @PostMapping("/reviews/{recordId}/approve")
    public ShelvingRecordVO approve(@PathVariable("recordId") String recordId,
                                    @Valid @RequestBody ShelvingReviewRequest request) {
        return shelvingService.approve(recordId, request);
    }

    /** 审核驳回：本次上架册数回补待上架数量（退回待上架清单） */
    @PostMapping("/reviews/{recordId}/reject")
    public ShelvingRecordVO reject(@PathVariable("recordId") String recordId,
                                   @Valid @RequestBody ShelvingReviewRequest request) {
        return shelvingService.reject(recordId, request);
    }

    /** 上架记录分页检索（按书名 / 操作人模糊，上架时间倒序） */
    @GetMapping("/records")
    public PageResult<ShelvingRecordVO> records(@RequestParam(required = false) String title,
                                                @RequestParam(required = false) String operator,
                                                @RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size) {
        return shelvingService.records(title, operator, page, size);
    }
}
