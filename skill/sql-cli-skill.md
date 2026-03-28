---
name: sql-cli
description: Database CLI tool for connecting to relational databases (MySQL, Oracle, PostgreSQL, SQLite, etc.) via JDBC. Provides query execution, metadata browsing, data import/export, and connection management.
type: user
---

# SQL Database Tool (sql-cli)

## Prerequisites

- **Java 17+** must be installed and on PATH. sql-cli checks this on startup.
- **Node.js 16+** required for npm installation (not needed at runtime).

## Installation & Setup

### Check Installation
Run `sql-cli --version` to check if sql-cli is installed.

### Install via npm (Recommended)
```bash
npm install -g @cyq/sql-cli
```

After installation, `~/.sql-cli/` directory is auto-created with `drivers/` subdirectory.

### Install from source (development)
1. Clone the repository and build:
   ```bash
   cd /path/to/SQL-cli
   ./gradlew shadowJar
   ```
2. Create a startup script:
   ```bash
   sudo tee /usr/local/bin/sql-cli << 'EOF'
   #!/bin/bash
   java -jar /path/to/SQL-cli/build/libs/sql-cli.jar "$@"
   EOF
   sudo chmod +x /usr/local/bin/sql-cli
   ```
3. Run `sql-cli init` to initialize configuration
4. Set the encryption key environment variable (shown during init)

### Initialize Configuration
```bash
sql-cli init
```
This creates `~/.sql-cli/config.yml` and `~/.sql-cli/drivers/`.

### Uninstall
```bash
# npm uninstall
npm uninstall -g @cyq/sql-cli

# Remove config and drivers (optional)
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

### Uninstall
```bash
sql-cli uninstall --confirm    # Removes ~/.sql-cli/ entirely
```

## Usage

### 1. First, check existing connections
```bash
sql-cli conn list
```

### 2. Add a connection (interactive or non-interactive)
```bash
# Interactive
sql-cli conn add

# Non-interactive
sql-cli conn add --name mydb --type mysql --host localhost --port 3306 --user root --password xxx --db testdb --driver mysql-connector-j-8.3.0.jar

# Direct JDBC URL (for uncommon databases)
sql-cli conn add --name mydb --type generic --url "jdbc:xxx://host:port/db" --user admin --password xxx --driver MyDriver.jar --driver-class com.example.Driver
```

### 3. Test a connection
```bash
sql-cli conn test mydb
```

### 4. Execute queries
```bash
# SELECT query (returns Markdown table by default)
sql-cli query "SELECT * FROM users LIMIT 10" -c mydb

# Change output format
sql-cli query "SELECT * FROM users" -c mydb -f json
sql-cli query "SELECT * FROM users" -c mydb -f csv

# Custom row limit
sql-cli query "SELECT * FROM users" -c mydb --limit 100
sql-cli query "SELECT * FROM users" -c mydb --no-limit

# Direct connection (without saved config)
sql-cli query "SELECT 1" --type mysql --host localhost --port 3306 --user root --password xxx --db testdb --driver mysql-connector-j-8.3.0.jar
```

### 5. Execute DDL/DML
```bash
# Safe operations (CREATE, ALTER, INSERT with WHERE)
sql-cli exec "CREATE TABLE test (id INT PRIMARY KEY, name VARCHAR(50))" -c mydb

# Dangerous operations require --confirm
sql-cli exec "DROP TABLE temp_data" -c mydb --confirm
sql-cli exec "TRUNCATE TABLE temp_data" -c mydb --confirm

# BLOCKED operations (will fail):
# - DROP DATABASE / DROP SCHEMA
# - DELETE without WHERE
# - UPDATE without WHERE
```

### 6. Browse metadata
```bash
sql-cli meta dbs -c mydb
sql-cli meta tables -c mydb
sql-cli meta tables -c mydb -d testdb
sql-cli meta columns -t users -c mydb
sql-cli meta indexes -t users -c mydb
sql-cli meta views -c mydb
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

## Safety Rules

1. **Strict mode connections**: Only SELECT and metadata queries allowed
2. **Dangerous operations** (DROP TABLE, TRUNCATE, ALTER TABLE DROP): Require `--confirm` flag
3. **Blocked operations** (DROP DATABASE, DELETE/UPDATE without WHERE): Always rejected
4. **Auto row limit**: SELECT queries default to 500 rows max, use `--limit N` or `--no-limit` to override
5. **NEVER execute destructive SQL on production databases** without explicit user confirmation

## Connection Parameters Reference

| Parameter | Description |
|-----------|-------------|
| `-c, --connection` | Use saved connection by name |
| `--type` | Database type (mysql/oracle/postgresql/sqlite/generic) |
| `--host` | Database host |
| `--port` | Database port |
| `--user` | Username |
| `--password` | Password |
| `--db` | Database name |
| `--url` | Direct JDBC URL |
| `--driver` | Driver jar file name (in drivers/ directory) |
| `--driver-class` | JDBC Driver class name |
