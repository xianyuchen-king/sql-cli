package com.sqlcli.dialect;

import com.sqlcli.config.ConnectionConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MysqlDialectTest {

    private final MysqlDialect dialect = new MysqlDialect();

    @Test
    void buildUrlBasic() {
        ConnectionConfig config = new ConnectionConfig();
        config.setHost("localhost");
        config.setPort(3306);
        config.setDb("testdb");

        assertEquals("jdbc:mysql://localhost:3306/testdb", dialect.buildUrl(config));
    }

    @Test
    void buildUrlWithParams() {
        ConnectionConfig config = new ConnectionConfig();
        config.setHost("10.0.0.1");
        config.setPort(3307);
        config.setDb("mydb");
        config.setParams(java.util.Map.of("useSSL", "false", "charset", "utf8mb4"));

        String url = dialect.buildUrl(config);
        assertTrue(url.startsWith("jdbc:mysql://10.0.0.1:3307/mydb?"));
        assertTrue(url.contains("useSSL=false"));
        assertTrue(url.contains("charset=utf8mb4"));
    }

    @Test
    void buildUrlDefaultPort() {
        ConnectionConfig config = new ConnectionConfig();
        config.setHost("localhost");
        config.setDb("test");

        assertEquals("jdbc:mysql://localhost:3306/test", dialect.buildUrl(config));
    }

    @Test
    void wrapLimit() {
        assertEquals("SELECT * FROM users LIMIT 500",
                dialect.wrapLimit("SELECT * FROM users", 500));
    }

    @Test
    void wrapLimitWithSemicolon() {
        assertEquals("SELECT * FROM users LIMIT 100",
                dialect.wrapLimit("SELECT * FROM users;", 100));
    }

    @Test
    void hasLimitTrue() {
        assertTrue(dialect.hasLimit("SELECT * FROM users LIMIT 10"));
    }

    @Test
    void hasLimitFalse() {
        assertFalse(dialect.hasLimit("SELECT * FROM users"));
    }

    @Test
    void getDefaultPort() {
        assertEquals(3306, dialect.getDefaultPort());
    }

    @Test
    void getDefaultDriverClass() {
        assertEquals("com.mysql.cj.jdbc.Driver", dialect.getDefaultDriverClass());
    }
}
