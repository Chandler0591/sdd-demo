package com.library.dto.reader;

import com.library.entity.Reader;
import lombok.Data;

/**
 * 读者视图对象。
 */
@Data
public class ReaderVO {

    private String id;

    private String cardNo;

    private String name;

    private String phone;

    /** 是否有效（false = 已注销） */
    private Boolean active;

    public static ReaderVO from(Reader reader) {
        ReaderVO vo = new ReaderVO();
        vo.setId(reader.getId());
        vo.setCardNo(reader.getCardNo());
        vo.setName(reader.getName());
        vo.setPhone(reader.getPhone());
        vo.setActive(reader.getActive());
        return vo;
    }
}
