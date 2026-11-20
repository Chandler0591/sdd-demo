package com.library;

import com.jayway.jsonpath.JsonPath;
import com.library.entity.Book;
import com.library.entity.Loan;
import com.library.entity.Reader;
import com.library.id.SnowflakeIdGenerator;
import com.library.repository.BookRepository;
import com.library.repository.LoanRepository;
import com.library.repository.ReaderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 借阅流通 API 测试（任务 6.1 / 6.2 / 6.3）：
 * 借出成功、库存不足拒绝、达上限拒绝、已注销读者拒绝、
 * 归还成功与重复归还拒绝、在借列表与超期标记。
 */
@AutoConfigureMockMvc
public class LoanApiTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ReaderRepository readerRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private SnowflakeIdGenerator idGenerator;

    private String createBook(int copies) {
        Book book = new Book();
        book.setId(idGenerator.nextId());
        book.setIsbn("ISBN-" + idGenerator.nextId());
        book.setTitle("测试图书");
        book.setAuthor("测试作者");
        book.setCategoryCode("literature");
        book.setTotalCopies(copies);
        book.setAvailableCopies(copies);
        book.setPendingShelving(0);
        bookRepository.save(book);
        return book.getId();
    }

    private String createReader(boolean active) {
        Reader reader = new Reader();
        reader.setId(idGenerator.nextId());
        reader.setCardNo("CARD-" + idGenerator.nextId());
        reader.setName("测试读者");
        reader.setActive(active);
        readerRepository.save(reader);
        return reader.getId();
    }

    private String borrow(String readerId, String bookId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"readerId": "%s", "bookId": "%s"}
                                """.formatted(readerId, bookId)))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    /** 借出成功：创建借阅记录，借期默认 60 天，可借库存减 1。 */
    @Test
    void borrowSucceeds() throws Exception {
        String bookId = createBook(2);
        String readerId = createReader(true);

        MvcResult result = mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"readerId": "%s", "bookId": "%s"}
                                """.formatted(readerId, bookId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.returnedAt").isEmpty())
                .andExpect(jsonPath("$.bookTitle").value("测试图书"))
                .andExpect(jsonPath("$.readerName").value("测试读者"))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        LocalDateTime borrowedAt = LocalDateTime.parse(JsonPath.read(body, "$.borrowedAt"));
        LocalDateTime dueAt = LocalDateTime.parse(JsonPath.read(body, "$.dueAt"));
        assertEquals(60, Duration.between(borrowedAt, dueAt).toDays(), "借期应为配置的 60 天");

        Book book = bookRepository.findById(bookId);
        assertEquals(1, book.getAvailableCopies(), "借出后可借库存应减 1");
    }

    /** 图书无可借库存时借出失败。 */
    @Test
    void borrowFailsWhenNoStock() throws Exception {
        String bookId = createBook(1);
        String readerId = createReader(true);
        borrow(readerId, bookId);

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"readerId": "%s", "bookId": "%s"}
                                """.formatted(readerId, bookId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("该图书暂无可借库存"));
    }

    /** 读者在借数量达上限（10 册）时借出失败。 */
    @Test
    void borrowFailsWhenLimitReached() throws Exception {
        String bookId = createBook(2);
        String readerId = createReader(true);

        // 直接构造 10 条在借记录，模拟已达上限
        for (int i = 0; i < 10; i++) {
            Loan loan = new Loan();
            loan.setId(idGenerator.nextId());
            loan.setBookId(bookId);
            loan.setReaderId(readerId);
            loan.setBorrowedAt(LocalDateTime.now().minusDays(1));
            loan.setDueAt(LocalDateTime.now().plusDays(30));
            loanRepository.save(loan);
        }

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"readerId": "%s", "bookId": "%s"}
                                """.formatted(readerId, bookId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("该读者在借数量已达上限（10 册），无法继续借阅"));
    }

    /** 为已注销读者借出失败。 */
    @Test
    void borrowFailsForDeactivatedReader() throws Exception {
        String bookId = createBook(1);
        String readerId = createReader(false);

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"readerId": "%s", "bookId": "%s"}
                                """.formatted(readerId, bookId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("该读者已注销，无法办理借阅"));
    }

    /** 归还成功：记录归还日期，图书转入待上架，可借数量不变。 */
    @Test
    void returnSucceeds() throws Exception {
        String bookId = createBook(2);
        String readerId = createReader(true);
        String loanId = borrow(readerId, bookId);

        mockMvc.perform(post("/api/loans/" + loanId + "/return"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnedAt").isNotEmpty())
                .andExpect(jsonPath("$.overdue").value(false));

        Book book = bookRepository.findById(bookId);
        assertEquals(1, book.getPendingShelving(), "归还后待上架数量应 +1");
        assertEquals(1, book.getAvailableCopies(), "归还后可借数量保持不变");
    }

    /** 重复归还被拒绝。 */
    @Test
    void returnRejectedTwice() throws Exception {
        String bookId = createBook(1);
        String readerId = createReader(true);
        String loanId = borrow(readerId, bookId);

        mockMvc.perform(post("/api/loans/" + loanId + "/return"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/loans/" + loanId + "/return"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("该借阅记录已归还，请勿重复操作"));
    }

    /** 按读者查询在借记录：返回图书信息并正确标记超期。 */
    @Test
    void activeLoansWithOverdueFlag() throws Exception {
        String readerId = createReader(true);

        // 正常在借记录（借出时间较早）
        String book1Id = createBook(1);
        Loan normal = new Loan();
        normal.setId(idGenerator.nextId());
        normal.setBookId(book1Id);
        normal.setReaderId(readerId);
        normal.setBorrowedAt(LocalDateTime.now().minusHours(1));
        normal.setDueAt(LocalDateTime.now().plusDays(30));
        loanRepository.save(normal);

        // 超期在借记录（应还日期早于当前，借出时间最近 → 排在列表首位）
        String book2Id = createBook(1);
        Loan overdue = new Loan();
        overdue.setId(idGenerator.nextId());
        overdue.setBookId(book2Id);
        overdue.setReaderId(readerId);
        overdue.setBorrowedAt(LocalDateTime.now());
        overdue.setDueAt(LocalDateTime.now().minusDays(1));
        loanRepository.save(overdue);

        MvcResult result = mockMvc.perform(get("/api/loans/reader/" + readerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        List<Object> loans = JsonPath.read(body, "$");
        assertNotNull(loans);

        // 列表按借出时间倒序：首位为超期记录，末位为正常记录
        assertTrue((Boolean) JsonPath.read(body, "$[0].overdue"), "应还日期早于当前应标记为超期");
        assertEquals(false, JsonPath.read(body, "$[1].overdue"));
        assertEquals(overdue.getId(), JsonPath.read(body, "$[0].id"));
    }
}
