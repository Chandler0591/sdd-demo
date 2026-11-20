# Design

## Context

全新（greenfield）项目，工作区无现有代码。约束：数据库 MUST 使用 MySQL（用户明确要求），所有表主键为字符串类型雪花 ID；Web 全栈形态；MVP 仅管理员（馆员）单角色操作，无登录认证。动机与范围见 proposal.md。

## Goals / Non-Goals

**Goals:**
- 以最少技术栈交付「图书编目 / 读者管理 / 借阅流通 / 系统字典」四个 capability 的可运行 Web 应用。
- 数据访问统一经 easy-query 的 EasyEntityQuery 类型安全查询层（实体查询器 + 表达式更新），后续演进（读写分离、多实例）不重写业务层。

**Non-Goals:**
- 不做登录、权限、读者自助门户（OPAC）、续借、预约、罚款。
- 不做多分馆、条码扫描等硬件集成。

## Decisions

### 1. 后端选型：Java 17 + Spring Boot 3 + easy-query

MySQL 与 Spring Boot 生态整合成熟（easy-query 官方 starter + mysql-connector-j），easy-query 的 Lambda 类型安全查询与声明式事务适合借阅这类强一致性业务。
- 备选 A：Spring Data JPA —— 生态最大，但复杂多表查询需 JPQL/原生 SQL 脱离类型安全，动态条件查询冗长，弃用。
- 备选 B：Node.js/Express + MySQL —— 生态同样成熟，但缺乏类型安全查询与声明式事务体系，弃用。
- 备选 C：Python/FastAPI + MySQL —— 可行，但 ORM 迁移与类型安全弱于 easy-query，弃用。
- 备选 D：Spring Boot 之外的其他 JVM 框架（Quarkus 等）—— 无特殊性能需求，Spring Boot 生态与资料最丰富，选 Spring Boot。

### 2. 前端选型：Vue 3 + Vite + TypeScript + Element Plus

管理后台需要表格、表单、弹窗等成熟组件，Element Plus 覆盖完整。
- 备选 A：React + Ant Design —— 同样可行，选 Vue 仅为团队偏好假设（记录：用户未明确，按推荐项 Vue 落地，若需 React 可低成本替换）。
- 备选 B：Thymeleaf 服务端渲染 —— 省去前端工程，但页面交互（检索、借还办理）开发体验差，弃用。

### 3. 架构：前后端分离 + REST API

- 后端暴露 `/api/books`、`/api/readers`、`/api/loans`、`/api/shelvings`、`/api/dicts` 五组 REST 资源。
- 开发期：Vite dev server 代理 `/api` 到后端（localhost:8080），避免 CORS。
- 交付期：前端构建产物由 Spring Boot 以静态资源方式托管，单进程部署（图书馆内网一个 jar 即可）。

### 4. 数据库：MySQL 8 + 手动建表脚本

- 依赖 `mysql-connector-j`，JDBC URL 指向本地/内网 MySQL 8，数据库 `library`，字符集 utf8mb4。
- easy-query 无内置 DDL 管理能力（无自动建表/更新表结构机制），建表由首次部署时管理员手工执行建表脚本（`db/schema.sql`，含五张表与唯一约束），应用启动仅做连接校验；表结构演进在后续 change 中通过增量 SQL 脚本管理，结构变更频繁或需要可回放迁移时再引入 Flyway（见 Open Questions）。
- 彻底移除 H2：开发与集成测试统一使用 MySQL，集成测试使用独立测试库 `library_test`，避免与开发数据互相污染。

### 5. 数据模型

五张表（所有主键均为字符串类型雪花 ID，见决策 10），由 easy-query 实体（@Table / @Column 注解）映射：

- `book`：`id`、`isbn`（唯一约束）、`title`、`author`、`category_code`（分类字典 code，引用 dict）、`total_copies`、`available_copies`（已上架可借）、`pending_shelving`（已归还待上架）、`location`（当前位置，取最近一次上架记录）。
- `reader`：`id`、`card_no`（唯一约束）、`name`、`phone`、`active`（注销标记）。
- `loan`：`id`、`book_id`、`reader_id`、`borrowed_at`、`due_at`、`returned_at`（null 表示未归还）。
- `shelving_record`：`id`、`book_id`、`location`（书架位置）、`photo_path`（现场照片存储路径）、`shelved_at`（上架时间，服务端生成）、`operator`（操作人姓名）。
- `dict`：`id`、`code`（唯一约束）、`name`、`pcode`（父字典项 code，空表示顶层）、`sort_order`（同级排序）。

库存不变量：`total_copies = available_copies + 借出中 + pending_shelving`。采用冗余计数而非实时聚合：查询简单、无性能压力，代价是需与借阅/上架操作保持事务一致（见决策 6）。图书的当前位置取最近一条 `shelving_record.location`，供读者定位。

字典表 `dict` 以 `pcode` 形成树形层级（顶层 `pcode` 为空，如「分类」根节点下挂「文学、科技」等），业务表只存 code、展示时按 code 取 name，统一维护枚举取值。

### 6. 借阅业务规则与事务

