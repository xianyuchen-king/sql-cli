# sql-cli 设计文档

## 概述

sql-cli 是一个基于 Java 21 + JDBC 的数据库命令行工具，为 AI 编码工具（如 Claude Code）提供数据库交互能力。通过 CLI 命令调用，支持连接管理、数据查询、元数据浏览、DDL 执行、数据导入导出等操作。

## 核心定位

- 纯 CLI 工具，AI 智能体通过 Bash 调用
- 配备 Claude Code Skill，覆盖安装、使用、更新、卸载全生命周期
- 支持所有常见关系型数据库，非常见数据库通过用户提供驱动 jar 扩展

## 1. 整体架构

```
┌─────────────────────────────┐
│  CLI 层 (Picocli)           │  命令解析、参数校验、输出格式化
├─────────────────────────────┤
│  安全层                     │  危险操作拦截、行数保护、确认提示
├─────────────────────────────┤
│  连接管理层                 │  配置 CRUD、分组标签、连接测试
├─────────────────────────────┤
│  执行层 (JDBC)              │  查询、DDL、元数据、导入导出
├─────────────────────────────┤
│  方言层                     │  URL 构建、LIMIT 追加、元数据补充
├─────────────────────────────┤
│  驱动加载层                 │  URLClassLoader 动态加载 drivers/ 目录 jar
└─────────────────────────────┘
```

### 目录结构

```
~/.sql-cli/
├── config.yml          # 连接配置（密码加密存储）
├── drivers/            # JDBC 驱动 jar（用户指定目录）
└── logs/               # 日志
```

## 2. 命令体系

命令结构：`sql-cli <command> [subcommand] [options]`

### 连接管理命令

| 命令 | 说明 |
|------|------|
| `sql-cli conn add` | 交互式添加连接 |
| `sql-cli conn list` | 列出所有连接（支持按分组/标签过滤） |
| `sql-cli conn show <name>` | 查看连接详情（密码脱敏） |
| `sql-cli conn update <name>` | 修改连接配置 |
| `sql-cli conn remove <name>` | 删除连接 |
| `sql-cli conn test <name>` | 测试连接是否可用 |
| `sql-cli conn group list` | 列出所有分组 |
| `sql-cli conn tag list` | 列出所有标签 |
| `sql-cli conn types` | 列出所有已注册数据库类型（内置+自定义） |
| `sql-cli conn register-type` | 注册自定义数据库类型 |

### 数据操作命令

| 命令 | 说明 |
|------|------|
| `sql-cli query <sql>` | 执行 DML，返回格式化结果 |
| `sql-cli exec <sql>` | 执行 DDL/DML，返回影响行数 |
| `sql-cli meta dbs` | 列出所有数据库/schema |
| `sql-cli meta tables [-d <db>]` | 列出表 |
| `sql-cli meta columns -t <table>` | 列出字段信息 |
| `sql-cli meta indexes -t <table>` | 列出索引 |
| `sql-cli meta views [-d <db>]` | 列出视图 |
| `sql-cli export` | 导出数据（支持 csv/json/insert/update） |
| `sql-cli import` | 导入数据（支持 csv/json/insert/update） |

### 驱动管理命令

| 命令 | 说明 |
|------|------|
| `sql-cli driver install <type> [--version]` | 下载安装驱动 |
| `sql-cli driver list` | 列出已安装驱动 |
| `sql-cli driver available` | 列出可安装驱动 |
| `sql-cli driver remove <jar>` | 移除驱动 |

### 其他命令

| 命令 | 说明 |
|------|------|
| `sql-cli init` | 初始化配置 |
| `sql-cli update check` | 检查新版本 |
| `sql-cli update` | 执行更新 |
| `sql-cli uninstall` | 卸载清理 |

### 连接指定方式

```bash
# 方式1：通过别名引用已保存的连接
sql-cli query "SELECT 1" -c dev-mysql

# 方式2：命令行直接传入连接信息
sql-cli query "SELECT 1" --type mysql --host localhost --port 3306 --user root --password xxx --db testdb

# 方式3：直接传 JDBC URL（兜底）
sql-cli query "SELECT 1" --url "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=...))"
```

## 3. 安全层

### 危险操作分级

| 级别 | 操作 | 处理方式 |
|------|------|----------|
| **禁止** | `DROP DATABASE`、`DROP SCHEMA`、无 WHERE 的 `DELETE`/`UPDATE` | 拒绝执行 |
| **高危** | `DROP TABLE`、`TRUNCATE`、`ALTER TABLE DROP` | 需 `--confirm` 参数 |
| **警告** | `ALTER TABLE`（非 DROP）、批量 INSERT（>1000行） | 输出警告，允许执行 |
| **正常** | `SELECT`、`CREATE TABLE`、有 WHERE 的 `DELETE`/`UPDATE` | 直接执行 |

### 查询行数保护

