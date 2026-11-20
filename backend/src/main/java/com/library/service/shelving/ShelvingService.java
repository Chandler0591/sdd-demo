package com.library.service.shelving;

import com.library.dto.book.BookLocationVO;
import com.library.dto.book.BookVO;
import com.library.dto.common.PageResult;
import com.library.dto.shelving.ShelvingConfirmRequest;
import com.library.dto.shelving.ShelvingRecordVO;
import com.library.dto.shelving.ShelvingReviewRequest;
import com.library.entity.Book;
import com.library.entity.Dict;
import com.library.entity.ShelvingRecord;
import com.library.exception.BusinessException;
import com.library.id.SnowflakeIdGenerator;
import com.library.repository.BookRepository;
import com.library.repository.DictRepository;
import com.library.repository.ShelvingRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 上架业务：待上架清单、上架确认（事务）、图书当前位置查询。
 */
@Service
public class ShelvingService {

    private final BookRepository bookRepository;
    private final ShelvingRecordRepository shelvingRecordRepository;
    private final DictRepository dictRepository;
    private final SnowflakeIdGenerator idGenerator;

    public ShelvingService(BookRepository bookRepository,
                           ShelvingRecordRepository shelvingRecordRepository,
                           DictRepository dictRepository,
                           SnowflakeIdGenerator idGenerator) {
        this.bookRepository = bookRepository;
        this.shelvingRecordRepository = shelvingRecordRepository;
        this.dictRepository = dictRepository;
        this.idGenerator = idGenerator;
    }

    /**
     * 待上架清单：pending_shelving > 0 的图书。
     */
    public List<BookVO> pendingList() {
        List<Book> books = bookRepository.findPendingShelving();
        Map<String, String> categoryNames = books.stream().map(Book::getCategoryCode).distinct().toList().stream()
                .collect(Collectors.toMap(code -> code, code -> {
                    Dict dict = dictRepository.findByCode(code);
                    return dict == null ? code : dict.getName();
                }));
        return books.stream()
                .map(book -> BookVO.from(book, categoryNames.getOrDefault(book.getCategoryCode(), book.getCategoryCode())))
                .toList();
    }

    /**
     * 上架确认（事务）：位置与照片必填校验 → 待上架 -count、写待审核上架记录
     * （含位置 / 照片路径 / 本次上架册数 / 服务端时间戳 / 操作人，review_status=PENDING）。
     * 确认后不恢复可借：可借与当前位置在审核通过时才更新。
     * count 缺省为 1（逐册），支持一次上架多册（批量）。
     */
    @Transactional
    public ShelvingRecordVO confirm(ShelvingConfirmRequest request) {
        Book book = bookRepository.findById(request.getBookId());
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        int pending = book.getPendingShelving() == null ? 0 : book.getPendingShelving();
        if (pending <= 0) {
            throw new BusinessException("该图书不在待上架清单中");
        }
        int count = request.getCount() == null ? 1 : request.getCount();
        if (count > pending) {
            throw new BusinessException("本次上架数量不能超过待上架数量（" + pending + "）");
        }

        // 待上架 -count（从待上架清单移入待审核）；可借与位置待审核通过后更新
        book.setPendingShelving(pending - count);
        bookRepository.update(book);

        ShelvingRecord record = new ShelvingRecord();
        record.setId(idGenerator.nextId());
        record.setBookId(book.getId());
        record.setLocation(request.getLocation().trim());
        record.setPhotoPath(request.getPhotoPath().trim());
        record.setShelvedAt(LocalDateTime.now());
        record.setOperator(request.getOperator().trim());
        record.setShelvedCount(count);
        record.setReviewStatus("PENDING");
        shelvingRecordRepository.save(record);
        return ShelvingRecordVO.from(record);
    }

    /**
     * 上架记录分页检索：按书名模糊（先定位图书再限定记录）、按操作人模糊，上架时间倒序；
     * 返回记录含书名与 ISBN，供上架记录页展示与考核。
     */
    public PageResult<ShelvingRecordVO> records(String title, String operator, long page, long size) {
        List<String> bookIds = null;
        if (title != null && !title.isBlank()) {
            bookIds = bookRepository.findByTitleLike(title.trim()).stream().map(Book::getId).toList();
            if (bookIds.isEmpty()) {
                return new PageResult<>(0, List.of());
            }
        }
        var result = shelvingRecordRepository.search(bookIds, operator, page, size);
        return new PageResult<>(result.getTotal(), assembleVOs(result.getData()));
    }

