package com.library.service.book;

import com.library.dto.book.RecognizedBookInfo;
import com.library.exception.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 书籍识别模拟实现：返回固定识别结果，用于 MVP 阶段跑通「拍照 → 识别 → 录入」流程。
 * 通过配置 library.recognizer.provider=mock 装配（缺省也生效）；
 * 接入真实大模型时新增实现类并将 provider 改为对应值。
 */
@Service
@ConditionalOnProperty(name = "library.recognizer.provider", havingValue = "mock", matchIfMissing = true)
public class MockBookInfoRecognizer implements BookInfoRecognizer {

    @Override
    public RecognizedBookInfo recognize(byte[] photoBytes) {
        if (photoBytes == null || photoBytes.length == 0) {
            throw new BusinessException("无法从照片识别出书籍信息，请重新拍照或手工录入");
        }
        RecognizedBookInfo info = new RecognizedBookInfo();
        info.setTitle("人工智能导论（模拟识别）");
        info.setAuthor("模拟作者");
        info.setPublisher("模拟出版社");
        info.setPublishYear(2026);
        info.setIsbn("978-7-302-00000-1");
        return info;
    }
}