- `autoLimit: true` 时自动检测 SELECT 是否有行数限制
- 无限制 → 方言层追加对应语法（MySQL: `LIMIT 500`，Oracle: `FETCH FIRST 500 ROWS ONLY`）
- 非常见数据库追加失败时提醒用户手动处理
- 用户可通过 `--limit` 手动指定，或 `--no-limit` 关闭（触发警告）
- 结果末尾提示截断信息

### 连接级别安全配置

```yaml
connections:
  - name: prod-mysql
    safetyLevel: strict    # strict=仅允许 SELECT 和元数据查询
                           # normal=默认分级策略
                           # none=不做拦截（慎用）
```

## 4. 配置文件设计

### config.yml 结构

```yaml
# 全局默认配置
defaults:
  maxRows: 500
  autoLimit: true
  safetyLevel: normal
  outputFormat: markdown
  driverDir: ~/.sql-cli/drivers

# 加密密钥来源
encryption:
  keyEnv: SQL_CLI_SECRET

# 连接分组
groups:
  - name: 开发环境
  - name: 生产环境

# 用户注册的自定义数据库类型
customTypes:
  - name: dm
    driverClass: dm.jdbc.driver.DmDriver
    driver: DmJdbcDriver18.jar
    urlTemplate: "jdbc:dm://{host}:{port}/{db}"
    defaultPort: 5236

# 连接列表
connections:
  # 简化参数（常见数据库）
  - name: dev-mysql
    group: 开发环境
    tags: [mysql, 开发]
    type: mysql
    driver: mysql-connector-j-8.3.0.jar
    host: localhost
    port: 3306
    user: root
    password: ENC(AQCG7b3...)
    db: testdb
    params:
      useSSL: false
      charset: utf8mb4
    safetyLevel: normal

  # 直接 URL（非常见数据库）
  - name: prod-dm
    group: 生产环境
    tags: [dm, 生产]
    type: dm
    host: 192.168.1.100
    port: 5236
    db: SYSDBA
    user: SYSDBA
    password: ENC(xxx...)

  # SQLite
  - name: local-sqlite
    group: 开发环境
    tags: [sqlite, 本地]
    type: sqlite
    driver: sqlite-jdbc-3.45.1.jar
    db: /data/mydata.db
```

### 密码加密

- 算法：AES-256
- 密钥来源：环境变量 `SQL_CLI_SECRET`
- 密码格式：`ENC(Base64编码密文)`
- 首次使用无密钥时自动生成并提示保存到环境变量
- `conn show` 显示时密码脱敏为 `ENC(***)`

## 5. 方言层与驱动管理

### 方言接口

```java
interface Dialect {
    String buildUrl(ConnectionConfig config);
    String wrapLimit(String sql, int maxRows);
    List<TableMeta> listTables(Connection conn, String schema);  // 可选覆盖
}
```

### 内置方言

| 方言 | URL 模板 | LIMIT 语法 |
|------|---------|-----------|
| MysqlDialect | `jdbc:mysql://{host}:{port}/{db}?{params}` | `LIMIT N` |
| OracleDialect | `jdbc:oracle:thin:@//{host}:{port}/{db}` | `FETCH FIRST N ROWS ONLY` / `ROWNUM` |
| PostgresqlDialect | `jdbc:postgresql://{host}:{port}/{db}?{params}` | `LIMIT N` |
| SqliteDialect | `jdbc:sqlite:{db}` | `LIMIT N` |
| GenericDialect | 不构建，直接用用户 URL | 不追加，提醒用户 |

### type 与默认 driverClass 映射

| type | 默认 driverClass |
|------|-----------------|
| mysql | `com.mysql.cj.jdbc.Driver` |
| oracle | `oracle.jdbc.OracleDriver` |
| postgresql | `org.postgresql.Driver` |
| sqlite | `org.sqlite.JDBC` |
| generic / 自定义类型 | 必须指定 `driverClass` |

### 驱动管理

- **所有驱动均为外部管理**，不内置任何驱动
- 驱动目录由用户配置（`defaults.driverDir`），默认 `~/.sql-cli/drivers/`
- 通过 `sql-cli driver install <type>` 从 Maven Central 下载常见驱动
- 非常见驱动用户手动放入目录
- 通过 `URLClassLoader` 动态加载，按连接配置中指定的 jar 文件名匹配

### 自定义数据库类型注册

首次使用非常见数据库后，通过 `conn register-type` 注册：

```bash
sql-cli conn register-type \
  --name dm \
  --driver DmJdbcDriver18.jar \
  --driverClass dm.jdbc.driver.DmDriver \
  --urlTemplate "jdbc:dm://{host}:{port}/{db}" \
  --defaultPort 5236
```

注册后使用体验与内置类型一致。

## 6. 数据导入导出

统一支持四种格式：CSV、JSON、INSERT、UPDATE。

### 导出

