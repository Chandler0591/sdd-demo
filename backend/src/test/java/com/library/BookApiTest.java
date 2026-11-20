package com.library;

import com.jayway.jsonpath.JsonPath;
import com.library.entity.Loan;
import com.library.entity.Reader;
import com.library.id.SnowflakeIdGenerator;
import com.library.repository.LoanRepository;
import com.library.repository.ReaderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 图书编目 API 测试（任务 4.1 / 4.2）：
 * 录入成功、重复 ISBN 累加确认、修改后检索返回新值、分类取自字典、
 * 删除有未归还借阅被拒、删除无未归还借阅成功。
 */
@AutoConfigureMockMvc
public class BookApiTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private ReaderRepository readerRepository;

    @Autowired
    private SnowflakeIdGenerator idGenerator;

    /** 执行字典种子脚本（分类下拉选项依赖），按 UTF-8 读取避免中文乱码。 */
    private void runSeedScript() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection,
                    new EncodedResource(new FileSystemResource("db/seed-dict.sql"), StandardCharsets.UTF_8));
        }
    }

    /** 录入一本图书并返回其雪花 ID。 */
    private String createBook(String isbn, String title, int copies) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isbn": "%s", "title": "%s", "author": "测试作者", "categoryCode": "literature", "totalCopies": %d}
                                """.formatted(isbn, title, copies)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    /** 录入成功：返回雪花 ID，新书进入待上架（可借 0、待上架 = 馆藏总量），分类名取自字典。 */
    @Test
    void createBookSucceeds() throws Exception {
        runSeedScript();
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isbn": "978-7-100-00001", "title": "红楼梦", "author": "曹雪芹", "categoryCode": "literature", "totalCopies": 5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.isbn").value("978-7-100-00001"))
                .andExpect(jsonPath("$.categoryName").value("文学"))
                .andExpect(jsonPath("$.totalCopies").value(5))
                .andExpect(jsonPath("$.availableCopies").value(0))
                .andExpect(jsonPath("$.pendingShelving").value(5));
    }

    /** 分类必须取自系统字典：非法分类拒绝。 */
    @Test
    void createRejectsUnknownCategory() throws Exception {
        runSeedScript();
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isbn": "978-7-100-00002", "title": "测试书", "categoryCode": "nonexistent", "totalCopies": 1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("分类不存在，请从系统字典中选择"));
    }

    /** 重复 ISBN：自动累加副本（用现有馆藏），查重回填与 add-copies 接口保持可用。 */
    @Test
    void duplicateIsbnAutoMergesCopies() throws Exception {
        runSeedScript();
        String id = createBook("978-7-100-00003", "西游记", 3);

        // 重复 ISBN 录入不再拒绝：自动累加副本（总量与待上架同步增加）
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isbn": "978-7-100-00003", "title": "西游记", "categoryCode": "literature", "totalCopies": 2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.totalCopies").value(5))
                .andExpect(jsonPath("$.pendingShelving").value(5));

        // 按 ISBN 查重接口返回已有图书信息（前端回填表单）
        mockMvc.perform(get("/api/books/isbn/978-7-100-00003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("西游记"));

        // add-copies 接口保持可用：总量与待上架同步增加，可借不变（新副本同样需上架）
        mockMvc.perform(post("/api/books/" + id + "/add-copies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"copies": 2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCopies").value(7))
                .andExpect(jsonPath("$.availableCopies").value(0))
                .andExpect(jsonPath("$.pendingShelving").value(7));
    }

    /** ISBN 留空：按馆内规则自动生成 978-7-5566-XXX（三位补零）。 */
    @Test
    void createWithoutIsbnGeneratesOne() throws Exception {
        runSeedScript();
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "自动编号书", "author": "测试", "categoryCode": "literature", "totalCopies": 2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value(org.hamcrest.Matchers.matchesPattern("978-7-5566-\\d{3}")))
                .andExpect(jsonPath("$.pendingShelving").value(2));
    }

    /** ISBN 留空但四要素与馆内现有书一致：自动复用其 ISBN 累加副本（后端兑底）。 */
    @Test
    void createWithoutIsbnReusesSameBook() throws Exception {
        runSeedScript();
        String id = createBook("978-7-100-00011", "同一本书", 1);
        // 补齐出版社与年份，与后续录入的四要素保持一致
        mockMvc.perform(put("/api/books/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "同一本书", "author": "测试作者", "publisher": "同一出版社", "publishYear": 2018, "categoryCode": "literature"}
                                """))
                .andExpect(status().isOk());

        // 留空 ISBN 录入同一本书：复用现有 ISBN 并累加副本
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "同一本书", "author": "测试作者", "publisher": "同一出版社", "publishYear": 2018, "categoryCode": "literature", "totalCopies": 2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.isbn").value("978-7-100-00011"))
                .andExpect(jsonPath("$.totalCopies").value(3));
    }

    /** 自动生成接口：返回馆内规则格式的下一个 ISBN。 */
    @Test
    void nextIsbnEndpointReturnsPrefixFormat() throws Exception {
        mockMvc.perform(get("/api/books/isbn/next"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value(org.hamcrest.Matchers.matchesPattern("978-7-5566-\\d{3}")));
    }

    /** 同一本书判据：书名 + 出版社 + 作者 + 年份全部一致才命中。 */
    @Test
    void matchBookRequiresAllFourAttributes() throws Exception {
        runSeedScript();
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isbn": "978-7-100-00009", "title": "四要素书", "author": "某作者", "publisher": "某出版社", "publishYear": 2020, "categoryCode": "literature", "totalCopies": 1}
                                """))
                .andExpect(status().isOk());

        // 四要素一致：命中
        mockMvc.perform(get("/api/books/match")
                        .param("title", "四要素书")
                        .param("publisher", "某出版社")
                        .param("author", "某作者")
                        .param("year", "2020"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value("978-7-100-00009"));

        // 年份不同：不命中
        mockMvc.perform(get("/api/books/match")
                        .param("title", "四要素书")
                        .param("publisher", "某出版社")
                        .param("author", "某作者")
                        .param("year", "2021"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    /** 录入时保存出版社与出版年份，返回 VO 携带。 */
    @Test
    void createSavesPublisherAndYear() throws Exception {
        runSeedScript();
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isbn": "978-7-100-00010", "title": "出版社测试书", "publisher": "人民文学出版社", "publishYear": 2019, "categoryCode": "literature", "totalCopies": 1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publisher").value("人民文学出版社"))
                .andExpect(jsonPath("$.publishYear").value(2019));
    }

    /** 修改图书信息后，检索返回更新后的值。 */
    @Test
    void updateBookThenSearchReturnsNewValue() throws Exception {
        runSeedScript();
        String id = createBook("978-7-100-00004", "三国演义", 2);

        mockMvc.perform(put("/api/books/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "三国演义（修订版）", "author": "罗贯中", "categoryCode": "literature"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("三国演义（修订版）"))
                .andExpect(jsonPath("$.author").value("罗贯中"))
                .andExpect(jsonPath("$.isbn").value("978-7-100-00004"));

        // 按新书名检索能查到更新后的值
        mockMvc.perform(get("/api/books").param("title", "修订版"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].id").value(id))
                .andExpect(jsonPath("$.data[0].title").value("三国演义（修订版）"));
    }

    /** 删除有未归还借阅的图书被拒绝。 */
    @Test
    void deleteRejectedWhenActiveLoanExists() throws Exception {
        runSeedScript();
        String bookId = createBook("978-7-100-00005", "水浒传", 1);

        Reader reader = new Reader();
        reader.setId(idGenerator.nextId());
        reader.setCardNo("R20260001");
        reader.setName("测试读者");
        reader.setActive(true);
        readerRepository.save(reader);

        Loan loan = new Loan();
        loan.setId(idGenerator.nextId());
        loan.setBookId(bookId);
        loan.setReaderId(reader.getId());
        loan.setBorrowedAt(LocalDateTime.now());
        loan.setDueAt(LocalDateTime.now().plusDays(60));
        loanRepository.save(loan);

        mockMvc.perform(delete("/api/books/" + bookId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("该图书仍有未归还借阅记录，请先处理相关借阅"));
    }

    /** 删除无未归还借阅的图书成功，检索不再返回。 */
    @Test
    void deleteSucceedsWhenNoActiveLoan() throws Exception {
        runSeedScript();
        String bookId = createBook("978-7-100-00006", "平凡的世界", 2);

        mockMvc.perform(delete("/api/books/" + bookId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/books").param("isbn", "978-7-100-00006"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    /** 照片识别：multipart 上传返回模拟识别结果，识别不产生馆藏数据。 */
    @Test
    void recognizeBookFromPhoto() throws Exception {
        runSeedScript();
        MockMultipartFile photo = new MockMultipartFile(
                "file", "cover.jpg", "image/jpeg", "fake-image-bytes".getBytes());

        mockMvc.perform(multipart("/api/books/recognize").file(photo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("人工智能导论（模拟识别）"))
                .andExpect(jsonPath("$.author").value("模拟作者"))
                .andExpect(jsonPath("$.isbn").value("978-7-302-00000-1"));

        // 识别不落库：按识别出的 ISBN 检索应无馆藏
        mockMvc.perform(get("/api/books").param("isbn", "978-7-302-00000-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));

        // 工作人员确认并修正识别结果后录入：新书进入待上架清单
        MvcResult created = mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isbn": "978-7-302-00000-1", "title": "人工智能导论", "author": "张三", "categoryCode": "technology", "totalCopies": 2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingShelving").value(2))
                .andReturn();
        String bookId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        MvcResult pending = mockMvc.perform(get("/api/shelvings/pending"))
                .andExpect(status().isOk())
                .andReturn();
        List<Object> pendingList = JsonPath.read(
                pending.getResponse().getContentAsString(), "$[?(@.id == '%s')]".formatted(bookId));
        assertEquals(1, pendingList.size(), "识别后录入的图书应出现在待上架清单");
    }
}
