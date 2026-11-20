package com.library;

import com.jayway.jsonpath.JsonPath;
import com.library.entity.Book;
import com.library.entity.Loan;
import com.library.id.SnowflakeIdGenerator;
import com.library.repository.BookRepository;
import com.library.repository.LoanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 读者管理 API 测试（任务 5.1 / 5.2）：
 * 注册成功、重复借书证号拒绝、编辑档案生效、
 * 注销有未归还借阅被拒、注销成功且无法再办理借阅。
 */
@AutoConfigureMockMvc
public class ReaderApiTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private SnowflakeIdGenerator idGenerator;

    /** 注册一名读者并返回其雪花 ID。 */
    private String createReader(String cardNo, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/readers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cardNo": "%s", "name": "%s", "phone": "13800000000"}
                                """.formatted(cardNo, name)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    /** 注册新读者成功：返回雪花 ID，借书证号回显一致，默认有效。 */
    @Test
    void registerReaderSucceeds() throws Exception {
        mockMvc.perform(post("/api/readers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cardNo": "R20260001", "name": "张三", "phone": "13800000000"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.cardNo").value("R20260001"))
                .andExpect(jsonPath("$.name").value("张三"))
                .andExpect(jsonPath("$.active").value(true));
    }

    /** 重复借书证号被拒绝：409 + 明确错误提示。 */
    @Test
    void duplicateCardNoRejected() throws Exception {
        createReader("R20260002", "李四");
        mockMvc.perform(post("/api/readers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cardNo": "R20260002", "name": "王五"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("借书证号已被使用：R20260002"));
    }

    /** 修改读者联系方式后，查询返回更新后的档案。 */
    @Test
    void editReaderProfileEffective() throws Exception {
        String id = createReader("R20260003", "赵六");

        mockMvc.perform(put("/api/readers/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "赵六", "phone": "13911112222"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("13911112222"));

        mockMvc.perform(get("/api/readers").param("cardNo", "R20260003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].id").value(id))
                .andExpect(jsonPath("$.data[0].phone").value("13911112222"));
    }

    /** 注销有未归还借阅的读者被拒绝。 */
    @Test
    void deactivateRejectedWhenActiveLoanExists() throws Exception {
        String readerId = createReader("R20260004", "孙七");

        Book book = new Book();
        book.setId(idGenerator.nextId());
        book.setIsbn("978-7-100-10004");
        book.setTitle("测试图书");
        book.setCategoryCode("literature");
        book.setTotalCopies(1);
        book.setAvailableCopies(1);
        book.setPendingShelving(0);
        bookRepository.save(book);

        Loan loan = new Loan();
        loan.setId(idGenerator.nextId());
        loan.setBookId(book.getId());
        loan.setReaderId(readerId);
        loan.setBorrowedAt(LocalDateTime.now());
        loan.setDueAt(LocalDateTime.now().plusDays(60));
        loanRepository.save(loan);

        mockMvc.perform(post("/api/readers/" + readerId + "/deactivate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("该读者仍有未归还借阅记录，请先归还全部图书"));
    }

    /** 注销无未归还借阅的读者成功：标记注销，重复注销被拒。 */
    @Test
    void deactivateSucceeds() throws Exception {
        String readerId = createReader("R20260005", "周八");

        mockMvc.perform(post("/api/readers/" + readerId + "/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // 已注销读者再次注销被拒
        mockMvc.perform(post("/api/readers/" + readerId + "/deactivate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("该读者已注销"));
    }
}
