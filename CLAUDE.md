# CLAUDE.md — sql-cli 项目上下文

## 项目概述

sql-cli 是一个基于 JDBC 的多数据库 CLI 工具，支持 MySQL、Oracle、PostgreSQL、SQL Server、SQLite、达梦等数据库。专为 AI 编程助手（如 Claude Code）设计。

## 项目状态

**Beta 测试阶段**。npm 包 `@black-cyq/sql-cli`，GitHub 仓库 `xianyuchen-king/sql-cli`。
CI：打 tag 自动构建、测试、创建 GitHub Release、发布 npm。

## 技术栈

- Java 21 + Gradle (shadow jar)
- Picocli 4.7.6（CLI 框架）
- SnakeYAML 2.3（配置文件）
- JUnit 5 + Mockito（测试）
- JaCoCo（覆盖率）
- 版本号管理：`gradle.properties` → `build.gradle` → JAR manifest → `Version.java`

## 构建命令

```bash
./gradlew shadowJar           # 构建 fat jar → build/libs/sql-cli.jar
./gradlew test                # 运行测试
./gradlew jacocoTestReport    # 生成覆盖率报告 → build/reports/jacoco/
```

## 代码结构

```
src/main/java/com/sqlcli/
├── SqlCliApp.java              # 入口，Picocli @Command 顶层命令
├── Version.java                # 版本号，从 manifest 读取，实现 IVersionProvider
├── cli/                        # CLI 命令层，每个命令一个类
│   ├── InitCommand             # sql-cli init
│   ├── ConnCommand (group)     # sql-cli conn add/test/list/remove/update/show
│   ├── QueryCommand            # sql-cli query "SQL" -c name [--timeout] [-o file]
│   ├── ExecCommand             # sql-cli exec "SQL" -c name [--confirm]
│   ├── MetaCommand (group)     # sql-cli meta dbs/tables/columns/indexes/views/describe
│   ├── ExportCommand           # sql-cli export -t table -c name -f csv/json/insert
│   ├── ImportCommand           # sql-cli import -t table -c name -f file
│   ├── DriverCommand (group)   # sql-cli driver install/list/remove/available
│   └── UpdateCommand           # sql-cli update / update check
├── config/
│   ├── AppConfig               # 配置模型（defaults, connections, customTypes, groups）
│   ├── ConnectionConfig        # 单个连接的配置模型
│   ├── CustomTypeConfig        # 自定义数据库类型配置
│   ├── ConfigManager           # 读写 ~/.sql-cli/config.yml，内存缓存
│   └── EncryptionService       # AES-256-GCM 密码加密，密钥从环境变量 SQL_CLI_SECRET 读取
├── connection/
│   ├── ConnectionManager       # JDBC 连接创建，使用 URLClassLoader 加载驱动 jar
│   └── DriverLoader            # 驱动 jar 加载、缓存、列出、删除
├── dialect/
│   ├── Dialect                 # 接口：buildUrl, wrapLimit, hasLimit, getDefaultPort, getDefaultDriverClass
│   ├── DialectFactory          # 解析逻辑：内置 → 自定义类型 → GenericDialect
│   ├── MysqlDialect / OracleDialect / PostgresqlDialect / SqliteDialect
│   ├── MssqlDialect            # SQL Server，使用 SELECT TOP N 行限制
│   └── GenericDialect          # 兜底方言，需要手动指定 JDBC URL
├── driver/
│   └── DriverRegistry          # 统一驱动元数据（groupId, artifactId, version, driverClass, port）
├── executor/
│   ├── QueryExecutor           # SELECT 执行，输出格式化，计时，超时
│   ├── MetaExecutor            # 元数据查询（DatabaseMetaData），含 describeTable
│   └── TransferExecutor        # 导入导出
├── output/
│   ├── OutputFormatter         # 接口 + 工厂方法
│   ├── MarkdownFormatter       # 默认，AI 友好的 Markdown 表格
│   ├── JsonFormatter           # JSON 数组输出
│   └── CsvFormatter            # CSV 输出
└── safety/
    ├── SqlAnalyzer             # SQL 解析，分类为 SAFE/WARNING/DANGEROUS/BLOCKED
    └── SafetyGuard             # 安全级别控制：strict(仅SELECT) / normal(默认) / none
```

## 关键设计模式

- **命令模式**：所有 CLI 命令 `implements Runnable`，使用 Picocli 注解
- **方言工厂**：`DialectFactory.getDialect(type, config)` 按优先级解析
- **驱动注册表**：`DriverRegistry` 集中管理所有驱动的 Maven 坐标和元数据
- **配置缓存**：`ConfigManager.load()` 首次读取后缓存在内存
- **版本管理**：`gradle.properties` → manifest `Implementation-Version` → `Version.java`
- **安全级别**：连接级可覆盖全局配置，strict/normal/none 三档
- **自动行数限制**：SELECT 默认限制 500 行，通过各 dialect 的 `wrapLimit()` 实现

## 配置文件位置

- 配置目录：`~/.sql-cli/`
- 配置文件：`~/.sql-cli/config.yml`
- 驱动目录：`~/.sql-cli/drivers/`
- 加密密钥环境变量：`SQL_CLI_SECRET`

## 添加新数据库支持

1. 在 `dialect/` 下创建方言类实现 `Dialect` 接口
2. 在 `DialectFactory` 的内置 map 中注册
3. 在 `DriverRegistry` 中添加驱动元数据
4. 添加 `src/test/java/com/sqlcli/dialect/` 下的测试

## 测试

- 测试目录：`src/test/java/com/sqlcli/`
- 已有测试：SqlAnalyzerTest, SafetyGuardTest, EncryptionServiceTest, ConfigManagerTest
- 方言测试：{Mysql,Oracle,Postgresql,Sqlite,Mssql,Generic}DialectTest, DialectFactoryTest
- 输出测试：{Csv,Json,Markdown}FormatterTest
- 运行：`./gradlew test`
- 覆盖率：重点模块 67-89%
