package com.sqlcli.safety;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SqlAnalyzerTest {

    private final SqlAnalyzer analyzer = new SqlAnalyzer();

    // BLOCKED operations

    @Test
    void dropDatabaseIsBlocked() {
        assertEquals(SqlAnalyzer.RiskLevel.BLOCKED, analyzer.analyze("DROP DATABASE testdb"));
    }

    @Test
    void dropSchemaIsBlocked() {
        assertEquals(SqlAnalyzer.RiskLevel.BLOCKED, analyzer.analyze("DROP SCHEMA myschema"));
    }

    @Test
    void deleteWithoutWhereIsBlocked() {
        assertEquals(SqlAnalyzer.RiskLevel.BLOCKED, analyzer.analyze("DELETE FROM users"));
    }

    @Test
    void updateWithoutWhereIsBlocked() {
        assertEquals(SqlAnalyzer.RiskLevel.BLOCKED, analyzer.analyze("UPDATE users SET name = 'x'"));
    }

    @Test
    void deleteWithoutWhereCaseInsensitive() {
        assertEquals(SqlAnalyzer.RiskLevel.BLOCKED, analyzer.analyze("delete from users"));
    }

    // DANGEROUS operations

    @Test
    void dropTableIsDangerous() {
        assertEquals(SqlAnalyzer.RiskLevel.DANGEROUS, analyzer.analyze("DROP TABLE users"));
    }

    @Test
    void truncateIsDangerous() {
        assertEquals(SqlAnalyzer.RiskLevel.DANGEROUS, analyzer.analyze("TRUNCATE TABLE users"));
    }

    @Test
    void alterTableDropIsDangerous() {
        assertEquals(SqlAnalyzer.RiskLevel.DANGEROUS, analyzer.analyze("ALTER TABLE users DROP COLUMN age"));
    }

    // WARNING operations

    @Test
    void alterTableIsWarning() {
        assertEquals(SqlAnalyzer.RiskLevel.WARNING, analyzer.analyze("ALTER TABLE users ADD COLUMN age INT"));
    }

    // SAFE operations

    @Test
    void selectIsSafe() {
        assertEquals(SqlAnalyzer.RiskLevel.SAFE, analyzer.analyze("SELECT * FROM users"));
    }

    @Test
    void createTableIsSafe() {
        assertEquals(SqlAnalyzer.RiskLevel.SAFE, analyzer.analyze("CREATE TABLE test (id INT)"));
    }

    @Test
    void createIndexIsSafe() {
        assertEquals(SqlAnalyzer.RiskLevel.SAFE, analyzer.analyze("CREATE INDEX idx_name ON users(name)"));
    }

    @Test
    void insertIsSafe() {
        assertEquals(SqlAnalyzer.RiskLevel.SAFE, analyzer.analyze("INSERT INTO users VALUES (1, 'test')"));
    }

    @Test
    void deleteWithWhereIsSafe() {
        assertEquals(SqlAnalyzer.RiskLevel.SAFE, analyzer.analyze("DELETE FROM users WHERE id = 1"));
    }

    @Test
    void updateWithWhereIsSafe() {
        assertEquals(SqlAnalyzer.RiskLevel.SAFE, analyzer.analyze("UPDATE users SET name = 'x' WHERE id = 1"));
    }

    // isSelect

    @Test
    void isSelectReturnsTrue() {
        assertTrue(analyzer.isSelect("SELECT * FROM users"));
    }

    @Test
    void isSelectReturnsFalse() {
        assertFalse(analyzer.isSelect("DELETE FROM users"));
    }

    @Test
    void isSelectNullInput() {
        assertFalse(analyzer.isSelect(null));
    }

    // getMessage

    @Test
    void blockedMessage() {
        String msg = analyzer.getMessage(SqlAnalyzer.RiskLevel.BLOCKED, "DELETE FROM users");
        assertNotNull(msg);
        assertTrue(msg.contains("[BLOCKED]"));
    }

    @Test
    void safeMessageReturnsNull() {
        assertNull(analyzer.getMessage(SqlAnalyzer.RiskLevel.SAFE, "SELECT 1"));
    }
}
