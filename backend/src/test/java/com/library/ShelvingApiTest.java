package com.library;

import com.jayway.jsonpath.JsonPath;
import com.library.entity.Book;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 上架 API 测试（任务 6.4 / 6.5）：
 * 归还后进入待上架清单、上架确认成功（库存恢复 + 记录写入）、
 * 缺少位置或照片拒绝、图书当前位置查询（已上架 / 待上架）。
 */
@AutoConfigureMockMvc
public class ShelvingApiTest extends BaseIntegrationTest {

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

    /** 创建一本图书并完成一次「借出 → 归还」，使其进入待上架状态，返回图书 ID。 */
    private String createReturnedBook() throws Exception {
        Book book = new Book();
        book.setId(idGenerator.nextId());
        book.setIsbn("ISBN-" + idGenerator.nextId());
        book.setTitle("已归还图书");
        book.setAuthor("测试作者");
        book.setCategoryCode("literature");
        book.setTotalCopies(1);
        book.setAvailableCopies(1);
        book.setPendingShelving(0);
        bookRepository.save(book);

        Reader reader = new Reader();
        reader.setId(idGenerator.nextId());
        reader.setCardNo("CARD-" + idGenerator.nextId());
        reader.setName("测试读者");
        reader.setActive(true);
        readerRepository.save(reader);

        String body = mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"readerId": "%s", "bookId": "%s"}
                                """.formatted(reader.getId(), book.getId())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String loanId = JsonPath.read(body, "$.id");

        mockMvc.perform(post("/api/loans/" + loanId + "/return"))
                .andExpect(status().isOk());

        return book.getId();
    }

    /** 归还后图书出现在待上架清单，且可借数量不变。 */
    @Test
    void returnedBookAppearsInPendingList() throws Exception {
        String bookId = createReturnedBook();

        MvcResult result = mockMvc.perform(get("/api/shelvings/pending"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        List<Object> list = JsonPath.read(body, "$[?(@.id == '%s')]".formatted(bookId));
        assertEquals(1, list.size(), "已归还图书应出现在待上架清单");

        Book book = bookRepository.findById(bookId);
        assertEquals(1, book.getPendingShelving());
        assertEquals(0, book.getAvailableCopies(), "归还后可借数量保持不变");
    }

    /** 上架确认成功：待上架 -1、写入待审核上架记录，可借与位置不变。 */
    @Test
    void confirmShelvingSucceeds() throws Exception {
        String bookId = createReturnedBook();

        mockMvc.perform(post("/api/shelvings/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": "%s", "location": "A-01", "photoPath": "uploads/shelving/demo.jpg", "operator": "张三"}
                                """.formatted(bookId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.location").value("A-01"))
                .andExpect(jsonPath("$.shelvedAt").isNotEmpty())
                .andExpect(jsonPath("$.operator").value("张三"))
                .andExpect(jsonPath("$.reviewStatus").value("PENDING"));

        Book book = bookRepository.findById(bookId);
        assertEquals(0, book.getPendingShelving(), "上架确认后待上架数量应 -1");
        assertEquals(0, book.getAvailableCopies(), "上架确认后（待审核）可借数量不变");
        assertEquals(null, book.getLocation(), "审核通过前不更新当前位置");

        // 上架确认后移出待上架清单，进入待审核清单
        String body = mockMvc.perform(get("/api/shelvings/pending"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Object> list = JsonPath.read(body, "$[?(@.id == '%s')]".formatted(bookId));
        assertEquals(0, list.size(), "上架确认后图书应移出待上架清单");

        String reviews = mockMvc.perform(get("/api/shelvings/reviews"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Object> reviewList = JsonPath.read(reviews, "$[?(@.bookId == '%s')]".formatted(bookId));
        assertEquals(1, reviewList.size(), "上架确认后图书应出现在待审核清单");
    }

    /** 缺少位置或照片时上架确认被拒绝。 */
    @Test
    void confirmRejectedWithoutLocationOrPhoto() throws Exception {
        String bookId = createReturnedBook();

        // 缺位置
        mockMvc.perform(post("/api/shelvings/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": "%s", "location": "", "photoPath": "uploads/shelving/demo.jpg", "operator": "张三"}
                                """.formatted(bookId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("书架位置不能为空"));

        // 缺照片
        mockMvc.perform(post("/api/shelvings/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": "%s", "location": "A-01", "photoPath": "", "operator": "张三"}
                                """.formatted(bookId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请上传现场照片"));
    }

    /** 批量上架：一次确认处理多册，待上架/可借按本次数量增减，记录带本次上架册数。 */
    @Test
    void batchShelvingSucceeds() throws Exception {
        // 构造待上架 3 册、可借 0 的图书（新书入库场景）
        Book book = new Book();
        book.setId(idGenerator.nextId());
        book.setIsbn("ISBN-" + idGenerator.nextId());
        book.setTitle("批量上架图书");
        book.setAuthor("测试作者");
        book.setCategoryCode("literature");
        book.setTotalCopies(3);
        book.setAvailableCopies(0);
        book.setPendingShelving(3);
        bookRepository.save(book);

        // 一次上架 2 册
        mockMvc.perform(post("/api/shelvings/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": "%s", "location": "A 区 1 排 01 号", "photoPath": "uploads/shelving/demo.jpg", "operator": "张三", "count": 2}
                                """.formatted(book.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shelvedCount").value(2));

        Book loaded = bookRepository.findById(book.getId());
        assertEquals(1, loaded.getPendingShelving(), "批量上架后待上架数量应扣减 2");
        assertEquals(0, loaded.getAvailableCopies(), "批量上架后（待审核）可借数量不变");
    }

    /** 本次上架数量超过待上架数量时被拒绝。 */
    @Test
    void batchShelvingRejectedWhenCountExceedsPending() throws Exception {
        String bookId = createReturnedBook();

        mockMvc.perform(post("/api/shelvings/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": "%s", "location": "A-01", "photoPath": "uploads/shelving/demo.jpg", "operator": "张三", "count": 5}
                                """.formatted(bookId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("本次上架数量不能超过待上架数量（1）"));
    }

    /** 上架记录检索：上架后记录出现在分页接口（含书名/册数），可按书名与操作人筛选。 */
    @Test
    void shelvingRecordsSearchable() throws Exception {
        String bookId = createReturnedBook();
        mockMvc.perform(post("/api/shelvings/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": "%s", "location": "A-01", "photoPath": "uploads/shelving/demo.jpg", "operator": "张三", "count": 1}
                                """.formatted(bookId)))
                .andExpect(status().isOk());

        // 全量检索：记录按上架时间倒序返回，含书名 / 位置 / 册数 / 操作人
        mockMvc.perform(get("/api/shelvings/records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].bookTitle").value("已归还图书"))
                .andExpect(jsonPath("$.data[0].location").value("A-01"))
                .andExpect(jsonPath("$.data[0].shelvedCount").value(1))
                .andExpect(jsonPath("$.data[0].operator").value("张三"));

        // 按书名筛选：命中与不命中
        mockMvc.perform(get("/api/shelvings/records").param("title", "已归还"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));
        mockMvc.perform(get("/api/shelvings/records").param("title", "不存在的书"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));

        // 按操作人筛选：命中与不命中
        mockMvc.perform(get("/api/shelvings/records").param("operator", "张三"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));
        mockMvc.perform(get("/api/shelvings/records").param("operator", "李四"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    /** 图书当前位置：审核通过返回记录位置，待上架与待审核位置为空。 */
    @Test
    void bookLocationDisplay() throws Exception {
        // 待上架图书（已归还未上架）：位置为空
        String returnedBookId = createReturnedBook();
        mockMvc.perform(get("/api/books/" + returnedBookId + "/location"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookId").value(returnedBookId))
                .andExpect(jsonPath("$.location").isEmpty());

        // 上架确认后（待审核）：位置仍为空
        String body = mockMvc.perform(post("/api/shelvings/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": "%s", "location": "B-02", "photoPath": "uploads/shelving/demo.jpg", "operator": "李四"}
                                """.formatted(returnedBookId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String recordId = JsonPath.read(body, "$.id");
        mockMvc.perform(get("/api/books/" + returnedBookId + "/location"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").isEmpty());

        // 审核通过后：位置为记录的书架位置
        mockMvc.perform(post("/api/shelvings/reviews/" + recordId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reviewer": "审核员"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewStatus").value("APPROVED"));
        mockMvc.perform(get("/api/books/" + returnedBookId + "/location"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("B-02"))
                .andExpect(jsonPath("$.shelvedAt").isNotEmpty());
    }

    /** 图书检索结果展示位置信息（book-catalog spec：检索结果 MUST 展示位置；审核通过后才有位置）。 */
    @Test
    void searchResultShowsLocation() throws Exception {
        String returnedBookId = createReturnedBook();

        String body = mockMvc.perform(post("/api/shelvings/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": "%s", "location": "C-03", "photoPath": "uploads/shelving/demo.jpg", "operator": "王五"}
                                """.formatted(returnedBookId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String recordId = JsonPath.read(body, "$.id");

        mockMvc.perform(post("/api/shelvings/reviews/" + recordId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reviewer": "审核员"}
                                """))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/books").param("title", "已归还图书"))
                .andExpect(status().isOk())
                .andReturn();
        String body2 = result.getResponse().getContentAsString();
        List<Object> list = JsonPath.read(body2, "$.data[?(@.id == '%s')]".formatted(returnedBookId));
        assertEquals(1, list.size());
        assertEquals("C-03", JsonPath.read(body2, "$.data[0].location"));
    }

    /** 审核流程：待审核清单 → 通过恢复可借并记审核信息 → 驳回回补待上架并记理由。 */
    @Test
    void shelvingReviewApproveAndReject() throws Exception {
        // 通过流程：上架确认 → 待审核 → 通过 → 可借 +1、位置更新、记录置 APPROVED
        String bookId = createReturnedBook();
        String confirmBody = mockMvc.perform(post("/api/shelvings/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": "%s", "location": "A-01", "photoPath": "uploads/shelving/demo.jpg", "operator": "张三"}
                                """.formatted(bookId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String recordId = JsonPath.read(confirmBody, "$.id");

        mockMvc.perform(get("/api/shelvings/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(recordId)).exists());

        mockMvc.perform(post("/api/shelvings/reviews/" + recordId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reviewer": "审核员"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewStatus").value("APPROVED"))
                .andExpect(jsonPath("$.reviewer").value("审核员"))
                .andExpect(jsonPath("$.reviewedAt").isNotEmpty());

        Book book = bookRepository.findById(bookId);
        assertEquals(1, book.getAvailableCopies(), "审核通过后可借数量应 +1");
        assertEquals("A-01", book.getLocation(), "审核通过后位置更新为记录位置");

        // 重复审核被拒
        mockMvc.perform(post("/api/shelvings/reviews/" + recordId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reviewer": "审核员"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("该上架记录已审核"));

        // 驳回流程：上架确认 → 驳回 → 待上架回补、记录置 REJECTED 并记理由
        String bookId2 = createReturnedBook();
        String confirmBody2 = mockMvc.perform(post("/api/shelvings/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": "%s", "location": "B-01", "photoPath": "uploads/shelving/demo.jpg", "operator": "李四"}
                                """.formatted(bookId2)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String recordId2 = JsonPath.read(confirmBody2, "$.id");

        mockMvc.perform(post("/api/shelvings/reviews/" + recordId2 + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reviewer": "审核员", "reason": "位置照片不符"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewStatus").value("REJECTED"))
                .andExpect(jsonPath("$.rejectReason").value("位置照片不符"));

        Book book2 = bookRepository.findById(bookId2);
        assertEquals(1, book2.getPendingShelving(), "驳回后待上架数量应回补");
        assertEquals(0, book2.getAvailableCopies(), "驳回后可借数量不变");

        // 驳回后回到待上架清单，且移出待审核清单
        String pending = mockMvc.perform(get("/api/shelvings/pending"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Object> pendingList = JsonPath.read(pending, "$[?(@.id == '%s')]".formatted(bookId2));
        assertEquals(1, pendingList.size(), "驳回后图书应回到待上架清单");
        String reviews = mockMvc.perform(get("/api/shelvings/reviews"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Object> reviewList = JsonPath.read(reviews, "$[?(@.id == '%s')]".formatted(recordId2));
        assertEquals(0, reviewList.size(), "驳回后记录应移出待审核清单");
    }
}
