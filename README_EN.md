# SQL-CLI

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![GitHub Release](https://img.shields.io/github/v/release/xianyuchen-king/sql-cli?include_prereleases&display_name=tag)](https://github.com/xianyuchen-king/sql-cli/releases)
[![npm version](https://img.shields.io/npm/v/@black-cyq/sql-cli.svg)](https://www.npmjs.com/package/@black-cyq/sql-cli)
![Status](https://img.shields.io/badge/Status-Beta-orange.svg)

A CLI tool for connecting to relational databases via JDBC drivers. Supports MySQL, Oracle, PostgreSQL, SQLite, DM, and more. Designed for AI coding agents like Claude Code.

> **Beta** — This project is under active development. Feedback and [bug reports](https://github.com/xianyuchen-king/sql-cli/issues/new?template=bug_report.yml) are welcome!

English | [中文](README.md)

## Features

- **Multi-database** — MySQL, Oracle, PostgreSQL, SQLite, DM, and extensible via custom drivers
- **Security** — AES-256-GCM encrypted password storage, configurable safety levels (strict/normal/none)
- **Import/Export** — CSV, JSON, SQL INSERT/UPDATE formats
- **Connection management** — Saved configs with grouping and tagging
- **Metadata browsing** — Databases, tables, columns, indexes
- **AI-friendly** — Markdown table output by default, optimized for AI agents

## Installation

### Prerequisites

- Java 21+ installed and on PATH
- Node.js 16+ (only needed for npm installation)

### Option 1: npm (recommended)

```bash
npm install -g @black-cyq/sql-cli
```

### Option 2: Build from source

```bash
git clone https://github.com/xianyuchen-king/sql-cli.git
cd sql-cli
./gradlew shadowJar
```

The jar will be at `build/libs/sql-cli.jar`.

```bash
# Run directly
java -jar build/libs/sql-cli.jar --help

# Or create a launcher script
sudo tee /usr/local/bin/sql-cli << 'EOF'
#!/bin/bash
java -jar /path/to/sql-cli/build/libs/sql-cli.jar "$@"
EOF
sudo chmod +x /usr/local/bin/sql-cli
```

### Option 3: Download pre-built jar

Visit the [Releases](https://github.com/xianyuchen-king/sql-cli/releases) page to download the latest jar.

```bash
java -jar sql-cli.jar --help
```

## Quick Start

```bash
# 1. Initialize config
sql-cli init

# 2. Install a database driver
sql-cli driver install mysql

# 3. Add a connection
sql-cli conn add --name mydb --type mysql --host localhost --port 3306 \
  --user root --password xxx --db testdb

# 4. Test connection
sql-cli conn test mydb -v

# 5. Run a query
sql-cli query "SELECT * FROM users LIMIT 10" -c mydb

# 6. Export data
sql-cli export -t users -c mydb -f csv -o /tmp/users.csv
```

## Usage Example

```
$ sql-cli query "SELECT id, name, email FROM users LIMIT 5" -c mydb

| id | name     | email              |
|----|----------|--------------------|
| 1  | Alice    | alice@example.com  |
| 2  | Bob      | bob@example.com    |
| 3  | Charlie  | charlie@example.com |

5 rows in set (0.03s)
```

See [full documentation](skill/sql-cli-skill.md) for more details.

## Supported Databases

| Database   | Type ID        | Default Port |
|-----------|----------------|-------------|
| MySQL     | `mysql`        | 3306        |
| Oracle    | `oracle`       | 1521        |
| PostgreSQL| `postgresql`   | 5432        |
| SQLite    | `sqlite`       | -           |
| DM (达梦)  | `dm` (custom)  | 5236        |
| Others    | `generic`      | -           |

## Tech Stack

Java 21, Gradle, Picocli (CLI), SnakeYAML (YAML), JDBC

## Contributing

Contributions, bug reports, and feature requests are welcome! See [Contributing Guide](CONTRIBUTING.md).

## Feedback

- [Report a Bug](https://github.com/xianyuchen-king/sql-cli/issues/new?template=bug_report.yml)
- [Request a Feature](https://github.com/xianyuchen-king/sql-cli/issues/new?template=feature_request.yml)
- [Discussions](https://github.com/xianyuchen-king/sql-cli/discussions)

## License

[MIT](LICENSE)
