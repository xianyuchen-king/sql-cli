package com.sqlcli.dialect;

import com.sqlcli.config.AppConfig;
import com.sqlcli.config.CustomTypeConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomDialectTest {

    private AppConfig buildConfig(CustomTypeConfig... types) {
        AppConfig config = new AppConfig();
        config.setCustomTypes(List.of(types));
        return config;
    }

    private CustomTypeConfig makeType(String name) {
        CustomTypeConfig ct = new CustomTypeConfig();
        ct.setName(name);
        return ct;
    }

    // --- wrapLimit: limitSuffix ---

    @Test
    void limitSuffix_appendsWithTrailingSemicolonStripped() {
        CustomTypeConfig ct = makeType("testdb");
        ct.setLimitSuffix(" LIMIT {n}");
        Dialect d = DialectFactory.getDialect("testdb", buildConfig(ct));

        String result = d.wrapLimit("SELECT * FROM users;", 5);
        assertEquals("SELECT * FROM users LIMIT 5", result);
    }

    @Test
    void limitSuffix_withoutTrailingSemicolon_appendsNormally() {
        CustomTypeConfig ct = makeType("testdb");
        ct.setLimitSuffix(" LIMIT {n}");
        Dialect d = DialectFactory.getDialect("testdb", buildConfig(ct));

        String result = d.wrapLimit("SELECT * FROM users", 10);
        assertEquals("SELECT * FROM users LIMIT 10", result);
    }

    // --- wrapLimit: limitPrefix ---

    @Test
    void limitPrefix_insertsAfterSelect() {
        CustomTypeConfig ct = makeType("testdb");
        ct.setLimitPrefix("TOP {n} ");
        Dialect d = DialectFactory.getDialect("testdb", buildConfig(ct));

        String result = d.wrapLimit("SELECT * FROM users", 5);
        assertEquals("SELECT TOP 5 * FROM users", result);
    }

    @Test
    void limitPrefix_withDistinct_insertsAfterSelectDistinct() {
        CustomTypeConfig ct = makeType("testdb");
        ct.setLimitPrefix("TOP {n} ");
        Dialect d = DialectFactory.getDialect("testdb", buildConfig(ct));

        String result = d.wrapLimit("SELECT DISTINCT col FROM users", 5);
        assertEquals("SELECT DISTINCT TOP 5 col FROM users", result);
    }

    @Test
    void limitPrefix_withNonSelectSQL_returnsNull() {
        CustomTypeConfig ct = makeType("testdb");
        ct.setLimitPrefix("TOP {n} ");
        Dialect d = DialectFactory.getDialect("testdb", buildConfig(ct));

        String result = d.wrapLimit("WITH cte AS (SELECT 1) SELECT * FROM cte", 5);
        assertNull(result);
    }

    // --- hasLimit ---

    @Test
    void hasLimit_withValidPattern_matches() {
        CustomTypeConfig ct = makeType("testdb");
        ct.setLimitPattern("LIMIT\\s+\\d+");
        Dialect d = DialectFactory.getDialect("testdb", buildConfig(ct));

        assertTrue(d.hasLimit("SELECT * FROM users LIMIT 10"));
    }

    @Test
    void hasLimit_withValidPattern_noMatch() {
        CustomTypeConfig ct = makeType("testdb");
        ct.setLimitPattern("LIMIT\\s+\\d+");
        Dialect d = DialectFactory.getDialect("testdb", buildConfig(ct));

        assertFalse(d.hasLimit("SELECT * FROM users"));
    }

    @Test
    void hasLimit_withInvalidRegex_returnsFalse() {
        CustomTypeConfig ct = makeType("testdb");
        ct.setLimitPattern("[invalid(regex");
        Dialect d = DialectFactory.getDialect("testdb", buildConfig(ct));

        assertFalse(d.hasLimit("SELECT * FROM users LIMIT 10"));
    }

    @Test
    void hasLimit_withNullPattern_returnsFalse() {
        CustomTypeConfig ct = makeType("testdb");
        Dialect d = DialectFactory.getDialect("testdb", buildConfig(ct));

        assertFalse(d.hasLimit("SELECT * FROM users LIMIT 10"));
    }

    // --- listDatabases: schemas method ---

    @Mock
    private Connection conn;

    @Mock
    private DatabaseMetaData meta;

    @Mock
    private ResultSet rs;

    @Test
    void listDatabases_schemas_returnsSchemas() throws Exception {
        CustomTypeConfig ct = makeType("testdb");
        ct.setListDatabasesMethod("schemas");
        Dialect d = DialectFactory.getDialect("testdb", buildConfig(ct));

        when(conn.getMetaData()).thenReturn(meta);
        when(meta.getSchemas()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getString("TABLE_SCHEM")).thenReturn("HR", "SCOTT");

        var result = d.listDatabases(conn);

        assertEquals(2, result.size());
        assertEquals("HR", result.get(0)[0]);
        assertEquals("SCOTT", result.get(1)[0]);
        verify(rs).close();
    }

    @Test
    void listDatabases_schemas_withSystemSchemaFilter() throws Exception {
        CustomTypeConfig ct = makeType("testdb");
        ct.setListDatabasesMethod("schemas");
        ct.setSystemSchemaFilter("SYS%,OUTLN");
        Dialect d = DialectFactory.getDialect("testdb", buildConfig(ct));

        when(conn.getMetaData()).thenReturn(meta);
        when(meta.getSchemas()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, true, false);
        when(rs.getString("TABLE_SCHEM")).thenReturn("SYS", "SYSTEM", "HR");

        var result = d.listDatabases(conn);

        assertEquals(1, result.size());
        assertEquals("HR", result.get(0)[0]);
    }

    // --- systemSchemaFilter behavior ---

    @Test
    void systemSchemaFilter_prefixMatch() throws Exception {
        CustomTypeConfig ct = makeType("testdb");
        ct.setListDatabasesMethod("schemas");
        ct.setSystemSchemaFilter("SYS%");
        Dialect d = DialectFactory.getDialect("testdb", buildConfig(ct));

        when(conn.getMetaData()).thenReturn(meta);
        when(meta.getSchemas()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, true, false);
        when(rs.getString("TABLE_SCHEM")).thenReturn("SYS", "SYSTEM", "SYS_ADMIN");

        var result = d.listDatabases(conn);
        assertEquals(0, result.size());
    }

    @Test
    void systemSchemaFilter_exactMatch() throws Exception {
        CustomTypeConfig ct = makeType("testdb");
        ct.setListDatabasesMethod("schemas");
        ct.setSystemSchemaFilter("OUTLN");
        Dialect d = DialectFactory.getDialect("testdb", buildConfig(ct));

        when(conn.getMetaData()).thenReturn(meta);
        when(meta.getSchemas()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getString("TABLE_SCHEM")).thenReturn("OUTLN", "OUTLINE");

        var result = d.listDatabases(conn);
        assertEquals(1, result.size());
        assertEquals("OUTLINE", result.get(0)[0]);
    }

    @Test
    void systemSchemaFilter_null_noFiltering() throws Exception {
        CustomTypeConfig ct = makeType("testdb");
        ct.setListDatabasesMethod("schemas");
        ct.setSystemSchemaFilter(null);
        Dialect d = DialectFactory.getDialect("testdb", buildConfig(ct));

        when(conn.getMetaData()).thenReturn(meta);
        when(meta.getSchemas()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("TABLE_SCHEM")).thenReturn("SYS");

        var result = d.listDatabases(conn);
        assertEquals(1, result.size());
        assertEquals("SYS", result.get(0)[0]);
    }

    @Test
    void systemSchemaFilter_withEmptyEntries_skipsEmpty() throws Exception {
        CustomTypeConfig ct = makeType("testdb");
        ct.setListDatabasesMethod("schemas");
        ct.setSystemSchemaFilter("SYS%,,OUTLN");
        Dialect d = DialectFactory.getDialect("testdb", buildConfig(ct));

        when(conn.getMetaData()).thenReturn(meta);
        when(meta.getSchemas()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, true, false);
        when(rs.getString("TABLE_SCHEM")).thenReturn("SYS", "HR", "OUTLN");

        var result = d.listDatabases(conn);
        assertEquals(1, result.size());
        assertEquals("HR", result.get(0)[0]);
    }

    @Test
    void listDatabases_default_usesCatalogs() throws Exception {
        CustomTypeConfig ct = makeType("testdb");
        // No listDatabasesMethod set -> should use default catalogs
        Dialect d = DialectFactory.getDialect("testdb", buildConfig(ct));

        when(conn.getMetaData()).thenReturn(meta);
        when(meta.getCatalogs()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("TABLE_CAT")).thenReturn("mydb");

        var result = d.listDatabases(conn);
        assertEquals(1, result.size());
        assertEquals("mydb", result.get(0)[0]);
    }

    // --- backward compatibility ---

    @Test
    void allNewFieldsNull_backwardCompatible() {
        CustomTypeConfig ct = makeType("testdb");
        ct.setDriverClass("com.example.Driver");
        ct.setDefaultPort(5432);
        ct.setUrlTemplate("jdbc:example://{host}:{port}/{db}");
        Dialect d = DialectFactory.getDialect("testdb", buildConfig(ct));

        assertNull(d.wrapLimit("SELECT 1", 10));
        assertFalse(d.hasLimit("SELECT 1 LIMIT 10"));
        assertEquals("Database", d.getDatabaseLabel());
        assertEquals(5432, d.getDefaultPort());
        assertEquals("com.example.Driver", d.getDefaultDriverClass());
    }

    // --- getDatabaseLabel ---

    @Test
    void getDatabaseLabel_returnsConfiguredValue() {
        CustomTypeConfig ct = makeType("testdb");
        ct.setDatabaseLabel("Schema");
        Dialect d = DialectFactory.getDialect("testdb", buildConfig(ct));

        assertEquals("Schema", d.getDatabaseLabel());
    }

    @Test
    void getDatabaseLabel_defaultsToDatabase() {
        CustomTypeConfig ct = makeType("testdb");
        Dialect d = DialectFactory.getDialect("testdb", buildConfig(ct));

        assertEquals("Database", d.getDatabaseLabel());
    }
}
