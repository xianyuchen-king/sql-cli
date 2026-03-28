---
name: sql-cli
description: Database CLI tool for connecting to relational databases (MySQL, Oracle, PostgreSQL, SQLite, SQL Server, etc.) via JDBC. Provides query execution, metadata browsing, data import/export, and connection management.
type: user
---

# SQL Database Tool (sql-cli)

## Prerequisites

- **Java 21+** must be installed and on PATH. sql-cli checks this on startup.
- **Node.js 16+** required for npm installation (not needed at runtime).

## Installation & Setup

### Check Installation
Run `sql-cli --version` to check if sql-cli is installed.

### Install via npm (Recommended)
```bash
npm install -g @black-cyq/sql-cli
```

After installation, `~/.sql-cli/` directory is auto-created with `drivers/` subdirectory.

### Install from source (development)
1. Clone the repository and build:
   ```bash
   git clone https://github.com/xianyuchen-king/sql-cli.git
   cd sql-cli
   ./gradlew shadowJar
   ```
2. Run directly:
   ```bash
   java -jar build/libs/sql-cli.jar --help
   ```
3. Run `sql-cli init` to initialize configuration
4. Set the encryption key environment variable (shown during init)

### Uninstall
```bash
npm uninstall -g @black-cyq/sql-cli
sql-cli uninstall --confirm    # Removes ~/.sql-cli/ entirely
```

### Initialize Configuration
```bash
sql-cli init
```
This creates `~/.sql-cli/config.yml` and `~/.sql-cli/drivers/`.

### Install JDBC Drivers
```bash
# Install common database drivers (downloads from Maven Central)
sql-cli driver install mysql
sql-cli driver install oracle
sql-cli driver install postgresql
sql-cli driver install sqlite
sql-cli driver install mssql

# List installed drivers
sql-cli driver list

# List available drivers
sql-cli driver available

# Install specific version
sql-cli driver install mysql --version 5.1.49
```

For uncommon databases, manually place the JDBC driver jar in `~/.sql-cli/drivers/` and register the type:
```bash
sql-cli conn register-type \
  --name dm \
  --driver DmJdbcDriver18.jar \
  --driver-class dm.jdbc.driver.DmDriver \
  --url-template "jdbc:dm://{host}:{port}/{db}" \
  --default-port 5236
```

### Update
```bash
sql-cli update check    # Check for new version
sql-cli update          # Update to latest version
```

## Usage

### 1. Check existing connections
```bash
sql-cli conn list
```

### 2. Add a connection
```bash
# Non-interactive
sql-cli conn add --name mydb --type mysql --host localhost --port 3306 --user root --password xxx --db testdb --driver mysql-connector-j-8.3.0.jar

# SQL Server
sql-cli conn add --name mssqldb --type mssql --host localhost --port 1433 --user sa --password xxx --db mydb --driver mssql-jdbc-12.4.2.jre11.jar

# Direct JDBC URL (for uncommon databases)
sql-cli conn add --name mydb --type generic --url "jdbc:xxx://host:port/db" --user admin --password xxx --driver MyDriver.jar --driver-class com.example.Driver
```

### 3. Test a connection
```bash
sql-cli conn test mydb        # Basic test
sql-cli conn test mydb -v     # Verbose (shows JDBC URL and details)
```

### 4. Execute queries
```bash
# SELECT query (returns Markdown table by default, with timing)
sql-cli query "SELECT * FROM users LIMIT 10" -c mydb

# Change output format
sql-cli query "SELECT * FROM users" -c mydb -f json
sql-cli query "SELECT * FROM users" -c mydb -f csv

# Custom row limit
sql-cli query "SELECT * FROM users" -c mydb --limit 100
sql-cli query "SELECT * FROM users" -c mydb --no-limit

# Query timeout (seconds)
sql-cli query "SELECT * FROM large_table" -c mydb --timeout 30

# Save results to file
sql-cli query "SELECT * FROM users" -c mydb -o /tmp/result.csv

# Direct connection (without saved config)
sql-cli query "SELECT 1" --type mysql --host localhost --port 3306 --user root --password xxx --db testdb --driver mysql-connector-j-8.3.0.jar
```

