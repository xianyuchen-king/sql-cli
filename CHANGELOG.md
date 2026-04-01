# Changelog

## [1.1.0] - 2026-04-01

### 新增
- `explore` 命令：一次调用探索数据库结构，概览模式（schema 列表）+ 详细模式（表+列）
- 声明式方言配置：`conn register-type` 支持 6 个方言行为字段（`--limit-suffix`、`--limit-prefix`、`--limit-pattern`、`--database-label`、`--list-databases-method`、`--system-schema-filter`），无需写 Java 即可接入小众数据库
- `conn remove-type` 命令：删除自定义数据库类型，含连接引用保护
- Oracle `meta dbs` 适配：OracleDialect 覆写 `listDatabases()` 使用 `getSchemas()`，返回 Schema 列表而非空结果
- Dialect 接口新增 `getDatabaseLabel()` 和 `listDatabases()` 默认方法
- ErrorCode 新增 `PERMIT_DENIED`、`SCHEMA_NOT_FOUND`

### 改进
- SQL 错误码校准：遍历 SQLException cause 链，新增方言特定模式（DM 中文、Oracle ORA、PostgreSQL、MSSQL），缩窄 config 匹配
- 密码加密 WARN 一次性提醒：同 JVM 生命周期内只输出一次
- Markdown 表格列宽截断：超过 50 字符的值自动截断显示（47 + "..."）
- 查询计时位置：Markdown 格式下计时信息显示在表格之前，CSV 格式保持表格之后
- Shell 模式集成 Dialect：`\db` 反斜杠命令使用方言特定的 listDatabases 实现

### 测试
- 新增 CliErrorHandlerTest（35 用例，含 cause 链遍历和方言特定模式）
- 新增 CustomDialectTest（16 用例，声明式方言配置全覆盖）
- 新增 ExploreCommandTest（28 用例）
- 新增 QueryExecutorTest（6 用例，计时位置验证）
- 新增 MetaExecutorTest（3 用例）
- 新增 ConnRegisterTypeCommandTest（3 用例）
- 新增 ConnRemoveTypeCommandTest（5 用例，含连接引用保护）
- 新增 ConfigManagerEncryptionWarningTest（5 用例）
- 更新 OracleDialectTest（+listDatabases 测试）
- 更新 DialectFactoryTest（+自定义类型解析）
- 更新 ConfigManagerTest（+新字段 round-trip + 向后兼容）
- 更新 MarkdownFormatterTest（+截断边界测试）

### 文档
- CLAUDE.md：补充 `shell/` 包、拆分子命令类、交互式 Shell 设计模式
- Skill 文件：补充 shell 命令、register-type 方言字段、错误码、section 编号修正

## [1.0.5] - 2026-03-30

### 修复
- QueryCommand resolveConnection 异常捕获和结构化错误输出
- describe 命令 JSON 输出纯净度（columns/indexes/fk_count 嵌套在 data 内）
- meta 子命令参数统一（MetaConnectionMixin，-c/-f 子命令名前后均可）

## [1.0.4] - 2026-03-29

### 改进
- Agent-JSON 输出模式：统一 status/data/meta/warnings 结构化信封
- SafetyGuard warnings 收集到 list，JSON 模式写入信封，plaintext 模式走 stderr

## [1.0.3] - 2026-03-29

### 修复
- SafetyException 在 validate() try-catch 内捕获，确保结构化输出

## [1.0.2] - 2026-03-29

### 改进
- 错误码细化：classifyError 新增 8 种模式匹配
- 所有命令通过 CliErrorHandler 统一处理错误

## [1.0.1] - 2026-03-28

### 新增
- 查询超时 `--timeout` 参数，防止长时间运行的查询挂起
- 查询结果输出到文件 `-o/--output` 参数
- 查询执行计时显示（如 `5 rows in set (0.03s)`）
- `meta describe` 子命令，综合展示表结构（列、主键、索引、外键）
- SQL Server (`mssql`) 内置方言支持
- `update check` 命令：从 GitHub Releases 检查最新版本
- `update` 命令：自动下载并替换 jar 文件
- `Version` 类：统一版本号管理，从 JAR manifest 读取
- `DriverRegistry` 类：统一驱动元数据，消除重复硬编码
- JaCoCo 测试覆盖率插件

### 改进
- 版本号集中管理到 `gradle.properties`，Java 和 Gradle 统一读取
- 驱动元数据（版本、driver class）从 `DriverInstallCommand` 和 `DriverAvailableCommand` 提取到 `DriverRegistry`
- `bin/sql-cli.js` 错误提示区分 npm 和源码构建两种安装场景

### 测试
- 新增 SafetyGuardTest（安全门禁测试，覆盖率 89%）
- 新增 ConfigManagerTest（配置读写测试，覆盖率 85%）
- 新增 GenericDialectTest、PostgresqlDialectTest、MssqlDialectTest
- 整体测试覆盖从 16% 提升至重点模块 67-89%

### 修复
- README 使用示例中 `5 rows in set (0.03s)` 的计时显示现已真正实现
- `bin/sql-cli.js` 中 `@cyq/sql-cli` 引用更新为 `@black-cyq/sql-cli`

## [1.0.0] - 2026-03-28

### 新增
- 多数据库支持：MySQL、Oracle、PostgreSQL、SQLite、达梦
- 连接管理：添加、更新、删除、测试、重命名
- 密码加密（AES-256-GCM）
- 安全级别控制：严格（仅 SELECT）、正常、无
- 数据导出：CSV、JSON、SQL INSERT/UPDATE
- 数据导入：CSV、SQL
- 驱动管理：自动下载、本地注册
- 元数据浏览：数据库、表、列、索引
- npm 全局安装支持（`@black-cyq/sql-cli`）
- Claude Code Skill 集成
- GitHub Actions CI/CD（自动构建、测试、Release、npm 发布）
- Issue 模板（Bug 报告、功能建议）
- 贡献指南（CONTRIBUTING.md）
- 中英文 README

### 修复
- 驱动加载器 URLClassLoader 过早关闭问题
- conn add 非交互模式自动检测

## [0.1.0] - 2026-03-27

### 新增
- 项目初始化
- 基础 CLI 框架
- MySQL 连接支持
