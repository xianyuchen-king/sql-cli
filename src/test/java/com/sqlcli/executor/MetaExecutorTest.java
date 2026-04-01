package com.sqlcli.executor;

import com.sqlcli.dialect.Dialect;
import com.sqlcli.dialect.GenericDialect;
import com.sqlcli.output.OutputFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MetaExecutorTest {

    private final MetaExecutor executor = new MetaExecutor();

    private Connection connection;
    private OutputFormatter formatter;
    private Dialect dialect;

    @BeforeEach
    void setUp() {
        connection = mock(Connection.class);
        formatter = mock(OutputFormatter.class);
        dialect = mock(Dialect.class);
    }

    @Test
    void listDatabases_withDialect_returnsFormattedResult() throws Exception {
        List<String[]> rows = List.of(new String[]{"mydb"}, new String[]{"testdb"});
        when(dialect.listDatabases(connection)).thenReturn(rows);
        when(dialect.getDatabaseLabel()).thenReturn("Database");
        when(formatter.formatTable(any(), any())).thenReturn("| Database |\n|----------|\n| mydb     |\n| testdb   |");

        String result = executor.listDatabases(connection, dialect, formatter);

        assertNotEquals("(no results)", result);
        verify(dialect).listDatabases(connection);
        verify(dialect).getDatabaseLabel();
        verify(formatter).formatTable(eq(List.of("Database")), any());
    }

    @Test
    void listDatabases_emptyResult() throws Exception {
        when(dialect.listDatabases(connection)).thenReturn(List.of());

        String result = executor.listDatabases(connection, dialect, formatter);

        assertEquals("(no results)", result);
        verify(formatter, never()).formatTable(any(), any());
    }

    @SuppressWarnings("deprecation")
    @Test
    void listDatabases_deprecatedOverload_stillWorks() throws Exception {
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.getMetaData()).thenReturn(meta);
        when(meta.getCatalogs()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("TABLE_CAT")).thenReturn("testdb");
        when(formatter.formatTable(any(), any())).thenReturn("| Database |\n|----------|\n| testdb   |");

        String result = executor.listDatabases(connection, formatter);

        assertNotEquals("(no results)", result);
        verify(formatter).formatTable(any(), any());
    }
}
