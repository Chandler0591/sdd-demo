-- ============================================================
-- 图书管理系统建表脚本（首次部署手工执行，可重复执行）
-- 用法：mysql -uroot -p library < db/schema.sql
-- ============================================================

DROP TABLE IF EXISTS shelving_record;
DROP TABLE IF EXISTS loan;
DROP TABLE IF EXISTS book;
DROP TABLE IF EXISTS reader;
DROP TABLE IF EXISTS dict;

-- 图书表
CREATE TABLE book (
    id               VARCHAR(19)  NOT NULL COMMENT '雪花 ID 主键',
    isbn             VARCHAR(32)  NOT NULL COMMENT 'ISBN',
    title            VARCHAR(200) NOT NULL COMMENT '书名',
    author           VARCHAR(100) NULL COMMENT '作者',
    publisher        VARCHAR(100) NULL COMMENT '出版社',
    publish_year     INT          NULL COMMENT '出版年份',
    category_code    VARCHAR(64)  NULL COMMENT '分类字典 code',
    total_copies     INT          NOT NULL DEFAULT 0 COMMENT '馆藏总量',
    available_copies INT          NOT NULL DEFAULT 0 COMMENT '可借数量（已上架）',
    pending_shelving INT          NOT NULL DEFAULT 0 COMMENT '待上架数量（已归还）',
    location         VARCHAR(100) NULL COMMENT '当前位置（最近一次上架记录）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_book_isbn (isbn)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '图书';

-- 读者表
CREATE TABLE reader (
    id      VARCHAR(19) NOT NULL COMMENT '雪花 ID 主键',
    card_no VARCHAR(32) NOT NULL COMMENT '借书证号',
    name    VARCHAR(50) NOT NULL COMMENT '姓名',
    phone   VARCHAR(20) NULL COMMENT '联系方式',
    active  TINYINT(1)  NOT NULL DEFAULT 1 COMMENT '是否有效（1=正常 0=已注销）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_reader_card_no (card_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '读者';

-- 借阅记录表
CREATE TABLE loan (
    id          VARCHAR(19) NOT NULL COMMENT '雪花 ID 主键',
    book_id     VARCHAR(19) NOT NULL COMMENT '图书 ID',
    reader_id   VARCHAR(19) NOT NULL COMMENT '读者 ID',
    borrowed_at DATETIME    NOT NULL COMMENT '借出时间',
    due_at      DATETIME    NOT NULL COMMENT '应还时间',
    returned_at DATETIME    NULL COMMENT '归还时间（null=未归还）',
    PRIMARY KEY (id),
    KEY idx_loan_reader (reader_id),
    KEY idx_loan_book (book_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '借阅记录';

-- 上架记录表
CREATE TABLE shelving_record (
    id            VARCHAR(19)  NOT NULL COMMENT '雪花 ID 主键',
    book_id       VARCHAR(19)  NOT NULL COMMENT '图书 ID',
    location      VARCHAR(100) NOT NULL COMMENT '书架位置',
    photo_path    VARCHAR(255) NOT NULL COMMENT '现场照片存储路径',
    shelved_at    DATETIME     NOT NULL COMMENT '上架时间（服务端生成）',
    operator      VARCHAR(50)  NOT NULL COMMENT '操作人姓名',
    shelved_count INT          NOT NULL DEFAULT 1 COMMENT '本次上架册数',
    review_status VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '审核状态（PENDING 待审核 / APPROVED 已通过 / REJECTED 已驳回）',
    reviewer      VARCHAR(50)  NULL COMMENT '审核人姓名',
    reviewed_at   DATETIME     NULL COMMENT '审核时间（服务端生成）',
    reject_reason VARCHAR(200) NULL COMMENT '驳回理由',
    PRIMARY KEY (id),
    KEY idx_shelving_book (book_id),
    KEY idx_shelving_review (review_status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '上架记录';

-- 系统字典表
CREATE TABLE dict (
    id         VARCHAR(19)  NOT NULL COMMENT '雪花 ID 主键',
    code       VARCHAR(64)  NOT NULL COMMENT '字典编码（全局唯一，创建后不可修改）',
    name       VARCHAR(100) NOT NULL COMMENT '字典名称',
    pcode      VARCHAR(64)  NULL COMMENT '父字典编码（空=顶层）',
    sort_order INT          NOT NULL DEFAULT 0 COMMENT '同级排序',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统字典';
