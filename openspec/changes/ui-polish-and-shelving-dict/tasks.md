# Tasks

## 1. 后端修复与种子数据

- [x] 1.1 修改 FileStorageService 照片上传返回固定 Web 路径 `/uploads/<文件名>`，WebConfig 静态映射 location 补齐尾斜杠，验证上传后请求返回路径可经 GET 访问到照片内容（修复目录段重复 404）
- [x] 1.2 在 db/seed-dict.sql 增加「书架位置」顶层字典（shelf-location）与示例位置子项（A 区 / B 区），验证 DictApiTest 种子相关断言仍通过
- [x] 1.3 对 library 开发库补插书架位置种子数据，并执行 shelving_record 照片旧路径一次性迁移（`uploads/shelving/xxx.png` → `/uploads/xxx.png`），验证历史上架记录照片路径可访问

## 2. 前端功能修复

- [x] 2.1 BookView 分类下拉改用字典树接口并平铺 category 全部后代（label 为「父级 / 子级」路径，检索与新增表单共用），验证多级分类在新增与检索中均可选择
- [x] 2.2 ShelvingView 上架确认的书架位置改为字典下拉（数据取自 /api/dicts/children?pcode=shelf-location，提交存所选名称），验证无位置项时提交被后端拒绝并提示
- [ ] 2.3 验证上架照片上传后弹窗内 el-image 可正常预览（依赖 1.1，上传后 photoPath 以 /uploads/ 开头且图片可见）

## 3. 前端体验优化

- [x] 3.1 BookView「新增图书」与 ReaderView「注册读者」按钮移到表格上方工具栏（卡片 header 左侧标题、右侧按钮），验证按钮可见且点击打开表单弹窗
- [x] 3.2 DictView「新增顶层字典」按钮文案改为「新增分类」，验证新增顶层项流程不受影响
- [x] 3.3 App.vue 改为页签式菜单（el-tabs 可关闭、与路由双向同步、全部关闭后到空白页），router-view 外包 keep-alive 保留页面数据，router.ts 增加 /blank 隐藏页签路由，验证切换菜单后旧页面检索条件与列表数据保留
- [x] 3.4 系统名称统一为「图书管理系统」（frontend/index.html、App.vue logo、pom.xml description、db/schema.sql 注释、README），验证页面标题与侧栏名称一致

## 4. 集成验证

- [x] 4.1 前端 npm run build 成功且后端 mvn test 全量通过（含种子变更后的字典测试），验证无编译与回归错误
- [x] 4.2 mvn package 后以 java -jar 重启单进程，HTTP 验证首页、/api/dicts、/uploads 照片访问均正常
- [ ] 4.3 浏览器手工走查：多级分类选择、书架位置下拉、照片上传预览、页签切换数据保留、工具栏按钮，验证行为与 specs 一致

## 5. 借还检索与分类树形选择

- [x] 5.1 LoanView 读者查询支持姓名模糊（借书证号精确命中优先，未命中按姓名模糊列出候选供选择），验证姓名关键字可找到读者并展示在借记录
- [x] 5.2 LoanView 图书查询支持书名模糊（ISBN 精确命中优先，未命中按书名模糊列出候选供选择），验证书名关键字可找到图书并办理借出
- [x] 5.3 BookView 新增/编辑弹窗分类改为树形选择（el-tree-select 展示 category 子树、任意层级可选），检索筛选保持平铺路径下拉，验证深层分类可在树形下拉中选择
- [x] 5.4 前端 build 成功、后端 mvn test 全量通过、重新打包重启 jar 后接口验证正常

## 6. 位置字典多级与页签关闭

- [x] 6.1 seed-dict.sql 书架位置种子增加「排 / 号」两级示例，开发库同步补插
- [x] 6.2 ShelvingView 位置改为树形选择（el-tree-select 展示 shelf-location 全部后代），提交保存完整路径名
- [x] 6.3 App.vue 页签补上 closable 关闭按钮，关闭当前页签后导航相邻页签、全部关闭导航空白页
- [x] 6.4 前端 build 成功、后端 mvn test 全量通过、重新打包重启 jar 后接口验证正常

## 7. 新书入库待上架与分类筛选树形

- [x] 7.1 后端 BookService.create/addCopies 新书进入待上架清单（可借 0、待上架 = 新增量），BookApiTest 断言同步更新
- [x] 7.2 BookView 检索筛选分类改树形选择，删除平铺逻辑，新增/叠加成功提示指向待上架清单
- [x] 7.3 LoanView 待上架图书（可借 0）标识「待上架，暂不可借」并禁用借出按钮
- [x] 7.4 开发库迁移无位置图书为待上架，build/test/重启后验证待上架清单包含万历十五年

## 8. 批量上架确认

- [x] 8.1 后端上架确认支持本次上架数量（1 ~ 待上架数，缺省 1），shelving_record 加 shelved_count 列，超限拒绝
- [x] 8.2 ShelvingApiTest 加批量上架与超限用例，schema.sql 与开发库同步加列
- [x] 8.3 前端上架弹窗加「本次上架数量」（默认全部待上架数），提交带数量
- [x] 8.4 build/test/重启后验证批量上架后待上架清单数量变化正确

## 9. 上架记录页

- [x] 9.1 后端新增 GET /api/shelvings/records 分页检索（书名/操作人模糊、上架时间倒序、返回书名与 ISBN），ShelvingApiTest 加检索用例
- [x] 9.2 ShelvingView 拆为「待上架清单 / 上架记录」页签，记录页含筛选、分页与照片预览
- [x] 9.3 build/test/重启后验证上架记录页检索与照片预览正常

## Workflow follow-up

- 实施完成后运行 openspec validate 校验实现与产物一致。
- 校验通过后归档本 change（archive）。
