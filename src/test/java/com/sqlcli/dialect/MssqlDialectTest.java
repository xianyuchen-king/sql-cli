package com.sqlcli.dialect;

import com.sqlcli.config.ConnectionConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MssqlDialectTest {

    private final MssqlDialect dialect = new MssqlDialect();

    @Test
    void buildUrl_basic() {
        ConnectionConfig config = new ConnectionConfig();
        config.setHost("localhost");
        config.setPort(1433);
        config.setDb("mydb");
        String url = dialect.buildUrl(config);
        assertTrue(url.startsWith("jdbc:sqlserver://localhost:1433"));
        assertTrue(url.contains("databaseName=mydb"));
    }

    @Test
    void buildUrl_defaultPort() {
        ConnectionConfig config = new ConnectionConfig();
        config.setHost("db.example.com");
        config.setDb("testdb");
        String url = dialect.buildUrl(config);
        assertTrue(url.contains("db.example.com:1433"));
    }

    @Test
    void buildUrl_withParams() {
        ConnectionConfig config = new ConnectionConfig();
        config.setHost("localhost");
        config.setPort(1433);
        config.setParams(java.util.Map.of("encrypt", "true"));
        String url = dialect.buildUrl(config);
        assertTrue(url.contains("encrypt=true"));
    }

    @Test
    void wrapLimit_addsTop() {
        assertEquals("SELECT TOP 100 * FROM users", dialect.wrapLimit("SELECT * FROM users", 100));
    }

    @Test
    void wrapLimit_preservesDistinct() {
        String result = dialect.wrapLimit("SELECT DISTINCT name FROM users", 50);
        assertEquals("SELECT DISTINCT TOP 50 name FROM users", result);
    }

    @Test
    void wrapLimit_stripsSemicolon() {
        assertEquals("SELECT TOP 10 * FROM t", dialect.wrapLimit("SELECT * FROM t;", 10));
    }

    @Test
    void hasLimit_detectsTop() {
        assertTrue(dialect.hasLimit("SELECT TOP 10 * FROM t"));
    }

    @Test
    void hasLimit_falseWhenNoTop() {
        assertFalse(dialect.hasLimit("SELECT * FROM t"));
    }

    @Test
    void getDefaultPort() {
        assertEquals(1433, dialect.getDefaultPort());
    }

    @Test
    void getDefaultDriverClass() {
        assertEquals("com.microsoft.sqlserver.jdbc.SQLServerDriver", dialect.getDefaultDriverClass());
    }
}
