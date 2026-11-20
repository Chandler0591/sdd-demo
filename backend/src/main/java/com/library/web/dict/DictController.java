package com.library.web.dict;

import com.library.dto.dict.DictCreateRequest;
import com.library.dto.dict.DictUpdateRequest;
import com.library.dto.dict.DictVO;
import com.library.service.dict.DictService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统字典接口：树形查询、按 pcode 查询子树、新增、编辑。
 */
@RestController
@RequestMapping("/api/dicts")
public class DictController {

    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    /** 树形查询全部字典项 */
    @GetMapping
    public List<DictVO> tree() {
        return dictService.tree();
    }

    /** 按父编码查询子节点（业务表单下拉） */
    @GetMapping("/children")
    public List<DictVO> children(@RequestParam("pcode") String pcode) {
        return dictService.children(pcode);
    }

    /** 新增字典项 */
    @PostMapping
    public DictVO create(@Valid @RequestBody DictCreateRequest request) {
        return dictService.create(request);
    }

    /** 编辑字典项（code 只读） */
    @PutMapping("/{id}")
    public DictVO update(@PathVariable("id") String id, @Valid @RequestBody DictUpdateRequest request) {
        return dictService.update(id, request);
    }
}
