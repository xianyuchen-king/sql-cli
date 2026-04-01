package com.sqlcli.dialect;

import com.sqlcli.config.ConnectionConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

    @Mock
    private Connection conn;

    @Mock
    private DatabaseMetaData meta;

    @Mock
    private ResultSet rs;

    @Test
    void listDatabases_returnsSchemasAndFiltersSystem() throws Exception {
        when(conn.getMetaData()).thenReturn(meta);
        when(meta.getSchemas()).thenReturn(rs);

        // Simulate rows: SYS (system), DBMS_SCHEDULER (system prefix), OUTLN (system prefix),
        // HR (user), SCOTT (user)
        when(rs.next()).thenReturn(true, true, true, true, true, false);
        when(rs.getString("TABLE_SCHEM")).thenReturn("SYS", "DBMS_SCHEDULER", "OUTLN", "HR", "SCOTT");

        var result = dialect.listDatabases(conn);

        assertEquals(2, result.size());
        assertEquals("HR", result.get(0)[0]);
        assertEquals("SCOTT", result.get(1)[0]);

        verify(rs).close();
    }

    @Test
    void getDatabaseLabel_returnsSchema() {
        assertEquals("Schema", dialect.getDatabaseLabel());
    }
}
