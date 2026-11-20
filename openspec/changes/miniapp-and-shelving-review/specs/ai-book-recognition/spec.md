# Spec Delta

## Purpose

通过上传书籍照片识别提取书名、作者与 ISBN 等书目信息，识别服务以接口抽象组织，支持模拟实现与后续真实大模型接入替换。

## ADDED Requirements

### Requirement: Recognize book info from photo
系统 MUST 提供书籍照片识别接口：接收照片文件，返回识别出的书名、作者与 ISBN；MVP 阶段返回模拟识别结果，接口形态保持与真实大模型一致以便后续替换。

#### Scenario: 上传照片返回识别结果
- **WHEN** 客户端上传一张书籍照片调用识别接口
- **THEN** 系统返回识别结果，包含书名、作者与 ISBN 字段

#### Scenario: 照片无法识别
- **WHEN** 上传的照片无法被识别出书籍信息
- **THEN** 系统返回识别失败提示，不产生任何馆藏数据变更

### Requirement: Recognition result editable before catalog entry
识别结果 MUST 允许在录入馆藏前被人工确认与修正，识别接口本身只做识别、不写入馆藏。

#### Scenario: 确认或修正后录入
- **WHEN** 工作人员对识别结果进行确认或修正后提交
- **THEN** 以确认后的信息录入馆藏，而非直接使用原始识别结果
