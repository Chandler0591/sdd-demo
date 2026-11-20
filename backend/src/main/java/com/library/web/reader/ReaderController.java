package com.library.web.reader;

import com.library.dto.common.PageResult;
import com.library.dto.reader.ReaderCreateRequest;
import com.library.dto.reader.ReaderUpdateRequest;
import com.library.dto.reader.ReaderVO;
import com.library.service.reader.ReaderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 读者管理接口：检索、借书证号查重、注册、编辑、注销。
 */
@RestController
@RequestMapping("/api/readers")
public class ReaderController {

    private final ReaderService readerService;

    public ReaderController(ReaderService readerService) {
        this.readerService = readerService;
    }

    /** 分页检索（借书证号 / 姓名） */
    @GetMapping
    public PageResult<ReaderVO> search(@RequestParam(required = false) String cardNo,
                                       @RequestParam(required = false) String name,
                                       @RequestParam(defaultValue = "1") long page,
                                       @RequestParam(defaultValue = "10") long size) {
        return readerService.search(cardNo, name, page, size);
    }

    /** 按借书证号查重 */
    @GetMapping("/card-no/{cardNo}")
    public ReaderVO findByCardNo(@PathVariable("cardNo") String cardNo) {
        return readerService.findByCardNo(cardNo);
    }

    /** 注册新读者 */
    @PostMapping
    public ReaderVO create(@Valid @RequestBody ReaderCreateRequest request) {
        return readerService.create(request);
    }

    /** 编辑读者档案 */
    @PutMapping("/{id}")
    public ReaderVO update(@PathVariable("id") String id, @Valid @RequestBody ReaderUpdateRequest request) {
        return readerService.update(id, request);
    }

    /** 注销读者（存在未归还借阅时拒绝） */
    @PostMapping("/{id}/deactivate")
    public ReaderVO deactivate(@PathVariable("id") String id) {
        return readerService.deactivate(id);
    }
}
