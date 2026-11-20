package com.library.service.book;

import cn.hutool.core.util.StrUtil;
import com.easy.query.core.api.pagination.EasyPageResult;
import com.library.dto.common.PageResult;
import com.library.dto.book.AddCopiesRequest;
import com.library.dto.book.BookCreateRequest;
import com.library.dto.book.BookUpdateRequest;
import com.library.dto.book.BookVO;
import com.library.dto.book.RecognizedBookInfo;
import com.library.entity.Book;
import com.library.entity.Dict;
import com.library.exception.BusinessException;
import com.library.id.SnowflakeIdGenerator;
import com.library.repository.BookRepository;
import com.library.repository.DictRepository;
import com.library.repository.LoanRepository;
import com.library.repository.ShelvingRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 图书编目业务：录入、重复 ISBN 累加、编辑、删除前置校验、多条件检索。
 */
@Service
public class BookService {

    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final DictRepository dictRepository;
    private final ShelvingRecordRepository shelvingRecordRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final BookInfoRecognizer recognizer;

    public BookService(BookRepository bookRepository,
                       LoanRepository loanRepository,
                       DictRepository dictRepository,
                       ShelvingRecordRepository shelvingRecordRepository,
                       SnowflakeIdGenerator idGenerator,
                       BookInfoRecognizer recognizer) {
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
        this.dictRepository = dictRepository;
        this.shelvingRecordRepository = shelvingRecordRepository;
        this.idGenerator = idGenerator;
        this.recognizer = recognizer;
    }

    /**
     * 多条件分页检索图书，返回列表及可借库存、当前位置与分类名称。
     */
    public PageResult<BookVO> search(String isbn, String title, String author,
                                     String categoryCode, String keyword, long page, long size) {
        EasyPageResult<Book> result = bookRepository.search(isbn, title, author, categoryCode, keyword, page, size);
        Map<String, String> categoryNames = resolveCategoryNames(result.getData());
        List<BookVO> vos = result.getData().stream()
                .map(book -> BookVO.from(book, categoryNames.getOrDefault(book.getCategoryCode(), book.getCategoryCode())))
                .toList();
        return new PageResult<>(result.getTotal(), vos);
    }

    /**
     * 书籍照片识别：委托识别器（模拟实现）提取书名 / 作者 / ISBN，只识别不落库；
     * 结果由前端展示供工作人员确认 / 修正后走录入接口。
     */
    public RecognizedBookInfo recognize(byte[] photoBytes) {
        return recognizer.recognize(photoBytes);
    }

    /**
     * 按 ISBN 查询已有图书（录入页查重与信息回填用），无则返回 null。
     */
    public BookVO findByIsbn(String isbn) {
        Book book = bookRepository.findByIsbn(isbn.trim());
        return book == null ? null : toVO(book);
    }

    /**
     * 同一本书查重：书名 + 出版社 + 作者 + 年份全部一致才判定为已有馆藏，无则返回 null。
     */
    public BookVO matchBook(String title, String publisher, String author, Integer publishYear) {
        Book book = bookRepository.findFirstMatch(
                title.trim(), publisher.trim(), author.trim(), publishYear);
        return book == null ? null : toVO(book);
    }

    /** 馆内自动生成 ISBN 的前缀（978-7-5566-XXX 序列，与种子数据一致）。 */
    private static final String ISBN_PREFIX = "978-7-5566-";

    /**
     * 四要素（书名+出版社+作者+年份）齐全时查馆内同一本书，供录入时自动复用 ISBN；
     * 任一要素缺失则无法精确判定，返回 null。
     */
    private Book matchExistingByAttributes(BookCreateRequest request) {
        if (StrUtil.isBlank(request.getTitle()) || StrUtil.isBlank(request.getPublisher())
                || StrUtil.isBlank(request.getAuthor()) || request.getPublishYear() == null) {
            return null;
        }
        return bookRepository.findFirstMatch(request.getTitle().trim(), request.getPublisher().trim(),
                request.getAuthor().trim(), request.getPublishYear());
    }

    /**
     * 录入图书：ISBN 留空时先按「书名+出版社+作者+年份」四要素查同一本书，
     * 命中则复用其 ISBN 累加副本；未命中才按馆内规则自动生成新号；
     * ISBN 已存在则直接累加副本（用现有馆藏），否则新建并进入待上架清单。分类取自系统字典。
     */
    public BookVO create(BookCreateRequest request) {
        String isbn = StrUtil.isBlank(request.getIsbn()) ? null : request.getIsbn().trim();
        if (isbn == null) {
            Book same = matchExistingByAttributes(request);
            isbn = same != null ? same.getIsbn() : generateIsbn();
        }
        Book existing = bookRepository.findByIsbn(isbn);
        if (existing != null) {
            // 已有馆藏：直接累加副本，新副本同样进入待上架清单
            existing.setTotalCopies(existing.getTotalCopies() + request.getTotalCopies());
            existing.setPendingShelving(existing.getPendingShelving() + request.getTotalCopies());
            bookRepository.update(existing);
            return toVO(existing);
        }
        String categoryCode = request.getCategoryCode().trim();
        validateCategory(categoryCode);
        Book book = new Book();
        book.setId(idGenerator.nextId());
        book.setIsbn(isbn);
        book.setTitle(request.getTitle().trim());
        book.setAuthor(trimToNull(request.getAuthor()));
        book.setPublisher(trimToNull(request.getPublisher()));
        book.setPublishYear(request.getPublishYear());
        book.setCategoryCode(categoryCode);
        book.setTotalCopies(request.getTotalCopies());
        // 新书入库进入待上架清单：可借 0、待上架 = 馆藏总量，上架确认后恢复可借
        book.setAvailableCopies(0);
        book.setPendingShelving(request.getTotalCopies());
        bookRepository.save(book);
        return toVO(book);
    }

