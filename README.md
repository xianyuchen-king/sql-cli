# SQL-CLI

一个用于连接关系型数据库（MySQL、Oracle、PostgreSQL、SQLite、达梦等）的 Java CLI 工具，通过 JDBC 驱动支持多数据库。专为 AI 编程助手（如 Claude Code）设计。

## 特性

- **多数据库支持** - MySQL、Oracle、PostgreSQL、SQLite、达梦等
- **安全保护** - 支持密码加密、安全级别控制（严格/正常/无）
- **数据导入导出** - 支持 CSV、JSON、SQL INSERT/UPDATE 格式
- **连接管理** - 保存连接配置，支持分组和标签
- **元数据浏览** - 查看数据库、表、列、索引信息
- **AI 友好** - 专为 Claude Code 等 AI 助手优化的输出格式

## 安装

### 通过 npm 安装（推荐）

```bash
npm install -g @cyq/sql-cli
```

### 从源码安装

```bash
git clone https://github.com/cyq/sql-cli.git
cd sql-cli
./gradlew shadowJar
```

## 快速开始

```bash
# 初始化配置
sql-cli init

# 安装 MySQL 驱动
sql-cli driver install mysql

# 添加连接
sql-cli conn add --name mydb --type mysql --host localhost --port 3306 --user root --password xxx --db testdb

# 测试连接
sql-cli conn test mydb -v

# 执行查询
sql-cli query "SELECT * FROM users LIMIT 10" -c mydb
```

## 完整文档

详见 [sql-cli-skill.md](skill/sql-cli-skill.md)

## 技术栈

- Java 21
- Gradle
- Picocli（CLI 框架）
- Jackson（YAML/JSON 处理）
- JDBC

## License

MIT
