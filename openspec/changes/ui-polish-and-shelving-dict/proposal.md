# Proposal

## Why

MVP 交付验收中发现若干影响使用的缺陷与体验问题：图书分类建立多层结构后深层分类无法在图书表单中选择；上架照片上传后无法预览；书架位置为自由文本，录入不一致且无法复用；菜单切换后原页面销毁导致查询条件与列表数据丢失；新增操作按钮位置不统一；系统名称需修正为「图书管理系统」。

## What Changes

- 修复：图书分类下拉仅展示 category 的一层子节点，深层分类无法选择 → 改为展示分类树全部后代，多级分类均可选择
- 修复：上架照片上传返回物理相对路径（uploads/shelving/xxx.png），经 /uploads/** 静态映射时目录段重复导致 404 无法预览 → 统一返回 Web 访问路径 /uploads/<文件名>
- 变更：书架位置纳入系统字典管理（新增「书架位置」顶层字典与种子数据），上架确认改为从字典下拉选择
- 变更：新增图书、注册读者按钮移到表格上方工具栏；字典管理「新增顶层字典」按钮文案改为「新增分类」
- 变更：菜单切换改为页签式（可关闭），已打开页面的查询条件与列表数据在切换后保留（keep-alive）
- 变更：系统名称「图书馆图书管理系统」改为「图书管理系统」（页面标题、应用描述、建表脚本注释、文档）

## Capabilities

### New Capabilities

- `web-ui`: 管理界面的交互体验能力：页签式菜单切换与页面数据保留、列表页工具栏布局

### Modified Capabilities

- `book-catalog`: 图书分类选择支持分类树全部后代（多级分类均可选）
- `circulation`: 上架确认的书架位置取自系统字典；上架照片以可访问的 Web 路径存储与展示
- `system-dictionary`: 新增「书架位置」字典种子数据

## Impact

- 后端：FileStorageService（照片返回 /uploads/ 前缀 Web 路径）、WebConfig（静态映射目录尾斜杠兜底）、db/seed-dict.sql（书架位置种子）
- 前端：BookView（分类树平铺下拉、工具栏）、ReaderView（工具栏）、ShelvingView（位置下拉）、DictView（按钮文案）、App.vue（页签 + keep-alive + 系统名）、router.ts（空白页路由）、index.html（标题）
- 数据：library 开发库需补插书架位置字典种子；既有 shelving_record 照片路径为旧格式，需一次性迁移为 /uploads/<文件名>
- 文档：README（系统名、字典种子说明）
