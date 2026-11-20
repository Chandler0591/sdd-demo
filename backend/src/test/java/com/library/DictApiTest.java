package com.library;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 系统字典 API 测试（任务 3.1 / 3.2）：
 * 新增成功、重复 code 拒绝、编辑名称生效且 code 只读、防环、
 * 树形查询、按 pcode 查询子树、种子脚本执行后分类字典可查询。
 */
@AutoConfigureMockMvc
public class DictApiTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /** 执行种子脚本（模拟首次部署手动执行 db/seed-dict.sql），按 UTF-8 读取避免中文乱码。 */
    private void runSeedScript() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection,
                    new EncodedResource(new FileSystemResource("db/seed-dict.sql"), StandardCharsets.UTF_8));
        }
    }

    /** 新增字典项成功：返回雪花 ID，编码与名称回显一致。 */
    @Test
    void createDictSucceeds() throws Exception {
        mockMvc.perform(post("/api/dicts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "category", "name": "图书分类", "pcode": null, "sortOrder": 0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.code").value("category"))
                .andExpect(jsonPath("$.name").value("图书分类"));
    }

    /** 重复 code 被拒绝：400 + 明确错误信息。 */
    @Test
    void duplicateCodeRejected() throws Exception {
        runSeedScript();
        mockMvc.perform(post("/api/dicts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "literature", "name": "重复文学"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("字典编码已存在：literature"));
    }

    /** 编辑名称生效，code 保持只读不变。 */
    @Test
    void updateNameEffectiveAndCodeImmutable() throws Exception {
        runSeedScript();
        mockMvc.perform(put("/api/dicts/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "文学（更新）", "pcode": "category", "sortOrder": 9}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("2"))
                .andExpect(jsonPath("$.code").value("literature"))
                .andExpect(jsonPath("$.name").value("文学（更新）"))
                .andExpect(jsonPath("$.sortOrder").value(9));
    }

    /** 防环：不能把顶层项移动到自己的子节点下。 */
    @Test
    void cannotMoveDictUnderItsChild() throws Exception {
        runSeedScript();
        mockMvc.perform(put("/api/dicts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "图书分类", "pcode": "literature"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("不能将字典项移动到自己的子节点下"));
    }

    /** 种子脚本执行后：树形查询返回父子挂接的完整结构。 */
    @Test
    void seedScriptEnablesTreeQuery() throws Exception {
        runSeedScript();
        mockMvc.perform(get("/api/dicts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("category"))
                .andExpect(jsonPath("$[0].children.length()").value(5))
                .andExpect(jsonPath("$[0].children[0].code").value("literature"))
                .andExpect(jsonPath("$[0].children[4].code").value("art"));
    }

    /** 种子脚本执行后：业务表单可按 pcode 获取分类项。 */
    @Test
    void childrenByPcodeForBusinessForms() throws Exception {
        runSeedScript();
        mockMvc.perform(get("/api/dicts/children").param("pcode", "category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].code").value("literature"))
                .andExpect(jsonPath("$[1].code").value("science"))
                .andExpect(jsonPath("$[4].code").value("art"));
    }
}