```bash
sql-cli export -t users -c dev-mysql -f csv -o /tmp/users.csv
sql-cli export -t users -c dev-mysql -f json -o /tmp/users.json
sql-cli export -t users -c dev-mysql -f insert -o /tmp/users.sql
sql-cli export -t users -c dev-mysql -f update --where-columns id -o /tmp/users_update.sql
sql-cli export --all-tables -c dev-mysql -d testdb -o /tmp/backup/
```

- UPDATE 格式默认用主键作为 WHERE 条件，可通过 `--where-columns` 指定
- 不指定 `-o` 则输出到 stdout

### 导入

```bash
sql-cli import -t users -c dev-mysql -f /tmp/users.csv
sql-cli import -t users -c dev-mysql -f /tmp/users.sql --format insert
sql-cli import -t users -c dev-mysql -f /tmp/users_update.sql --format update
sql-cli import -t users -c dev-mysql -f /tmp/users.csv --batch-size 1000
sql-cli import -t users -c dev-mysql -f /tmp/users.csv --on-error skip
```

- CSV：首行为列名，自动与目标表列匹配
- 大文件自动分批，使用 JDBC batch 提交
- 默认 `--on-error abort`，可选 `skip` 跳过错误行
- 完成后输出摘要：`[完成] 成功 1500 行，失败 3 行，跳过 0 行`

## 7. 输出格式

查询结果支持三种输出格式：

- **Markdown 表格**（默认）：AI 友好
- **JSON**：程序解析友好
- **CSV**：数据交换友好

通过 `-f` 参数切换，全局默认在 `config.yml` 中配置。

元数据输出同样使用 Markdown 表格，包含表名、类型、行数、字段详情等信息。

## 8. Claude Code Skill

Skill 覆盖 sql-cli 全生命周期：

| 职责 | 说明 |
|------|------|
| 安装 | 检测安装状态，引导下载安装 |
| 配置 | 初始化 config.yml，引导驱动目录和加密密钥配置 |
| 驱动管理 | 安装常见驱动，提供非常见驱动安装指导 |
| 使用指导 | 告诉 AI 如何调用各类命令 |
| 更新 | 检测新版本，引导更新 |
| 卸载 | 清理安装文件和配置 |

Skill 安装方式：用户通过 claude CLI 安装 skill 文件。

## 9. 技术栈与项目结构

### 技术栈

| 组件 | 选型 |
|------|------|
| 语言 | Java 21 |
| 构建工具 | Gradle |
| CLI 框架 | Picocli |
| 日志 | SLF4J + Logback |
| 加密 | JDK 内置 javax.crypto (AES-256) |
| 测试 | JUnit 5 + Mockito |
| 打包 | Gradle Shadow (fat jar) |

### 项目结构

```
sql-cli/
├── build.gradle
├── settings.gradle
├── src/
│   ├── main/java/com/sqlcli/
│   │   ├── SqlCliApp.java
│   │   ├── cli/                          # CLI 命令定义
│   │   │   ├── SqlCliCommand.java
│   │   │   ├── QueryCommand.java
│   │   │   ├── ExecCommand.java
│   │   │   ├── MetaCommand.java
│   │   │   ├── ExportCommand.java
│   │   │   ├── ImportCommand.java
│   │   │   ├── ConnCommand.java
│   │   │   ├── DriverCommand.java
│   │   │   └── InitCommand.java
│   │   ├── config/                       # 配置管理
│   │   │   ├── AppConfig.java
│   │   │   ├── ConnectionConfig.java
│   │   │   ├── ConfigManager.java
│   │   │   └── EncryptionService.java
│   │   ├── connection/                   # 连接管理
│   │   │   ├── ConnectionManager.java
│   │   │   └── DriverLoader.java
│   │   ├── dialect/                      # 方言层
│   │   │   ├── Dialect.java
│   │   │   ├── MysqlDialect.java
│   │   │   ├── OracleDialect.java
│   │   │   ├── PostgresqlDialect.java
│   │   │   ├── SqliteDialect.java
│   │   │   └── GenericDialect.java
│   │   ├── safety/                       # 安全层
│   │   │   ├── SqlAnalyzer.java
│   │   │   └── SafetyGuard.java
│   │   ├── executor/                     # 执行层
│   │   │   ├── QueryExecutor.java
│   │   │   ├── MetaExecutor.java
│   │   │   └── TransferExecutor.java
│   │   ├── output/                       # 输出格式化
│   │   │   ├── OutputFormatter.java
│   │   │   ├── MarkdownFormatter.java
│   │   │   ├── JsonFormatter.java
│   │   │   └── CsvFormatter.java
│   │   └── util/
│   │       └── VersionUtils.java
│   └── test/java/com/sqlcli/
│       ├── cli/
│       ├── safety/
│       ├── dialect/
│       └── ...
├── skill/
│   └── sql-cli-skill.md
└── README.md
```