- 借期默认 60 天、读者同时在借上限 10 册，均做成配置项（`library.loan-days`、`library.max-active-loans`），不硬编码。
- 借出/归还/上架均为单事务（复用 Spring @Transactional，easy-query 原生支持 Spring 事务集成）：借出 = 校验（读者未注销、可借 > 0、在借未达上限）→ 库存 -1 → 创建借阅记录；归还 = 标记归还并记录归还日期 → `pending_shelving` +1（可借不变）；上架确认 = 校验位置与照片必填 → `pending_shelving` -1、可借 +1 → 写入 `shelving_record`。任一步失败整体回滚。
- 删除图书/注销读者的前置校验（无未归还记录）在 Service 层实现，返回中文业务错误。
- 超期不落库：查询时按 `due_at < now AND returned_at IS NULL` 动态判定，避免定时任务。

### 7. 错误处理与接口风格

- 统一异常处理：业务规则冲突返回 4xx + 中文 message；前端直接展示。
- REST 请求/响应使用 DTO 而非直接暴露实体，避免循环引用（Loan ↔ Book/Reader）。

### 8. 上架照片与操作人

- 照片由浏览器端拍照上传，**暂用后端本地文件目录存储**（如 `uploads/shelving/`），数据库只存相对路径：MySQL 可存 BLOB 但会使库膨胀并拖慢备份与查询，文件目录方案更轻量；存储经统一文件服务封装（接口化），后续可平滑切换为对象存储（MinIO/OSS）。
- 上架时间 `shelved_at` 由服务端生成（服务器时钟），不信任客户端时间戳，保证考核数据可信。
- 操作人：MVP 无账号体系，上架确认时由管理员手工填写操作人姓名，存入 `shelving_record.operator`；后续引入登录后可替换为账号关联。

### 9. 字典种子数据与维护

- 种子数据手动初始化：首次部署时由管理员手动执行初始化 SQL 脚本（`db/seed-dict.sql`，插入分类等种子数据），应用启动不做自动初始化；后续新增字典经管理界面维护。
- 字典管理界面仅提供新增与编辑（树形列表），`code` 创建后只读（改名会破坏业务表引用语义；展示名 `name` 可随时修改，即时生效）；删除留作后续 change，避免引用一致性复杂度。
- 业务表单通过 `pcode` 查询子树获取下拉选项（如图书录入的分类下拉）。

### 10. 主键策略：字符串雪花 ID

- 所有实体主键定义为字符串类型（VARCHAR(19)），由后端统一生成雪花 ID，不依赖数据库自增。
- 生成器：引入 hutool `IdUtil.getSnowflake()`，应用启动时初始化机器号（配置项 `library.snowflake.worker-id`）；easy-query 无内置主键生成器钩子，实体插入前由应用层统一生成器赋值主键（Service 层写入）。
- 理由：避免 MySQL 自增主键在分库/多实例部署下的冲突；字符串主键便于跨系统传输与前端展示；雪花 ID 按时间趋势递增，保持聚簇索引友好。
- 备选 A：数据库自增 Long —— 简单，但多实例/分库扩展受限，弃用。
- 备选 B：UUID 字符串 —— 无序，导致聚簇索引页分裂、写入性能差，弃用。

## Risks / Trade-offs

- [MySQL 环境依赖] → 运行前需准备 MySQL 实例（本地安装或 Docker），README 提供建库脚本与 docker-compose 示例；MySQL 未就绪时应用启动失败并给出明确报错。
- [并发借出导致库存超卖] → 单馆员单实例场景并发极低；事务内先校验再扣减已足够，后续如需再引入悲观锁/版本号。
- [前后端分离增加工程复杂度] → 通过 Vite proxy 与单 jar 打包策略抵消；若团队更偏好单体，前端方案可替换（见决策 2 备选 B）。
- [借阅上限与借期为假设值（60 天/10 册）] → 已配置化，按图书馆实际政策调整配置即可，无需改代码。
- [照片文件与数据库记录不一致] → 上架确认回滚或记录删除时照片文件可能残留；实施时在 Service 层处理上传失败清理，孤儿文件清理留作后续 change。
- [字典项改名影响历史展示] → 图书展示的分类名随字典 name 实时变化，属统一维护的预期行为；若未来需要历史快照语义，可另做版本表（不在本 MVP）。
- [easy-query 社区相对较小] → 官方中文文档完整、查询 API 为纯 Java 类型安全接口无运行时魔法，必要时可用原生 SQL 兜底，不构成锁定。

## Migration Plan

- greenfield，无存量系统迁移。
- 部署：`mvn package` 产出单一可执行 jar（内嵌前端产物），内网服务器 `java -jar` 启动；需预先准备 MySQL 实例（部署机或独立数据库服务器），首次部署由管理员手工执行建表脚本（`db/schema.sql`）与字典种子初始化脚本（`db/seed-dict.sql`）。

## Open Questions

- 图书馆实际借期与借阅上限政策：已按 60 天/10 册的配置默认值落地，政策确认后仅改配置，不影响本设计。
- 整理人员考核的具体统计口径（如归还到上架的间隔阈值）：上架记录已包含所需数据，具体报表留作后续 change。
- 表结构演进是否引入 Flyway：MVP 用手工建表脚本（`db/schema.sql`），结构变更频繁或需要可回放迁移时再引入 Flyway。
