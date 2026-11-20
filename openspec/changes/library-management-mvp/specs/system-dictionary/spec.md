# Spec Delta

## Purpose

系统字典能力，统一维护 code / name / pcode 树形结构的字典数据（如图书分类），供各业务模块引用，避免枚举取值散落各处。

## ADDED Requirements

### Requirement: Maintain dictionary entries
系统 SHALL 允许管理员维护字典项（新增与编辑），字典项由编码 code、名称 name 与父编码 pcode 构成树形层级；code MUST 全局唯一且创建后不可修改。

#### Scenario: 查看字典树
- **WHEN** 管理员打开字典管理
- **THEN** 系统以树形层级展示全部字典项

#### Scenario: 新增字典项
- **WHEN** 管理员提交包含 code、name 与 pcode 的字典项新增请求
- **THEN** 系统保存该字典项并使其在树中可见

#### Scenario: 重复 code 被拒绝
- **WHEN** 管理员新增的字典项 code 已存在
- **THEN** 系统拒绝保存并返回明确错误提示

#### Scenario: 编辑字典项
- **WHEN** 管理员修改字典项的名称或父级
- **THEN** 系统保存修改，引用该字典项的业务界面展示更新后的名称

### Requirement: Provide dictionary items for business forms
系统 SHALL 支持按父编码（pcode）查询字典子树，向业务表单提供字典项列表供下拉选择。

#### Scenario: 获取分类字典项
- **WHEN** 图书录入表单请求分类字典
- **THEN** 系统返回分类子树下的全部字典项供下拉选择