### 5. Interactive Shell (REPL)

For frequent queries, use the interactive shell to reuse connections and improve response speed:

```bash
# Start shell with saved connection
sql-cli shell -c mydb

# Start shell with direct connection parameters
sql-cli shell --type mysql --host localhost --port 3306 --user root --password xxx --db testdb
```

**Slash Commands:**

| Command | Description |
|---------|-------------|
| `\dt`, `\tables` | List all tables |
| `\d <table>` | Describe table structure |
| `\db`, `\databases` | List all databases |
| `\dv`, `\views` | List all views |
| `\c <name>` | Switch to another connection |
| `\format [markdown\|json\|csv]` | Set output format |
| `\limit <n>` | Set max rows |
| `\nolimit` | Disable row limit |
| `\timeout <n>` | Set query timeout (seconds) |
| `\status` | Show current settings |
| `\help` | Show help |
| `exit`, `quit` | Exit shell |

**Example Shell Session:**

```
$ sql-cli shell -c mydb

sql-cli shell v1.0.4
Connected to: mysql://localhost:3306/testdb
Type '\help' for available commands, 'exit' or Ctrl+D to exit.

mydb> SELECT * FROM users LIMIT 5;
| id | name  | email             |
|----|-------|-------------------|
| 1  | Alice | alice@example.com |

5 rows in set (0.02s)

mydb> \d users
Table: users
...table structure...

mydb> \c otherdb
Switched to connection: otherdb
Connected to: mysql://localhost:3306/otherdb

otherdb> exit
Connection closed. Bye!
```

**Features:**
- Connection reuse across queries (faster response)
- SQL history (up/down arrow keys)
- Multi-line SQL support (statements end with `;`)
- Auto row limit and query timeout

### 6. Execute DDL/DML
```bash
# Safe operations
sql-cli exec "CREATE TABLE test (id INT PRIMARY KEY, name VARCHAR(50))" -c mydb

# Dangerous operations require --confirm
sql-cli exec "DROP TABLE temp_data" -c mydb --confirm
sql-cli exec "TRUNCATE TABLE temp_data" -c mydb --confirm

# BLOCKED operations (will always fail):
# - DROP DATABASE / DROP SCHEMA
# - DELETE without WHERE
# - UPDATE without WHERE
```

### 6. Browse metadata
```bash
# List databases/schemas
sql-cli meta dbs -c mydb

# List tables
sql-cli meta tables -c mydb
sql-cli meta tables -c mydb -d schema_name

# List columns
sql-cli meta columns -t users -c mydb

# List indexes
sql-cli meta indexes -t users -c mydb

# List views
sql-cli meta views -c mydb

# Comprehensive table description (columns + PK + indexes + foreign keys)
sql-cli meta describe -t users -c mydb
```

### 7. Export data
```bash
# Export table to CSV
sql-cli export -t users -c mydb -f csv -o /tmp/users.csv

# Export to JSON / INSERT / UPDATE
sql-cli export -t users -c mydb -f json -o /tmp/users.json
sql-cli export -t users -c mydb -f insert -o /tmp/users.sql
sql-cli export -t users -c mydb -f update --where-columns id -o /tmp/users_update.sql

# Export all tables
sql-cli export --all-tables -c mydb -o /tmp/backup/

# Export to stdout
sql-cli export -t users -c mydb -f csv
```

### 8. Import data
```bash
# Import CSV
sql-cli import -t users -c mydb -f /tmp/users.csv

# Import SQL file
sql-cli import -c mydb -f /tmp/backup.sql --format insert

# Import with options
sql-cli import -t users -c mydb -f /tmp/users.csv --batch-size 1000 --on-error skip
```

### 9. Manage connections
```bash
sql-cli conn list                     # List all connections
sql-cli conn list --group prod        # Filter by group
sql-cli conn list --tag mysql         # Filter by tag
sql-cli conn show mydb                # Show connection details
sql-cli conn update mydb --port 3307  # Update connection
sql-cli conn remove mydb              # Remove connection
sql-cli conn group list               # List groups
sql-cli conn tag list                 # List tags
sql-cli conn types                    # List registered database types
```

