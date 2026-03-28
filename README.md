# SQL-CLI

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![GitHub Release](https://img.shields.io/github/v/release/xianyuchen-king/sql-cli?include_prereleases&display_name=tag)](https://github.com/xianyuchen-king/sql-cli/releases)
[![npm version](https://img.shields.io/npm/v/@black-cyq/sql-cli.svg)](https://www.npmjs.com/package/@black-cyq/sql-cli)
![Status](https://img.shields.io/badge/Status-Beta-orange.svg)

一个用于连接关系型数据库的 CLI 工具，通过 JDBC 驱动支持 MySQL、Oracle、PostgreSQL、SQLite、达梦等数据库。专为 AI 编程助手（如 Claude Code）设计。

> **Beta 测试阶段** — 本项目正在积极开发中，欢迎试用并[提出反馈](https://github.com/xianyuchen-king/sql-cli/issues/new?template=bug_report.yml)或[功能建议](https://github.com/xianyuchen-king/sql-cli/issues/new?template=feature_request.yml)！

[English](README_EN.md) | 中文

## 特性

- **多数据库支持** — MySQL、Oracle、PostgreSQL、SQLite、达梦等，可通过自定义驱动扩展
- **安全保护** — 密码 AES-256-GCM 加密存储、安全级别控制（严格/正常/无）
- **数据导入导出** — CSV、JSON、SQL INSERT/UPDATE 格式
- **连接管理** — 保存连接配置，支持分组和标签
- **元数据浏览** — 查看数据库、表、列、索引信息
- **AI 友好** — 默认 Markdown 表格输出，专为 AI 助手优化

## 安装

### 前置要求

- Java 21+ 已安装并在 PATH 中
- Node.js 16+（仅 npm 安装方式需要）

### 方式一：npm 安装（推荐）

```bash
npm install -g @black-cyq/sql-cli
```

安装完成后即可使用 `sql-cli` 命令。

### 方式二：从源码构建

```bash
git clone https://github.com/xianyuchen-king/sql-cli.git
cd sql-cli
./gradlew shadowJar
```

构建完成后，jar 文件位于 `build/libs/sql-cli.jar`。

```bash
# 直接运行
java -jar build/libs/sql-cli.jar --help

# 或创建启动脚本
sudo tee /usr/local/bin/sql-cli << 'EOF'
#!/bin/bash
java -jar /path/to/sql-cli/build/libs/sql-cli.jar "$@"
EOF
sudo chmod +x /usr/local/bin/sql-cli
```

### 方式三：下载预编译版本

前往 [Releases](https://github.com/xianyuchen-king/sql-cli/releases) 页面下载最新 jar 文件。

```bash
java -jar sql-cli.jar --help
```

## 快速开始

```bash
# 1. 初始化配置
sql-cli init

# 2. 安装数据库驱动
sql-cli driver install mysql

# 3. 添加连接
sql-cli conn add --name mydb --type mysql --host localhost --port 3306 \
  --user root --password xxx --db testdb

# 4. 测试连接
sql-cli conn test mydb -v

# 5. 执行查询
sql-cli query "SELECT * FROM users LIMIT 10" -c mydb

# 6. 导出数据
sql-cli export -t users -c mydb -f csv -o /tmp/users.csv
```

## 使用示例

```
$ sql-cli query "SELECT id, name, email FROM users LIMIT 5" -c mydb

| id | name     | email              |
|----|----------|--------------------|
| 1  | Alice    | alice@example.com  |
| 2  | Bob      | bob@example.com    |
| 3  | Charlie  | charlie@example.com |

5 rows in set (0.03s)
```

更多用法详见 [完整文档](skill/sql-cli-skill.md)。

## 支持的数据库

| 数据库     | 类型标识       | 默认端口 |
|-----------|---------------|---------|
| MySQL     | `mysql`       | 3306    |
| Oracle    | `oracle`      | 1521    |
| PostgreSQL| `postgresql`  | 5432    |
| SQLite    | `sqlite`      | -       |
| 达梦       | `dm`（自定义） | 5236    |
| 其他       | `generic`     | -       |

## 技术栈

- Java 21、Gradle、Picocli（CLI 框架）、SnakeYAML（YAML 处理）、JDBC

## 贡献

欢迎贡献代码、报告 Bug 或提出建议！请阅读 [贡献指南](CONTRIBUTING.md)。

## 反馈与建议

- [报告 Bug](https://github.com/xianyuchen-king/sql-cli/issues/new?template=bug_report.yml)
- [功能建议](https://github.com/xianyuchen-king/sql-cli/issues/new?template=feature_request.yml)
- [讨论区](https://github.com/xianyuchen-king/sql-cli/discussions)

## License

[MIT](LICENSE)
