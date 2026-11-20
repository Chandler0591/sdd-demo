package com.library.service.dict;

import cn.hutool.core.util.StrUtil;
import com.library.dto.dict.DictCreateRequest;
import com.library.dto.dict.DictUpdateRequest;
import com.library.dto.dict.DictVO;
import com.library.entity.Dict;
import com.library.exception.BusinessException;
import com.library.id.SnowflakeIdGenerator;
import com.library.repository.DictRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统字典业务：树形查询、按 pcode 查询子树、新增与编辑（code 创建后只读）。
 */
@Service
public class DictService {

    private final DictRepository dictRepository;
    private final SnowflakeIdGenerator idGenerator;

    public DictService(DictRepository dictRepository, SnowflakeIdGenerator idGenerator) {
        this.dictRepository = dictRepository;
        this.idGenerator = idGenerator;
    }

    /**
     * 树形查询：返回以顶层项（pcode 为空或父级不存在）为根的嵌套结构。
     */
    public List<DictVO> tree() {
        List<Dict> all = dictRepository.findAllOrdered();
        Map<String, DictVO> byCode = new LinkedHashMap<>();
        for (Dict dict : all) {
            byCode.put(dict.getCode(), DictVO.from(dict));
        }
        List<DictVO> roots = new ArrayList<>();
        for (DictVO vo : byCode.values()) {
            if (StrUtil.isBlank(vo.getPcode()) || !byCode.containsKey(vo.getPcode())) {
                roots.add(vo);
            } else {
                byCode.get(vo.getPcode()).getChildren().add(vo);
            }
        }
        return roots;
    }

    /**
     * 按父编码查询直接子节点（业务表单下拉使用）。
     */
    public List<DictVO> children(String pcode) {
        return dictRepository.childrenByPcode(pcode).stream().map(DictVO::from).toList();
    }

    /**
     * 新增字典项：code 全局唯一，父级存在性校验。
     */
    public DictVO create(DictCreateRequest request) {
        String code = request.getCode().trim();
        if (dictRepository.findByCode(code) != null) {
            throw new BusinessException("字典编码已存在：" + code);
        }
        String pcode = normalizePcode(request.getPcode());
        if (pcode != null && dictRepository.findByCode(pcode) == null) {
            throw new BusinessException("父级字典不存在：" + pcode);
        }
        Dict dict = new Dict();
        dict.setId(idGenerator.nextId());
        dict.setCode(code);
        dict.setName(request.getName().trim());
        dict.setPcode(pcode);
        dict.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        dictRepository.save(dict);
        return DictVO.from(dict);
    }

    /**
     * 编辑字典项：code 只读，仅修改名称 / 父级 / 排序；父级校验与防环。
     */
    public DictVO update(String id, DictUpdateRequest request) {
        Dict dict = dictRepository.findById(id);
        if (dict == null) {
            throw new BusinessException("字典项不存在");
        }
        String pcode = normalizePcode(request.getPcode());
        if (pcode != null) {
            if (pcode.equals(dict.getCode())) {
                throw new BusinessException("父级不能是字典项自身");
            }
            if (dictRepository.findByCode(pcode) == null) {
                throw new BusinessException("父级字典不存在：" + pcode);
            }
            // 防环：新父级的祖先链上不允许出现自身
            Dict ancestor = dictRepository.findByCode(pcode);
            while (ancestor != null && StrUtil.isNotBlank(ancestor.getPcode())) {
                if (ancestor.getPcode().equals(dict.getCode())) {
                    throw new BusinessException("不能将字典项移动到自己的子节点下");
                }
                ancestor = dictRepository.findByCode(ancestor.getPcode());
            }
        }
        dict.setName(request.getName().trim());
        dict.setPcode(pcode);
        if (request.getSortOrder() != null) {
            dict.setSortOrder(request.getSortOrder());
        }
        dictRepository.update(dict);
        return DictVO.from(dict);
    }

    private String normalizePcode(String pcode) {
        return StrUtil.isBlank(pcode) ? null : pcode.trim();
    }
}
