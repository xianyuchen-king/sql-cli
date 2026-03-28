package com.sqlcli.dialect;

import com.sqlcli.config.ConnectionConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GenericDialectTest {

    private final GenericDialect dialect = new GenericDialect();

    @Test
    void buildUrl_returnsDirectUrl() {
        ConnectionConfig config = new ConnectionConfig();
        config.setUrl("jdbc:custom://localhost/db");
        assertEquals("jdbc:custom://localhost/db", dialect.buildUrl(config));
    }

    @Test
    void buildUrl_throwsWithoutUrl() {
        ConnectionConfig config = new ConnectionConfig();
        assertThrows(IllegalArgumentException.class, () -> dialect.buildUrl(config));
    }

    @Test
    void buildUrl_throwsWithBlankUrl() {
        ConnectionConfig config = new ConnectionConfig();
        config.setUrl("  ");
        assertThrows(IllegalArgumentException.class, () -> dialect.buildUrl(config));
    }

    @Test
    void wrapLimit_returnsNull() {
        assertNull(dialect.wrapLimit("SELECT * FROM t", 100));
    }

    @Test
    void hasLimit_returnsFalse() {
        assertFalse(dialect.hasLimit("SELECT * FROM t LIMIT 10"));
    }

    @Test
    void getDefaultPort_returnsZero() {
        assertEquals(0, dialect.getDefaultPort());
    }

    @Test
    void getDefaultDriverClass_returnsNull() {
        assertNull(dialect.getDefaultDriverClass());
    }
}