## Real-World Example

Complete workflow for connecting to a MySQL database and querying data:

```bash
# 1. Install MySQL driver
sql-cli driver install mysql

# 2. Add connection
sql-cli conn add \
  --name mydb \
  --type mysql \
  --host localhost \
  --port 3306 \
  --user root \
  --password xxxxxx \
  --db testdb \
  --driver mysql-connector-j-8.3.0.jar

# 3. Test connection with verbose output
sql-cli conn test mydb -v

# 4. Query all databases
sql-cli query "SHOW DATABASES" -c mydb

# 5. Describe table structure
sql-cli meta describe -t users -c mydb

# 6. Query first 10 rows
sql-cli query "SELECT * FROM users LIMIT 10" -c mydb

# 7. Interactive shell for multiple queries (connection reuse)
sql-cli shell -c mydb
# Then inside shell:
# mydb> SELECT * FROM users WHERE name LIKE '%test%';
# mydb> \d users
# mydb> \c otherdb
# mydb> exit
```

**Interactive Shell Tips:**
- Use shell mode when running multiple queries in succession (much faster due to connection reuse)
- Use `\dt` to list tables, `\d <table>` to describe table structure
- Use `\c <name>` to switch between connections without exiting
- SQL history is saved to `~/.sql-cli/history` for convenience
- Multi-line SQL is supported (statements end with `;`)

## Troubleshooting

### Connection Issues

**"Unknown database" error**
```bash
# Solution 1: Test without database first
sql-cli conn update mydb --db ""
sql-cli conn test mydb

# Then list databases and find correct one
sql-cli query "SHOW DATABASES" -c mydb

# Solution 2: Update to correct database name
sql-cli conn update mydb --db correct_db_name
```

**"Communications link failure"**
```bash
# Check network connectivity
ping <host>
telnet <host> <port>

# Add connection parameters for MySQL
sql-cli conn add --name mydb ... --param useSSL=false --param allowPublicKeyRetrieval=true
```

**"Driver not found" error**
```bash
# Check installed drivers
sql-cli driver list

# Install missing driver
sql-cli driver install mysql
```

### Connection Test Shows "NOT reachable" but verbose shows nothing
```bash
# Always use -v for verbose error details
sql-cli conn test mydb -v
```

## Safety Rules

1. **Strict mode connections**: Only SELECT and metadata queries allowed
2. **Dangerous operations** (DROP TABLE, TRUNCATE, ALTER TABLE DROP): Require `--confirm` flag
3. **Blocked operations** (DROP DATABASE, DELETE/UPDATE without WHERE): Always rejected
4. **Auto row limit**: SELECT queries default to 500 rows max, use `--limit N` or `--no-limit` to override
5. **Query timeout**: Use `--timeout` to prevent runaway queries
6. **NEVER execute destructive SQL on production databases** without explicit user confirmation

## Supported Databases

| Database   | Type ID        | Default Port |
|-----------|----------------|-------------|
| MySQL     | `mysql`        | 3306        |
| Oracle    | `oracle`       | 1521        |
| PostgreSQL| `postgresql`   | 5432        |
| SQL Server| `mssql`        | 1433        |
| SQLite    | `sqlite`       | -           |
| DM (达梦)  | `dm` (custom)  | 5236        |
| Others    | `generic`      | -           |

## Connection Parameters Reference

| Parameter | Description |
|-----------|-------------|
| `-c, --connection` | Use saved connection by name |
| `--type` | Database type (mysql/oracle/postgresql/mssql/sqlite/generic) |
| `--host` | Database host |
| `--port` | Database port |
| `--user` | Username |
| `--password` | Password |
| `--db` | Database name |
| `--url` | Direct JDBC URL |
| `--driver` | Driver jar file name (in drivers/ directory) |
| `--driver-class` | JDBC Driver class name |
| `--timeout` | Query timeout in seconds |
| `-o, --output` | Save query results to file |
| `-f, --format` | Output format: markdown/json/csv |
