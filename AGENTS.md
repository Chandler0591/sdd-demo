# AGENTS.md — 图书馆管理系统项目工作说明

给 AI Agent 的项目说明书：新会话 / 新任务开始前先读本文件，再动手。内容随开发持续更新，新踩的坑必须追加进「易踩坑清单」。

## 项目简介

图书馆图书管理系统 MVP（OpenSpec 驱动开发）：
- 后端：Spring Boot 3 + easy-query + MySQL 8（端口 8080）
- PC 端：Vue 3 + Element Plus（frontend/）
- 小程序：uni-app + Vue 3 微信小程序（miniapp/）

## 目录地图

| 目录 | 内容 | 规则 |
| --- | --- | --- |
| backend/ | Spring Boot 3 后端源码 | 可改代码 |
| backend/db/schema.sql | 数据库结构权威文件（幂等 DROP + CREATE） | **结构变更必须同步这里** |
| backend/src/test | 后端测试（52 个单测，@BeforeEach 用 schema.sql 重建测试库 library_test） | 可改，行为变更必须补测试 |
| frontend/ | Vue 3 PC 端 | 可改代码 |
| miniapp/ | uni-app 小程序（构建产物在 dist/build/mp-weixin） | 可改 src/ |
| openspec/ | OpenSpec 规划产物（changes/ 下按变更存放） | 只改文档，不手建 |
| scripts/ | 一键启停 / 打包 / 健康检查脚本（shell，Git Bash 与 WSL 均可运行） | 可改 |
| docs/ | 开发规范等文档 | 只改文档 |

## 环境与常用命令

### 关键环境（本机）

- **JDK 17**：`D:\myapp\Java\jdk-17`（PATH 上的 java 是 JDK 8，mvn / 打包必须显式指定 JAVA_HOME）
- **MySQL**：本地 3306，真实库 `library`、测试库 `library_test`（root / Ab#123456，mysql CLI 可用）
- 后端基地址：`http://127.0.0.1:8080`（真机 / 局域网联调用 `http://10.88.91.102:8080`）
- **千问 API Key**：只经环境变量 `DASHSCOPE_API_KEY` 注入，**禁止写入任何文件 / 仓库**
- **GitHub 推送需代理** `127.0.0.1:7897`（直连会被重置）：`git -c http.proxy=http://127.0.0.1:7897 push`

### 常用命令

**一键脚本（shell）：**

```bash
# 打包后端（自动先停运行中的实例，因为 jar 被进程锁定无法覆盖）
bash scripts/build-backend.sh

# 启动后端（带千问 Key；不传参数则读环境变量 DASHSCOPE_API_KEY；日志 backend/logs/backend.log）
bash scripts/start-backend.sh "sk-xxx"

# 停止后端 / 健康检查
bash scripts/stop-backend.sh
bash scripts/check.sh
```

> 脚本入口说明：本机 PowerShell 下 `bash` 解析到 WSL，脚本内已自动适配 JDK 路径（Git Bash 的 `/d/...` 与 WSL 的 `/mnt/d/...`）；Git Bash 中运行效果相同，启动脚本会自动选用合适方式脱离终端。

**手工命令（PowerShell；不支持 `&&`，用分号 `;`）：**

```powershell
# 跑后端测试
$env:JAVA_HOME = "D:\myapp\Java\jdk-17"
mvn -q -f backend\pom.xml test "-Dtest=BookApiTest,EntitySchemaTest,QwenBookInfoRecognizerTest"

# 构建小程序（终端 cwd 会漂移，先切目录）
Set-Location miniapp; npm run build:mp-weixin

# 手工调接口：一律 curl.exe（PowerShell 的 curl 是 Invoke-WebRequest 别名）
curl.exe -s http://127.0.0.1:8080/api/health
```

## 编码硬约束

### 强制

