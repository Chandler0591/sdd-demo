# Design

## Context

动机见 proposal.md。现状约束：

- 分类下拉使用 `GET /api/dicts/children?pcode=category`，只返回一层直接子节点，字典建出多层结构后深层分类不可选
- 上架照片：`FileStorageService` 返回物理相对路径 `uploads/shelving/xxx.png`；`WebConfig` 将 `/uploads/**` 映射到上传目录本身，请求 `/uploads/shelving/xxx.png` 时 `**` 匹配 `shelving/xxx.png` 再拼接到映射目录尾部，形成 `.../uploads/shelving/shelving/xxx.png` 目录段重复 → 404，无法预览
- 书架位置为上架确认弹窗的自由文本输入，无法复用与统一
- 前端 `App.vue` 为侧边菜单 + 单一 `router-view`，切换菜单组件销毁、数据丢失；前端栈 Vue 3.5 + Element Plus 2.10 + vue-router 4（hash 路由）

## Goals / Non-Goals

**Goals:**

- 分类下拉覆盖「图书分类」字典树全部后代，多级分类均可选，同名分类以路径消歧
- 照片上传返回可直接访问的 Web 路径，上架确认弹窗预览可用
- 书架位置纳入系统字典管理，上架确认改为下拉选择
- 菜单切换为可关闭页签，已打开页面数据在切换后保留
- 新增操作统一到表格上方工具栏；系统名称统一为「图书管理系统」

**Non-Goals:**

- 照片不做对象存储迁移（保持本地目录，演进接口已预留）
- 页签不做刷新后恢复（刷新后仅保留当前路由页签）
- 不改字典数据模型（书架位置复用现有 dict 树结构，不引入独立表）
- 不引入 Element Plus 之外的标签/树选择组件

## Decisions

1. **分类下拉：树接口 + 前端平铺**。BookView 改用 `GET /api/dicts` 树接口，取 `category` 子树递归平铺为 `{code, label}`（label 为「父级 / 子级」路径），检索与新增表单共用。备选：后端新增「子树平铺」接口——否决，纯展示诉求不应膨胀后端 API；`el-tree-select`——否决，需引入额外组件且与现有 `el-select` 交互不一致。

2. **书架位置字典化：下拉存名称**。新增顶层字典 `code=shelf-location` 与种子位置项；上架确认下拉数据复用 `GET /api/dicts/children?pcode=shelf-location`；下拉 value 用 code，提交时映射为 name 写入上架记录与图书当前位置（保持 VARCHAR 文本语义）。备选：直接存 code——否决，历史记录与图书位置展示需回查字典，字典项删除后展示失效；存名称则历史记录自包含，字典改名不影响已存数据。

3. **照片路径：固定 Web 前缀 + 可配置物理目录**。`FileStorageService.store` 返回 `/uploads/<文件名>`（固定 Web 前缀），物理目录仍由 `library.upload-dir` 配置；`WebConfig` 保持 `/uploads/**` → uploadDir 物理目录的映射并补齐 location 尾斜杠。请求 `/uploads/xxx.png` 时 `**` 仅匹配文件名，与映射目录拼接后命中真实文件，且物理目录可配置而 Web 前缀恒定。备选：把映射改指 uploadDir 父目录——否决，uploadDir 可变导致映射与存储脱节；前端拼接 `/${photoPath}`——否决，路径知识泄漏到前端且旧数据仍 404。

4. **页签 + keep-alive**。App.vue 内容区顶部放 `el-tabs`（type=card、closable）：tabs 列表与路由双向同步（watch 路由添加页签、点击页签 `router.push`、移除页签后导航到相邻页签）；`router-view` 外包 `keep-alive`（不设 include，缓存全部已渲染页面组件，恰好覆盖 5 个菜单页）；全部页签关闭后导航到 `/blank` 空白路由（`meta.hiddenTab` 不生成页签）。备选：keep-alive 按组件名 include——需为每个 SFC 显式声明 name，收益低；v-show 常驻多组件——无法与路由深度链接且全部页面常驻内存。