    /**
     * 待审核清单：review_status=PENDING 的上架记录（按上架时间倒序），含书名与 ISBN。
     */
    public List<ShelvingRecordVO> reviews() {
        return assembleVOs(shelvingRecordRepository.findPendingReviews());
    }

    /**
     * 审核通过（事务）：待审核记录置 APPROVED 并记审核人与时间，
     * 图书可借 +本次上架册数、当前位置更新为记录位置。
     */
    @Transactional
    public ShelvingRecordVO approve(String recordId, ShelvingReviewRequest request) {
        ShelvingRecord record = findPendingRecord(recordId);
        Book book = bookRepository.findById(record.getBookId());
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        book.setAvailableCopies(book.getAvailableCopies() + record.getShelvedCount());
        book.setLocation(record.getLocation());
        bookRepository.update(book);

        record.setReviewStatus("APPROVED");
        record.setReviewer(request.getReviewer().trim());
        record.setReviewedAt(LocalDateTime.now());
        shelvingRecordRepository.update(record);
        return ShelvingRecordVO.from(record);
    }

    /**
     * 审核驳回（事务）：待审核记录置 REJECTED 并记审核人 / 时间 / 理由，
     * 本次上架数量回补到图书待上架数量（退回待上架清单）。
     */
    @Transactional
    public ShelvingRecordVO reject(String recordId, ShelvingReviewRequest request) {
        ShelvingRecord record = findPendingRecord(recordId);
        Book book = bookRepository.findById(record.getBookId());
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        book.setPendingShelving(book.getPendingShelving() + record.getShelvedCount());
        bookRepository.update(book);

        record.setReviewStatus("REJECTED");
        record.setReviewer(request.getReviewer().trim());
        record.setReviewedAt(LocalDateTime.now());
        record.setRejectReason(request.getReason() == null ? null : request.getReason().trim());
        shelvingRecordRepository.update(record);
        return ShelvingRecordVO.from(record);
    }

    /** 定位待审核记录：不存在或已审核均拒绝。 */
    private ShelvingRecord findPendingRecord(String recordId) {
        ShelvingRecord record = shelvingRecordRepository.findById(recordId);
        if (record == null) {
            throw new BusinessException("上架记录不存在");
        }
        if (!"PENDING".equals(record.getReviewStatus())) {
            throw new BusinessException("该上架记录已审核");
        }
        return record;
    }

    /** 组装记录 VO：补齐书名与 ISBN（上架记录检索与待审核清单共用）。 */
    private List<ShelvingRecordVO> assembleVOs(List<ShelvingRecord> records) {
        if (records.isEmpty()) {
            return List.of();
        }
        Map<String, Book> books = bookRepository.findByIds(
                        records.stream().map(ShelvingRecord::getBookId).distinct().toList())
                .stream().collect(Collectors.toMap(Book::getId, Function.identity()));
        return records.stream().map(record -> {
            ShelvingRecordVO vo = ShelvingRecordVO.from(record);
            Book book = books.get(record.getBookId());
            if (book != null) {
                vo.setBookTitle(book.getTitle());
                vo.setBookIsbn(book.getIsbn());
            }
            return vo;
        }).toList();
    }

    /**
     * 图书当前位置：取最近一次上架记录的书架位置；尚无记录 = 待上架。
     */
    public BookLocationVO locationOf(String bookId) {
        if (bookRepository.findById(bookId) == null) {
            throw new BusinessException("图书不存在");
        }
        BookLocationVO vo = new BookLocationVO();
        vo.setBookId(bookId);
        ShelvingRecord latest = shelvingRecordRepository.latestByBook(bookId);
        if (latest != null) {
            vo.setLocation(latest.getLocation());
            vo.setShelvedAt(latest.getShelvedAt());
        }
        return vo;
    }
}
