# Tasks

## 1. 数据模型与迁移

- [x] 1.1 schema.sql 给 shelving_record 加 review_status（PENDING/APPROVED/REJECTED，默认 PENDING）、reviewer、reviewed_at、reject_reason 四列，ShelvingRecord 实体同步加字段，验证 mvn test 编译通过
- [x] 1.2 开发库 ALTER TABLE 补四列，历史记录 UPDATE review_status='APPROVED'，验证 SHOW COLUMNS 与历史记录状态正确

## 2. 后端上架审核

- [x] 2.1 ShelvingService.confirm 状态机调整：pendingShelving -= count、写记录（review_status=PENDING），availableCopies 与 location 不变，验证 ShelvingApiTest 既有上架用例断言更新后通过
- [x] 2.2 新增 GET /api/shelvings/reviews 待审核清单（review_status=PENDING 按上架时间倒序，组装书名/ISBN），新增 POST /api/shelvings/reviews/{recordId}/approve|reject（审核人必填、驳回可选理由），验证 ShelvingApiTest 新增审核用例（清单查询、通过后 available 增加、驳回后 pending 回补）全部通过
- [x] 2.3 借出校验确认：待审核图书（available=0）借出被拒，验证 LoanApiTest 通过且手工 HTTP 验证 POST /api/loans 返回拒绝提示

## 3. 后端书籍识别

- [x] 3.1 新增 BookInfoRecognizer 接口与 MockBookInfoRecognizer（配置 library.recognizer.provider=mock 装配），POST /api/books/recognize 接收 multipart 照片返回 {title, author, isbn}，识别不落库，验证 BookApiTest 新增识别用例通过
- [x] 3.2 录入复用 POST /api/books（新书进待上架清单），验证识别结果经确认修正后录入的图书出现在待上架清单

## 4. PC 端待审核页签

- [x] 4.1 ShelvingView 拆为三页签：待上架清单 / 待审核 / 上架记录；待审核页签展示书名、位置、照片（可预览）、本次册数、操作人、上架时间，支持通过与驳回（驳回可填理由），验证 npm run build 通过
- [x] 4.2 审核操作后待审核清单、待上架清单、图书可借状态联动正确，验证前端 build + 后端重启后接口联调通过

## 5. 微信小程序（uni-app）

- [x] 5.1 创建 miniapp/ 项目（uni-app + Vue 3 脚手架，微信小程序平台），配置后端地址指向 http://localhost:8080，验证微信开发者工具可导入运行
- [x] 5.2 书籍查询页：按书名/ISBN 查询，展示书名、作者、分类、可借数量、所在位置，验证与 /api/books 接口联调返回正确
- [x] 5.3 在借查询页：输入借书证号查询当前在借书籍（书名、借出时间、应还时间、逾期标识），验证与 /api/readers/card-no/{no}、/api/loans/reader/{readerId} 接口联调返回正确
- [x] 5.4 工作人员录入页：拍照上传 → 调用识别接口 → 确认/修正书名作者 ISBN → 提交录入馆藏，验证录入后 PC 待上架清单出现该图书

## 6. 集成验证

- [x] 6.1 后端 mvn test 全量通过（含新增审核与识别用例），前端 npm run build 通过
- [x] 6.2 重新打包 jar 并重启，HTTP 验证：识别接口、待审核清单、审核通过/驳回后库存变化、小程序复用接口均正常
- [ ] 6.3 浏览器走查 PC 待审核页签全流程（上架确认 → 待审核 → 通过后借还可借、驳回后回待上架），与 specs 一致

## Workflow follow-up

- 实施完成后运行 openspec validate 校验实现与产物一致。
- 校验通过后归档本 change（archive）。
