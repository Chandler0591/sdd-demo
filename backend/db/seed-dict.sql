-- ============================================================
-- 字典种子数据（首次部署手工执行）
-- 包含：图书分类（category）树 + 书架位置（shelf-location）四级树（区 / 排 / 号）
-- 用法：mysql -uroot -p --default-character-set=utf8mb4 library < db/seed-dict.sql
-- ============================================================

SET NAMES utf8mb4;

INSERT INTO dict (id, code, name, pcode, sort_order) VALUES
('1', 'category',       '图书分类', NULL,              0),
('2', 'literature',     '文学',     'category',        1),
('3', 'science',        '科学',     'category',        2),
('4', 'technology',     '技术',     'category',        3),
('5', 'history',        '历史',     'category',        4),
('6', 'art',            '艺术',     'category',        5),
('7', 'shelf-location', '书架位置',   NULL,              1),
('8', 'shelf-a',        'A 区',       'shelf-location',  1),
('9', 'shelf-b',        'B 区',       'shelf-location',  2),
('10', 'shelf-a-r1',    '1 排',       'shelf-a',         1),
('11', 'shelf-a-r2',    '2 排',       'shelf-a',         2),
('12', 'shelf-a-r1-1',  '01 号',      'shelf-a-r1',      1),
('13', 'shelf-a-r1-2',  '02 号',      'shelf-a-r1',      2),
('14', 'shelf-b-r1',    '1 排',       'shelf-b',         1),
('15', 'shelf-b-r1-1',  '01 号',      'shelf-b-r1',      1);
