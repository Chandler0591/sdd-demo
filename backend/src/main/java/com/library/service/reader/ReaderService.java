package com.library.service.reader;

import cn.hutool.core.util.StrUtil;
import com.easy.query.core.api.pagination.EasyPageResult;
import com.library.dto.common.PageResult;
import com.library.dto.reader.ReaderCreateRequest;
import com.library.dto.reader.ReaderUpdateRequest;
import com.library.dto.reader.ReaderVO;
import com.library.entity.Reader;
import com.library.exception.BusinessException;
import com.library.id.SnowflakeIdGenerator;
import com.library.repository.LoanRepository;
import com.library.repository.ReaderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 读者管理业务：注册、档案编辑、注销前置校验、检索。
 */
@Service
public class ReaderService {

    private final ReaderRepository readerRepository;
    private final LoanRepository loanRepository;
    private final SnowflakeIdGenerator idGenerator;

    public ReaderService(ReaderRepository readerRepository,
                         LoanRepository loanRepository,
                         SnowflakeIdGenerator idGenerator) {
        this.readerRepository = readerRepository;
        this.loanRepository = loanRepository;
        this.idGenerator = idGenerator;
    }

    /**
     * 按借书证号 / 姓名分页检索读者。
     */
    public PageResult<ReaderVO> search(String cardNo, String name, long page, long size) {
        EasyPageResult<Reader> result = readerRepository.search(cardNo, name, page, size);
        List<ReaderVO> vos = result.getData().stream().map(ReaderVO::from).toList();
        return new PageResult<>(result.getTotal(), vos);
    }

    /**
     * 按借书证号查询读者（查重用），无则返回 null。
     */
    public ReaderVO findByCardNo(String cardNo) {
        Reader reader = readerRepository.findByCardNo(cardNo.trim());
        return reader == null ? null : ReaderVO.from(reader);
    }

    /**
     * 注册新读者：借书证号全局唯一校验。
     */
    public ReaderVO create(ReaderCreateRequest request) {
        String cardNo = request.getCardNo().trim();
        if (readerRepository.findByCardNo(cardNo) != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "借书证号已被使用：" + cardNo);
        }
        Reader reader = new Reader();
        reader.setId(idGenerator.nextId());
        reader.setCardNo(cardNo);
        reader.setName(request.getName().trim());
        reader.setPhone(trimToNull(request.getPhone()));
        reader.setActive(true);
        readerRepository.save(reader);
        return ReaderVO.from(reader);
    }

    /**
     * 编辑读者档案：借书证号可修正，但不得与其他读者重复。
     */
    public ReaderVO update(String id, ReaderUpdateRequest request) {
        Reader reader = requireReader(id);
        if (StrUtil.isNotBlank(request.getCardNo())) {
            String newCardNo = request.getCardNo().trim();
            if (!newCardNo.equals(reader.getCardNo())) {
                Reader other = readerRepository.findByCardNo(newCardNo);
                if (other != null) {
                    throw new BusinessException(HttpStatus.CONFLICT, "借书证号已被其他读者使用：" + newCardNo);
                }
                reader.setCardNo(newCardNo);
            }
        }
        reader.setName(request.getName().trim());
        reader.setPhone(trimToNull(request.getPhone()));
        readerRepository.update(reader);
        return ReaderVO.from(reader);
    }

    /**
     * 注销读者：存在未归还借阅时拒绝；注销后无法再办理借阅。
     */
    public ReaderVO deactivate(String id) {
        Reader reader = requireReader(id);
        if (!Boolean.TRUE.equals(reader.getActive())) {
            throw new BusinessException("该读者已注销");
        }
        if (loanRepository.countActiveByReader(id) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "该读者仍有未归还借阅记录，请先归还全部图书");
        }
        reader.setActive(false);
        readerRepository.update(reader);
        return ReaderVO.from(reader);
    }

    private Reader requireReader(String id) {
        Reader reader = readerRepository.findById(id);
        if (reader == null) {
            throw new BusinessException("读者不存在");
        }
        return reader;
    }

    private String trimToNull(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }
}
