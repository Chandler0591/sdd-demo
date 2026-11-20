# 图书管理系统（MVP）

一个 Web 全栈的图书馆管理 MVP，覆盖图书编目、读者管理、借阅流通、归还后上架确认与图书位置查询、系统字典维护等核心场景。

- **后端**：Spring Boot 3.4 + Java 17 + easy-query 3.0.33 + MySQL 8（`backend/`）
- **前端**：Vue 3 + Vite + TypeScript + Element Plus（`frontend/`）
- **交付形态**：前端构建产物嵌入后端 jar，由 Spring Boot 静态托管，单进程运行

## 1. 环境要求

| 依赖 | 版本要求 | 说明 |
| --- | --- | --- |
| JDK | 17 | `java -version` 必须显示 17（若系统默认 JDK 为 8，请使用 JDK 17 的完整路径执行 `java`/`mvn`） |
| Maven | 3.6+ | |
| Node.js | 18+（含 npm） | 仅构建前端时需要 |
| MySQL | 8.x | 需 utf8mb4 字符集 |

## 2. MySQL 环境准备（首次部署）

1. 创建业务库与测试库（utf8mb4）：

   ```sql
   CREATE DATABASE library      DEFAULT CHARACTER SET utf8mb4;
   CREATE DATABASE library_test DEFAULT CHARACTER SET utf8mb4;
   ```

2. 执行建表脚本（五张表：book / reader / loan / shelving_record / dict，含唯一约束；脚本可重复执行）：

   ```bash
   cd backend
   mysql -uroot -p library < db/schema.sql
   ```

3. 执行字典种子数据脚本（首次部署手工执行，插入图书分类字典；务必携带 `--default-character-set=utf8mb4` 避免中文乱码）：

   ```bash
   mysql -uroot -p --default-character-set=utf8mb4 library < db/seed-dict.sql
   ```

   种子数据包含两棵树：「图书分类」（category → 文学 / 科学 / 技术 / 历史 / 艺术，也可在管理界面自行增删）与「书架位置」（shelf-location → A 区 / B 区，供上架确认的位置下拉选择）。

## 3. 配置项说明

配置集中在 `backend/src/main/resources/application.yml`：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/library` | 数据库连接（用户名/密码同步修改） |
| `library.loan-days` | `60` | 借期天数，借出时 `due_at = 借出时间 + 借期` |
| `library.max-active-loans` | `10` | 读者同时在借上限，超出后借出接口拒绝 |
| `library.snowflake.worker-id` | `1` | 雪花 ID 机器号；**多实例部署时每台机器必须不同**，否则可能产生重复主键 |
| `library.upload-dir` | `uploads/shelving/` | 上架照片本地存储目录（相对运行目录）；照片以固定 Web 路径 `/uploads/<文件名>` 对外提供访问（`/uploads/**` 静态映射），前端直接用于预览；当前为本地文件存储，可平滑演进为对象存储 |

## 4. 启动方式

### 4.1 开发模式（前后端分离）

后端（端口 8080）：

```bash
cd backend
mvn spring-boot:run
```

前端（Vite dev server，已配置 `/api` 代理到 8080）：

```bash
cd frontend
npm install
npm run dev
```

浏览器访问 Vite 输出的地址（默认 http://localhost:5173）。

### 4.2 单 jar 交付模式（生产推荐）

1. 构建前端产物：

   ```bash
   cd frontend
   npm install
   npm run build
   ```

2. 打包后端（`pom.xml` 已配置把 `frontend/dist` 复制进 jar 的 `static/`，由 Spring Boot 静态托管）：

   ```bash
   cd backend
   mvn package -DskipTests
   ```

3. 单进程启动：

   ```bash
   java -jar target/library-backend-0.1.0.jar
   ```

浏览器访问 http://localhost:8080 即可使用完整系统（页面与 API 同源，无需 Nginx 或前端进程）。

## 5. 运行测试

集成测试使用 `library_test` 库（`@ActiveProfiles("test")`），每次用例执行前会重建表结构：

```bash
cd backend
mvn test
```
