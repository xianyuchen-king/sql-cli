package com.sqlcli.dialect;

import com.sqlcli.config.ConnectionConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SqliteDialectTest {

    private final SqliteDialect dialect = new SqliteDialect();

    @Test
    void buildUrl() {
        ConnectionConfig config = new ConnectionConfig();
        config.setDb("/data/test.db");

        assertEquals("jdbc:sqlite:/data/test.db", dialect.buildUrl(config));
    }

    @Test
    void buildUrlNoDbThrows() {
        ConnectionConfig config = new ConnectionConfig();
        assertThrows(IllegalArgumentException.class, () -> dialect.buildUrl(config));
    }

    @Test
    void wrapLimit() {
        assertEquals("SELECT * FROM t LIMIT 100",
                dialect.wrapLimit("SELECT * FROM t", 100));
    }

    @Test
    void getDefaultPort() {
        assertEquals(0, dialect.getDefaultPort());
    }
}