5. **工具栏与文案**。BookView / ReaderView 把新增按钮从检索表单行移到表格卡片 header（左侧标题、右侧按钮），复用 DictView 已有的 header-row 样式；DictView 按钮文案「新增顶层字典」→「新增分类」。

6. **系统名**。前端 index.html title、App.vue logo、pom.xml description、schema.sql 注释、README 统一改为「图书管理系统」；openspec 历史规划产物不做修改。

7. **借还办理检索：精确优先 + 模糊候选**。LoanView 读者/图书查询先调用既有精确接口（`/readers/card-no/{no}`、`/books/isbn/{isbn}`），命中直接选中；未命中再调用既有分页检索接口（`/readers?name=`、`/books?title=`）展示候选列表供点选（单条自动选中）。后端零改动，复用已实现的检索能力。备选：新增关键字混合检索接口——否决，两个既有接口组合已覆盖交互，避免后端 API 膨胀。

8. **分类树形选择**。新增/编辑弹窗的分类改用 `el-tree-select`（check-strictly 任意层级可选、default-expand-all），数据为 category 子树；检索筛选保留平铺路径式下拉（路径 label 在检索场景更有区分度）。备选：全部改树形——否决，检索筛选用树形选择操作成本更高。

9. **书架位置多级树 + 树形选择**。书架位置字典种子扩展为「区 / 排 / 号」两级子层；上架确认的位置选择与分类一致改用 `el-tree-select` 展示全部后代，提交时按 code 在树中向上拼接完整路径名（如「A 区 1 排 01 号」）存储，保证历史上架记录位置自包含。页签关闭按钮缺失（`el-tabs` 未加 `closable`）为既有缺陷，补上 `closable` 属性即可。

10. **新书入库即待上架**。修正「新书入库即视为已上架」的旧设计：`create` 与 `add-copies` 统一进入待上架清单（可借 0、待上架 += 新增量），上架确认后才恢复可借，保证每册书都有位置与上架记录（支撑整理效率考核）；检索分类筛选同步改树形，与新增/编辑表单一致。已有数据一次性迁移：location 为空且 pending=0 的图书置为待上架。备选：新书直接可借——否决，与上架流程闭环矛盾。

11. **批量上架确认**。上架确认支持一次处理多册：请求带本次上架数量（缺省 1 向后兼容，范围 1 ~ 待上架数），待上架/可借按本次数量增减；上架记录表加 `shelved_count` 列（开发库 ALTER TABLE 补列）；前端弹窗默认带入全部待上架数量。备选：每册一条上架记录——否决，同位置批量上架一条记录更符合实际作业。

12. **上架记录页**。上架办理页拆为「待上架清单 / 上架记录」两个页签：上架记录按上架时间倒序分页，可按书名（先模糊定位图书再限定记录）与操作人模糊筛选，记录含现场照片缩略图（点击预览）；新增 `GET /api/shelvings/records` 接口，复用 book 与 shelving_record 两表查询后组装书名 / ISBN。备选：独立菜单页——否决，与上架业务同页切换更直接；在图书列表位置列加时间——否决，照片与操作人无法承载。

## Risks / Trade-offs

- [既有 shelving_record 照片为旧路径格式，不迁移则历史照片仍无法预览] → 随本次部署执行一次性 UPDATE，取旧路径最后一段文件名重构为 /uploads/<文件名>
- [keep-alive 缓存后跨页数据不会自动刷新（如借出后打开待上架清单）] → 各页面保留手动刷新/检索入口，接受为页签缓存的固有取舍
- [位置字典项名称可重名，下拉 value=code 已消歧，但历史记录只存名称] → 重名场景罕见，MVP 接受；后续可演进为存 code
- [深层分类的路径式 label 较长] → 路径式 label 本身即同名消歧手段，接受

## Migration Plan

1. 构建部署：前端 `npm run build` → 后端 `mvn package` → 替换 jar 重启
2. 数据脚本：library 开发库补插书架位置字典种子（shelf-location / A 区 / B 区）；执行 shelving_record 照片路径迁移 UPDATE
3. 回滚：重新部署上一版本 jar 即可——新增字典项与照片路径新格式不影响旧版读取，位置字段为文本天然兼容
