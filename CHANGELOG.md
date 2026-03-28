# Changelog

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