    /**
     * 返回下一个可用的馆内 ISBN（录入页自动生成展示用，可修改后提交）。
     */
    public String nextIsbn() {
        return generateIsbn();
    }

    /**
     * 按馆内规则生成 ISBN：978-7-5566- 前缀 + 现有最大序号 +1（三位补零），无则从 001 开始。
     */
    private String generateIsbn() {
        Book latest = bookRepository.findMaxByIsbnPrefix(ISBN_PREFIX);
        int next = 1;
        if (latest != null) {
            String isbn = latest.getIsbn();
            int idx = isbn.lastIndexOf('-');
            if (idx >= 0) {
                try {
                    next = Integer.parseInt(isbn.substring(idx + 1)) + 1;
                } catch (NumberFormatException ignored) {
                    // 后缀非数字时从 1 开始，保存前仍会走唯一校验
                }
            }
        }
        return ISBN_PREFIX + String.format("%03d", next);
    }

    /**
     * 重复 ISBN 录入确认后的数量叠加：馆藏总量与待上架数量同步累加（新副本同样需上架确认）。
     */
    public BookVO addCopies(String id, AddCopiesRequest request) {
        Book book = requireBook(id);
        int copies = request.getCopies();
        book.setTotalCopies(book.getTotalCopies() + copies);
        book.setPendingShelving(book.getPendingShelving() + copies);
        bookRepository.update(book);
        return toVO(book);
    }

    /**
     * 编辑图书元数据：书名 / 作者 / 分类可改，ISBN 可修正但不得与其他图书重复。
     */
    public BookVO update(String id, BookUpdateRequest request) {
        Book book = requireBook(id);
        if (StrUtil.isNotBlank(request.getIsbn())) {
            String newIsbn = request.getIsbn().trim();
            if (!newIsbn.equals(book.getIsbn())) {
                if (bookRepository.findByIsbn(newIsbn) != null) {
                    throw new BusinessException(HttpStatus.CONFLICT, "ISBN 已被其他图书占用：" + newIsbn);
                }
                book.setIsbn(newIsbn);
            }
        }
        String categoryCode = request.getCategoryCode().trim();
        validateCategory(categoryCode);
        book.setTitle(request.getTitle().trim());
        book.setAuthor(trimToNull(request.getAuthor()));
        book.setPublisher(trimToNull(request.getPublisher()));
        book.setPublishYear(request.getPublishYear());
        book.setCategoryCode(categoryCode);
        bookRepository.update(book);
        return toVO(book);
    }

    /**
     * 删除图书：存在未归还借阅时拒绝；位置记录随书一并移除（已归还借阅历史保留）。
     */
    public void delete(String id) {
        requireBook(id);
        if (loanRepository.existsActiveByBook(id)) {
            throw new BusinessException(HttpStatus.CONFLICT, "该图书仍有未归还借阅记录，请先处理相关借阅");
        }
        shelvingRecordRepository.deleteByBook(id);
        bookRepository.delete(requireBook(id));
    }

    private Book requireBook(String id) {
        Book book = bookRepository.findById(id);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        return book;
    }

    /**
     * 分类必须来自系统字典（录入时经下拉选择，服务端兜底校验）。
     */
    private void validateCategory(String categoryCode) {
        if (dictRepository.findByCode(categoryCode) == null) {
            throw new BusinessException("分类不存在，请从系统字典中选择");
        }
    }

    /**
     * 批量解析分类 code → name（检索列表一次查询，避免逐本 N+1）。
     */
    private Map<String, String> resolveCategoryNames(List<Book> books) {
        List<String> codes = books.stream().map(Book::getCategoryCode).distinct().toList();
        return dictRepository.findByCodes(codes).stream()
                .collect(Collectors.toMap(Dict::getCode, Dict::getName, (a, b) -> a));
    }

    private BookVO toVO(Book book) {
        Dict dict = dictRepository.findByCode(book.getCategoryCode());
        return BookVO.from(book, dict == null ? book.getCategoryCode() : dict.getName());
    }

    private String trimToNull(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }
}
