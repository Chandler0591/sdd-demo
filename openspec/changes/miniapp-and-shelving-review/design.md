# Design

## Context

动机见 proposal.md。现状约束：

- 后端 Spring Boot 3 + easy-query + MySQL 8，图书库存语义：`availableCopies`（可借）/ `pendingShelving`（待上架）/ `location`（当前位置）；借出校验 `availableCopies > 0`
- 上架确认（`POST /api/shelvings/confirm`）当前立即恢复可借（available += count）并更新位置，写 `shelving_record`（位置 / 照片 / 册数 / 操作人 / 时间）
- 既有可复用接口：`GET /api/books?title=` 检索（BookVO 含 location）、`GET /api/readers/card-no/{no}`、`GET /api/loans/reader/{readerId}`（LoanVO 含书名 / 借出时间 / 应还时间 / 逾期标记）
- 前端 PC 为 Vue 3 + Element Plus；微信小程序端尚不存在；无用户体系与鉴权

## Goals / Non-Goals

**Goals:**

- 小程序零鉴权可查书籍与位置、按借书证号查在借；工作人员拍照识别录入馆藏
- 上架增加审核环节：审核通过才可借，驳回退回待上架
- 识别服务接口抽象，模拟实现可无缝替换为真实大模型

**Non-Goals:**

- 不做微信登录与 openid 绑定（游客可查、工作人员不鉴权）
- 小程序不做上架确认与审核（统一在 PC 完成），小程序只负责识别 + 录入
- 不接真实大模型（模拟实现，接口形态预留）
- 不发布上线（开发阶段仅微信开发者工具本地调试）

## Decisions

1. **审核状态挂在 shelving_record 上，book 表不加字段**。`shelving_record` 增加 `review_status`（PENDING / APPROVED / REJECTED）、`reviewer`、`reviewed_at`、`reject_reason` 四列；待审核清单 = 查询 review_status=PENDING 的记录（按上架时间倒序，组装书名）。备选：book 表加 `shelf_status` 状态列——否决，审核天然是"对上架记录"的操作，记录留痕审核人与时间更直接，且 book 表与既有 pending/available 语义不必再加一维状态。

2. **库存状态机调整：确认不恢复可借，审核通过才恢复**。上架确认：`pendingShelving -= count`、写记录（PENDING），`availableCopies` 与 `location` 不变（图书从待上架清单移入待审核）；审核通过：`availableCopies += count`、`location = 记录位置`、记录置 APPROVED；审核驳回：`pendingShelving += count` 回补、记录置 REJECTED。借出校验 `availableCopies > 0` 天然阻止待审核图书借出，借还办理端零改动。备选：确认时即扣 pending 并加 available、审核驳回再回滚——否决，与"审核通过才算真正上架"的用户要求矛盾。

3. **识别接口抽象 + 模拟实现**。新增 `BookInfoRecognizer` 接口（`recognize(photo) -> {title, author, isbn}`）与 `MockBookInfoRecognizer`（返回固定书名 / 作者 / ISBN），通过配置 `library.recognizer.provider=mock` 装配；`POST /api/books/recognize` 接收 multipart 照片返回识别结果，只识别不落库，录入复用既有 `POST /api/books`（新书进待上架清单）。备选：直接写死 Controller 内模拟逻辑——否决，无法演示真实接入的替换点。

4. **小程序复用既有查询接口，后端只新增识别与审核接口**。书籍查询用 `GET /api/books?title=`（BookVO 含位置），在借查询用 `GET /api/readers/card-no/{no}` + `GET /api/loans/reader/{readerId}`；新增 `POST /api/books/recognize`、`GET /api/shelvings/reviews`、`POST /api/shelvings/reviews/{recordId}/approve|reject`。备选：为小程序另开 /miniapp 前缀接口——否决，查询语义相同，重复接口徒增维护。

5. **小程序用 uni-app + Vue 3（`miniapp/` 目录）**，与 PC 前端同栈。页面：书籍查询、在借查询（输入借书证号）、工作人员录入（拍照 → 识别 → 确认/修正 → 录入）。上架确认与审核留在 PC：避免双端重复上架流程，且审核本就是 PC 场景。备选：原生小程序——否决，Vue 经验与类型定义无法复用；小程序内嵌上架确认——否决，超出 MVP 且与 PC 审核流程重复。

6. **PC 上架办理页拆为三页签**：待上架清单 / 待审核 / 上架记录。待审核页签展示记录信息（书名、位置、照片、本次册数、操作人、上架时间）与通过 / 驳回操作（驳回可填理由）。

7. **历史数据迁移**：开发库与 schema.sql 同步给 `shelving_record` 加 4 列；已存在的上架记录（此前已生效可借）迁移为 `review_status='APPROVED'`，保持既有可借状态不变。

## Risks / Trade-offs

- [微信小程序发布需备案域名与 HTTPS，开发调试需在微信开发者工具勾选"不校验合法域名"] → 本 change 只到开发调试可用，上线前置条件另立 change
- [模拟识别返回固定结果，真实拍照无法识别真实书籍] → 接口形态与真实大模型一致，后续仅替换实现类并配置 provider，specs 不变
- [审核状态下"待上架"与"待审核"两态并存，借还办理均显示"待上架，暂不可借"] → available 均为 0 语义一致，MVP 接受共用文案，后续可细化
- [驳回后图书回到待上架清单需重新走确认流程] → 符合预期（驳回即上架信息不合规），审核记录保留驳回痕迹

## Migration Plan

1. 开发库执行 `ALTER TABLE shelving_record ADD COLUMN review_status/reviewer/reviewed_at/reject_reason`，历史记录 UPDATE 为 APPROVED
2. 后端实现后 `mvn test` + 打包重启；PC 前端 `npm run build` + 打包重启（前端产物嵌入 jar）
3. 小程序 `miniapp/` 项目用微信开发者工具导入，本地调试指向 `http://localhost:8080`
4. 回滚：重新部署上一版本 jar 即可（新增列带默认值，旧代码读写不受影响；APPROVED 迁移与旧语义一致）

## Open Questions

- 真实大模型选型与 API Key（后续接入时再定，不影响本 change 的接口形态与任务拆分）
