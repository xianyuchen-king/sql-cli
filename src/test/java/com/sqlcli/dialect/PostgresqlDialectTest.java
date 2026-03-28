package com.sqlcli.dialect;

import com.sqlcli.config.ConnectionConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PostgresqlDialectTest {

    private final PostgresqlDialect dialect = new PostgresqlDialect();

    @Test
    void buildUrl_basic() {
        ConnectionConfig config = new ConnectionConfig();
        config.setHost("localhost");
        config.setPort(5432);
        config.setDb("mydb");
        assertEquals("jdbc:postgresql://localhost:5432/mydb", dialect.buildUrl(config));
    }

    @Test
    void buildUrl_defaultPort() {
        ConnectionConfig config = new ConnectionConfig();
        config.setHost("db.example.com");
        config.setDb("testdb");
        assertEquals("jdbc:postgresql://db.example.com:5432/testdb", dialect.buildUrl(config));
    }

    @Test
    void buildUrl_withParams() {
        ConnectionConfig config = new ConnectionConfig();
        config.setHost("localhost");
        config.setPort(5432);
        config.setDb("mydb");
        config.setParams(java.util.Map.of("sslmode", "require"));
        String url = dialect.buildUrl(config);
        assertTrue(url.contains("sslmode=require"));
    }

    @Test
    void wrapLimit_appendsLimit() {
        assertEquals("SELECT * FROM t LIMIT 100", dialect.wrapLimit("SELECT * FROM t", 100));
    }

    @Test
    void wrapLimit_stripsSemicolon() {
        assertEquals("SELECT * FROM t LIMIT 50", dialect.wrapLimit("SELECT * FROM t;", 50));
    }

    @Test
    void hasLimit_detectsLimit() {
        assertTrue(dialect.hasLimit("SELECT * FROM t LIMIT 10"));
    }

    @Test
    void hasLimit_detectsFetchFirst() {
        assertTrue(dialect.hasLimit("SELECT * FROM t FETCH FIRST 10 ROWS ONLY"));
    }

    @Test
    void hasLimit_falseWhenNoLimit() {
        assertFalse(dialect.hasLimit("SELECT * FROM t"));
    }

    @Test
    void getDefaultPort() {
        assertEquals(5432, dialect.getDefaultPort());
    }

    @Test
    void getDefaultDriverClass() {
        assertEquals("org.postgresql.Driver", dialect.getDefaultDriverClass());
    }
}
