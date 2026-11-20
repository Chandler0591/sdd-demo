package com.library.dto.loan;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 借出请求：为指定读者办理一册图书借出。
 */
@Data
public class BorrowRequest {

    @NotBlank(message = "读者不能为空")
    private String readerId;

    @NotBlank(message = "图书不能为空")
    private String bookId;
}
