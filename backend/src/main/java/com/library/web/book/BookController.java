package com.library.web.book;

import com.library.dto.book.AddCopiesRequest;
import com.library.dto.book.BookCreateRequest;
import com.library.dto.book.BookLocationVO;
import com.library.dto.book.BookUpdateRequest;
import com.library.dto.book.BookVO;
import com.library.dto.book.RecognizedBookInfo;
import com.library.dto.common.PageResult;
import com.library.exception.BusinessException;
import com.library.service.book.BookService;
import com.library.service.shelving.ShelvingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * 图书编目接口：多条件检索、ISBN 查重、新增、副本累加、编辑、删除。
 */
@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    private final ShelvingService shelvingService;

    public BookController(BookService bookService, ShelvingService shelvingService) {
        this.bookService = bookService;
        this.shelvingService = shelvingService;
    }

    /** 多条件分页检索（ISBN / 书名 / 作者 / 分类；keyword = 书名或 ISBN 任一模糊命中） */
    @GetMapping
    public PageResult<BookVO> search(@RequestParam(required = false) String isbn,
                                         @RequestParam(required = false) String title,
                                         @RequestParam(required = false) String author,
                                         @RequestParam(required = false) String categoryCode,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "10") long size) {
        return bookService.search(isbn, title, author, categoryCode, keyword, page, size);
    }

    /** 生成下一个可用的馆内 ISBN（978-7-5566-XXX 序列），录入页自动生成用 */
    @GetMapping("/isbn/next")
    public Map<String, String> nextIsbn() {
        return Map.of("isbn", bookService.nextIsbn());
    }

    /** 按 ISBN 查重（录入页重复 ISBN 提示与信息回填） */
    @GetMapping("/isbn/{isbn}")
    public BookVO findByIsbn(@PathVariable("isbn") String isbn) {
        return bookService.findByIsbn(isbn);
    }

    /** 同一本书查重：书名 + 出版社 + 作者 + 年份全部一致才命中，无则返回 null */
    @GetMapping("/match")
    public BookVO matchBook(@RequestParam("title") String title,
                            @RequestParam("publisher") String publisher,
                            @RequestParam("author") String author,
                            @RequestParam("year") Integer year) {
        return bookService.matchBook(title, publisher, author, year);
    }

    /** 书籍照片识别（模拟实现）：返回书名 / 作者 / ISBN，只识别不落库 */
    @PostMapping("/recognize")
    public RecognizedBookInfo recognize(@RequestParam("file") MultipartFile file) {
        try {
            return bookService.recognize(file.getBytes());
        } catch (IOException e) {
            throw new BusinessException("照片读取失败，请重新上传");
        }
    }

    /** 新增图书 */
    @PostMapping
    public BookVO create(@Valid @RequestBody BookCreateRequest request) {
        return bookService.create(request);
    }

    /** 重复 ISBN 确认后做副本数量累加 */
    @PostMapping("/{id}/add-copies")
    public BookVO addCopies(@PathVariable("id") String id, @Valid @RequestBody AddCopiesRequest request) {
        return bookService.addCopies(id, request);
    }

    /** 编辑图书元数据 */
    @PutMapping("/{id}")
    public BookVO update(@PathVariable("id") String id, @Valid @RequestBody BookUpdateRequest request) {
        return bookService.update(id, request);
    }

    /** 删除图书（存在未归还借阅时拒绝） */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") String id) {
        bookService.delete(id);
    }

    /** 图书当前位置（最近一次上架记录的书架位置，无记录 = 待上架） */
    @GetMapping("/{id}/location")
    public BookLocationVO location(@PathVariable("id") String id) {
        return shelvingService.locationOf(id);
    }
}