1. **分层单向**：web(Controller) → service → repository；entity 不直接返回给前端，一律映射 VO / DTO。
2. **同一本书判据（四要素）**：书名 + 出版社 + 作者 + 出版年份**全部一致**才算同一本书（`GET /api/books/match`、create 留空 ISBN 时兜底复用）。任何查重逻辑不得只按书名或 ISBN。
3. **DB 结构变更必须同步 `backend/db/schema.sql`**：测试每次从 schema.sql 重建测试库，不同步会导致测试大范围失败。
4. **敏感信息不落仓库**：API Key 走环境变量 `DASHSCOPE_API_KEY`；数据库密码目前明文在 application.yml（已知风险，仓库公开前需改环境变量注入）。
5. **行为变更必须补测试**；禁止删断言 / 跳测试换取绿色。
6. 千问识别模型固定 **`qwen3.6-flash`**（qwen-vl-max 已于 2026-10 下线）。
7. 测试 profile 继承主配置：测试环境用 mock 识别，必须在 `application-test.yml` 显式 `provider: mock`。

### 推荐

1. 注释中文，密度与周围代码一致；公共 API 方法写一行 Javadoc。
2. 用户可见错误用 `BusinessException` + 友好中文提示，不裸抛异常。
3. 新增 REST 接口在 BookApiTest（或对应模块测试）补集成测试。

## 易踩坑清单（本机实战，勿重复踩）

1. PowerShell 的 `curl` 是 Invoke-WebRequest 别名 → 一律 `curl.exe`。
2. PowerShell 不支持 `&&` → 用分号 `;`。
3. `mvn "-Dtest=A,B"` 多类名要加引号，否则被 PowerShell 拆参。
4. 运行中的后端锁定 jar → repackage 失败；先 `bash scripts/stop-backend.sh`（按 jar 名匹配 `library-backend`，避免误杀 IntelliJ 编译进程）。
5. 中文 JSON 写文件用 `[System.IO.File]::WriteAllText($path, $s, (New-Object System.Text.UTF8Encoding($false)))`；`Out-File -Encoding ascii` 会乱码（?????）。
6. schema.sql 缺列 → 测试库重建后 `Unknown column` 大范围失败（ALTER 真实库 ≠ 改 schema.sql）。
7. 小程序真机请求局域网 IP 被域名校验拦截 → 开发者工具「详情 → 本地设置 → 勾选不校验合法域名」，然后**重新编译并重新生成预览二维码**（旧二维码不带调试标记）。
8. 手机预览调试开关：右上角「…」→「开发调试」→「打开调试」。
9. npm 命令前先 `Set-Location miniapp`（终端 cwd 会漂移）。
10. 构建小程序后产物在 `miniapp/dist/build/mp-weixin`，微信开发者工具指向该目录重新编译。
11. WSL 下 mvn 脚本校验 `$JAVA_HOME/bin/java` 失败（DrvFs 不解析无 .exe 后缀的文件），且经 cmd.exe 调 mvn.cmd 会按错误字符集编译中文源码 → 用 powershell 调 mvn.cmd（见 scripts/build-backend.sh）。
12. WSL 的 PATH 上没有 powershell.exe（在 `WindowsPowerShell\v1.0` 子目录），脚本中需用完整路径 `/mnt/c/Windows/System32/WindowsPowerShell/v1.0/powershell.exe`。

## 权威文件索引

| 查什么 | 看哪里 |
| --- | --- |
| 数据库结构 | backend/db/schema.sql |
| 主配置（端口 / 数据源 / 千问） | backend/src/main/resources/application.yml |
| 测试配置（mock 识别） | backend/src/test/resources/application-test.yml |
| API 契约 | backend/src/main/java/com/library/web/**（7 个 Controller，含 GET /api/health） |
| 小程序 API 封装 | miniapp/src/utils/api.js |
| 开发规范 | docs/开发规范.md |
| 变更规划 | openspec/changes/<change-name>/ |
