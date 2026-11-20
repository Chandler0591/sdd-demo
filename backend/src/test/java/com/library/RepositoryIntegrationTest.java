package com.library;

import com.easy.query.api.proxy.client.EasyEntityQuery;
import com.easy.query.core.api.pagination.EasyPageResult;
import com.library.entity.Book;
import com.library.entity.Dict;
import com.library.entity.Loan;
import com.library.entity.Reader;
import com.library.entity.ShelvingRecord;
import com.library.id.SnowflakeIdGenerator;
import com.library.repository.BookRepository;
import com.library.repository.DictRepository;
import com.library.repository.LoanRepository;
import com.library.repository.ReaderRepository;
import com.library.repository.ShelvingRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 任务 2.2 验证：五个数据访问接口的核心查询。
 */
class RepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ReaderRepository readerRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private ShelvingRecordRepository shelvingRecordRepository;

    @Autowired
    private DictRepository dictRepository;

    @Autowired
    private EasyEntityQuery entityQuery;

    @Autowired
    private SnowflakeIdGenerator idGenerator;

    private Book newBook(String isbn, String title, String author, String categoryCode) {
        Book book = new Book();
        book.setId(idGenerator.nextId());
        book.setIsbn(isbn);
        book.setTitle(title);
        book.setAuthor(author);
        book.setCategoryCode(categoryCode);
        book.setTotalCopies(2);
        book.setAvailableCopies(2);
        book.setPendingShelving(0);
        return book;
    }

    private Reader newReader(String cardNo, String name, boolean active) {
        Reader reader = new Reader();
        reader.setId(idGenerator.nextId());
        reader.setCardNo(cardNo);
        reader.setName(name);
        reader.setPhone("13800000000");
        reader.setActive(active);
        return reader;
    }

    @Test
    void bookSearch_shouldFilterByTitleAndCategory() {
        Book b1 = newBook("9787311111111", "Java 编程思想", "Bruce", "tech");
        Book b2 = newBook("9787322222222", "红楼梦", "曹雪芹", "literature");
        bookRepository.save(b1);
        bookRepository.save(b2);

        EasyPageResult<Book> byTitle = bookRepository.search(null, "Java", null, null, null, 1, 10);
        assertEquals(1, byTitle.getTotal());
        assertEquals("Java 编程思想", byTitle.getData().get(0).getTitle());

        EasyPageResult<Book> byCategory = bookRepository.search(null, null, null, "literature", null, 1, 10);
        assertEquals(1, byCategory.getTotal());
        assertEquals("红楼梦", byCategory.getData().get(0).getTitle());

        // keyword：书名或 ISBN 任一模糊命中（小程序查书页）
        EasyPageResult<Book> byKeywordTitle = bookRepository.search(null, null, null, null, "Java 编程", 1, 10);
        assertEquals(1, byKeywordTitle.getTotal());
        EasyPageResult<Book> byKeywordIsbn = bookRepository.search(null, null, null, null, "978731111", 1, 10);
        assertEquals(1, byKeywordIsbn.getTotal());
        assertEquals("Java 编程思想", byKeywordIsbn.getData().get(0).getTitle());
    }

    @Test
    void readerSearch_shouldFilterByCardNoAndName() {
        readerRepository.save(newReader("R001", "张三", true));
        readerRepository.save(newReader("R002", "李四", true));

        assertEquals(1, readerRepository.search("R001", null, 1, 10).getTotal());
        assertEquals(1, readerRepository.search(null, "李四", 1, 10).getTotal());
    }

    @Test
    void loanQueries_shouldCountActiveAndListByReader() {
        Reader reader = newReader("R003", "王五", true);
        readerRepository.save(reader);
        Book book = newBook("9787333333333", "测试图书", "作者", "tech");
        bookRepository.save(book);

        // 一条在借
        Loan active = new Loan();
        active.setId(idGenerator.nextId());
        active.setBookId(book.getId());
        active.setReaderId(reader.getId());
        active.setBorrowedAt(LocalDateTime.now().minusDays(1));
        active.setDueAt(LocalDateTime.now().plusDays(59));
        loanRepository.save(active);

        // 一条已归还
        Loan returned = new Loan();
        returned.setId(idGenerator.nextId());
        returned.setBookId(book.getId());
        returned.setReaderId(reader.getId());
        returned.setBorrowedAt(LocalDateTime.now().minusDays(10));
        returned.setDueAt(LocalDateTime.now().minusDays(1));
        returned.setReturnedAt(LocalDateTime.now().minusDays(2));
        loanRepository.save(returned);

        assertEquals(1, loanRepository.countActiveByReader(reader.getId()));
        assertEquals(1, loanRepository.activeLoansByReader(reader.getId()).size());
        assertTrue(loanRepository.existsActiveByBook(book.getId()));
    }

    @Test
    void shelvingRecord_latestByBook_shouldReturnMostRecent() {
        Book book = newBook("9787344444444", "上架测试", "作者", "tech");
        bookRepository.save(book);

        ShelvingRecord first = new ShelvingRecord();
        first.setId(idGenerator.nextId());
        first.setBookId(book.getId());
        first.setLocation("A区-1排-1层");
        first.setPhotoPath("uploads/shelving/a.jpg");
        first.setShelvedAt(LocalDateTime.now().minusDays(3));
        first.setOperator("张三");
        first.setReviewStatus("APPROVED");
        shelvingRecordRepository.save(first);

        ShelvingRecord second = new ShelvingRecord();
        second.setId(idGenerator.nextId());
        second.setBookId(book.getId());
        second.setLocation("B区-2排-3层");
        second.setPhotoPath("uploads/shelving/b.jpg");
        second.setShelvedAt(LocalDateTime.now());
        second.setOperator("李四");
        second.setReviewStatus("APPROVED");
        shelvingRecordRepository.save(second);

        ShelvingRecord latest = shelvingRecordRepository.latestByBook(book.getId());
        assertNotNull(latest);
        assertEquals("B区-2排-3层", latest.getLocation());

        // 待审核记录不参与最近已上架位置计算
        ShelvingRecord pending = new ShelvingRecord();
        pending.setId(idGenerator.nextId());
        pending.setBookId(book.getId());
        pending.setLocation("C区-9排");
        pending.setPhotoPath("uploads/shelving/c.jpg");
        pending.setShelvedAt(LocalDateTime.now().plusHours(1));
        pending.setOperator("王五");
        pending.setReviewStatus("PENDING");
        shelvingRecordRepository.save(pending);

        ShelvingRecord latestAfterPending = shelvingRecordRepository.latestByBook(book.getId());
        assertEquals("B区-2排-3层", latestAfterPending.getLocation(), "待审核记录不应覆盖已审核位置");
    }

    @Test
    void dictQueries_shouldReturnChildrenByPcode() {
        Dict root = new Dict();
        root.setId(idGenerator.nextId());
        root.setCode("category");
        root.setName("图书分类");
        root.setSortOrder(0);
        dictRepository.save(root);

        Dict child1 = new Dict();
        child1.setId(idGenerator.nextId());
        child1.setCode("literature");
        child1.setName("文学");
        child1.setPcode("category");
        child1.setSortOrder(2);
        dictRepository.save(child1);

        Dict child2 = new Dict();
        child2.setId(idGenerator.nextId());
        child2.setCode("tech");
        child2.setName("科技");
        child2.setPcode("category");
        child2.setSortOrder(1);
        dictRepository.save(child2);

        var children = dictRepository.childrenByPcode("category");
        assertEquals(2, children.size());
        // 按 sort_order 升序：科技在前
        assertEquals("tech", children.get(0).getCode());
        assertEquals(3, dictRepository.findAllOrdered().size());
        assertEquals("文学", entityQuery.queryable(Dict.class).where(d -> d.code().eq("literature")).firstOrNull().getName());
    }
}
