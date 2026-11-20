package com.library.service.book;

import com.library.dto.book.RecognizedBookInfo;
import com.library.exception.BusinessException;

/**
 * 书籍照片信息识别接口：接收照片字节，返回识别出的书名 / 作者 / ISBN。
 * MVP 提供模拟实现，后续接入真实大模型时新增实现类并切换配置即可。
 */
public interface BookInfoRecognizer {

    /**
     * 识别书籍照片。
     *
     * @param photoBytes 照片字节内容
     * @return 识别结果（书名 / 作者 / ISBN）
     * @throws BusinessException 照片无法识别时抛出
     */
    RecognizedBookInfo recognize(byte[] photoBytes);
}
