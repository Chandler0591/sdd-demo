package com.library.dto.dict;

import com.library.entity.Dict;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 字典项视图对象（树形展示用）。
 */
@Data
public class DictVO {

    private String id;

    private String code;

    private String name;

    private String pcode;

    private Integer sortOrder;

    private List<DictVO> children = new ArrayList<>();

    public static DictVO from(Dict dict) {
        DictVO vo = new DictVO();
        vo.setId(dict.getId());
        vo.setCode(dict.getCode());
        vo.setName(dict.getName());
        vo.setPcode(dict.getPcode());
        vo.setSortOrder(dict.getSortOrder());
        return vo;
    }
}
