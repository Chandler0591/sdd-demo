# Spec Delta

## Purpose

图书编目管理能力，支持管理员维护图书馆馆藏图书的元数据与库存数量，并提供多条件检索，是借阅流通业务的数据基础。

## ADDED Requirements

### Requirement: Register book in catalog
系统 SHALL 允许管理员录入新图书，登记 ISBN、书名、作者与分类并设置馆藏库存数量；ISBN 在同一馆藏中 MUST 唯一，分类 MUST 从系统字典中选择。

#### Scenario: 录入新图书成功
- **WHEN** 管理员提交包含 ISBN、书名、作者、分类与库存数量的图书录入请求
- **THEN** 系统保存该图书并返回图书唯一标识与保存结果

#### Scenario: 分类取自系统字典
- **WHEN** 管理员录入图书并选择分类
- **THEN** 分类选项来自系统字典，不允许自由输入

#### Scenario: 重复 ISBN 录入在原有书籍上累加
- **WHEN** 管理员录入的 ISBN 与馆藏中已有图书相同
- **THEN** 系统提示馆藏中已有图书，是否继续 要么管理员录入错误，不继续；要么继续，读取原有图书信息回填录入表单，确定已有图书做数量叠加

### Requirement: Edit book information
系统 SHALL 允许管理员修改已有图书的书名、作者、分类等元数据。

#### Scenario: 修改图书信息
- **WHEN** 管理员提交对某本图书元数据的修改
- **THEN** 系统保存修改，后续检索返回更新后的信息

### Requirement: Remove book from catalog
系统 SHALL 允许管理员删除图书；当该图书仍有未归还的借阅记录时，系统 MUST 拒绝删除。

#### Scenario: 删除无未归还借阅的图书
- **WHEN** 管理员删除一本没有未归还借阅记录的图书
- **THEN** 该图书从馆藏中移除

#### Scenario: 删除有未归还借阅的图书被拒绝
- **WHEN** 管理员删除一本仍有未归还借阅记录的图书
- **THEN** 系统拒绝删除并提示先处理相关借阅记录

### Requirement: Search books
系统 SHALL 支持按 ISBN、书名、作者或分类中的任一条件检索图书，返回匹配的图书列表及各自的可借库存。

#### Scenario: 按书名检索
- **WHEN** 管理员以部分书名作为关键词检索
- **THEN** 系统返回书名匹配的图书列表

#### Scenario: 检索显示可借库存
- **WHEN** 检索返回图书列表
- **THEN** 每本图书均展示其当前可借库存数量

### Requirement: Track available stock
系统 SHALL 跟踪每本图书的馆藏总量与当前可借数量，可借数量随借出与上架确认自动增减。

#### Scenario: 借出后库存减少
- **WHEN** 一本图书被成功借出
- **THEN** 该图书的可借数量减少 1

#### Scenario: 上架确认后库存恢复
- **WHEN** 一本已归还的图书完成上架确认
- **THEN** 该图书的可借数量增加 1

### Requirement: Display book location
系统 SHALL 为每本图书展示当前位置，位置取该图书最近一次上架确认记录的书架位置；检索结果中 MUST 展示位置信息。

#### Scenario: 检索结果展示图书位置
- **WHEN** 检索返回图书列表
- **THEN** 每本图书展示其当前位置（最近一次上架确认记录的位置）

#### Scenario: 待上架图书的位置展示
- **WHEN** 图书处于待上架状态（尚无上架确认记录）
- **THEN** 该图书位置展示为待上架，不显示书架位置
