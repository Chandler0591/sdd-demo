package com.library.dto.reader;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 读者注册请求。
 */
@Data
public class ReaderCreateRequest {

    @NotBlank(message = "借书证号不能为空")
    @Size(max = 32, message = "借书证号过长")
    private String cardNo;

    @NotBlank(message = "读者姓名不能为空")
    @Size(max = 50, message = "读者姓名过长")
    private String name;

    @Size(max = 20, message = "联系方式过长")
    private String phone;
}
