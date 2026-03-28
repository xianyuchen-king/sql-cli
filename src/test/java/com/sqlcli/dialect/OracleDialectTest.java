package com.sqlcli.dialect;

import com.sqlcli.config.ConnectionConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OracleDialectTest {

    private final OracleDialect dialect = new OracleDialect();

    @Test
    void buildUrl() {
        ConnectionConfig config = new ConnectionConfig();
        config.setHost("localhost");
        config.setPort(1521);
        config.setDb("ORCL");

        assertEquals("jdbc:oracle:thin:@//localhost:1521/ORCL", dialect.buildUrl(config));
    }

    @Test
    void wrapLimit() {
        assertEquals("SELECT * FROM users FETCH FIRST 500 ROWS ONLY",
                dialect.wrapLimit("SELECT * FROM users", 500));
    }

    @Test
    void hasLimitFetchFirst() {
        assertTrue(dialect.hasLimit("SELECT * FROM users FETCH FIRST 100 ROWS ONLY"));
    }

    @Test
    void hasLimitRownum() {
        assertTrue(dialect.hasLimit("SELECT * FROM users WHERE ROWNUM < 100"));
    }

    @Test
    void getDefaultPort() {
        assertEquals(1521, dialect.getDefaultPort());
    }
}
