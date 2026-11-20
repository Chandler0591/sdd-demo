# Spec Delta

## Purpose

读者管理能力，支持管理员维护图书馆读者的档案信息，是办理借阅业务的前置数据。

## ADDED Requirements

### Requirement: Register reader
系统 SHALL 允许管理员注册读者，登记姓名、借书证号与联系方式；借书证号 MUST 全局唯一。

#### Scenario: 注册新读者成功
- **WHEN** 管理员提交包含姓名、借书证号与联系方式的读者注册请求
- **THEN** 系统保存读者档案并返回读者唯一标识

#### Scenario: 重复借书证号被拒绝
- **WHEN** 管理员注册的借书证号已被使用
- **THEN** 系统拒绝保存并返回明确错误提示

### Requirement: Edit reader profile
系统 SHALL 允许管理员修改读者档案信息。

#### Scenario: 修改读者联系方式
- **WHEN** 管理员提交对读者档案的修改
- **THEN** 系统保存修改，后续查询返回更新后的档案

### Requirement: Deactivate reader
系统 SHALL 允许管理员注销读者；当该读者仍有未归还的借阅记录时，系统 MUST 拒绝注销。

#### Scenario: 注销无未归还借阅的读者
- **WHEN** 管理员注销一名没有未归还借阅记录的读者
- **THEN** 该读者档案被标记为注销，无法再办理借阅

#### Scenario: 注销有未归还借阅的读者被拒绝
- **WHEN** 管理员注销一名仍有未归还借阅记录的读者
- **THEN** 系统拒绝注销并提示先归还全部图书

### Requirement: Search readers
系统 SHALL 支持按姓名或借书证号检索读者档案。

#### Scenario: 按借书证号查询读者
- **WHEN** 管理员以借书证号查询
- **THEN** 系统返回匹配的读者档案
