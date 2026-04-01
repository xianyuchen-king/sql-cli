package com.sqlcli.dialect;

import com.sqlcli.config.AppConfig;
import com.sqlcli.config.CustomTypeConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DialectFactoryTest {

    @Test
    void getBuiltinDialects() {
        assertTrue(DialectFactory.getDialect("mysql", null) instanceof MysqlDialect);
        assertTrue(DialectFactory.getDialect("oracle", null) instanceof OracleDialect);
        assertTrue(DialectFactory.getDialect("postgresql", null) instanceof PostgresqlDialect);
        assertTrue(DialectFactory.getDialect("sqlite", null) instanceof SqliteDialect);
        assertTrue(DialectFactory.getDialect("unknown", null) instanceof GenericDialect);
    }

    @Test
    void getAllTypes() {
        var types = DialectFactory.getAllTypes(null);
        assertTrue(types.containsKey("mysql"));
        assertTrue(types.containsKey("oracle"));
        assertTrue(types.containsKey("postgresql"));
        assertTrue(types.containsKey("sqlite"));
        assertEquals("builtin", types.get("mysql"));
    }

    @Test
    void getDefaultDriverClass() {
        assertEquals("com.mysql.cj.jdbc.Driver", DialectFactory.getDefaultDriverClass("mysql", null));
        assertEquals("oracle.jdbc.OracleDriver", DialectFactory.getDefaultDriverClass("oracle", null));
        assertNull(DialectFactory.getDefaultDriverClass("unknown", null));
    }

    @Test
    void getDialect_withCustomType_returnsDialect() {
        AppConfig config = new AppConfig();
        CustomTypeConfig ct = new CustomTypeConfig();
        ct.setName("testdb");
        ct.setDriverClass("com.example.Driver");
        ct.setUrlTemplate("jdbc:example://{host}:{port}/{db}");
        config.setCustomTypes(List.of(ct));

        Dialect dialect = DialectFactory.getDialect("testdb", config);
        assertNotNull(dialect);
        assertFalse(dialect instanceof GenericDialect);
        assertEquals("com.example.Driver", dialect.getDefaultDriverClass());
    }

    @Test
    void getAllTypes_withCustomTypes() {
        AppConfig config = new AppConfig();
        CustomTypeConfig ct = new CustomTypeConfig();
        ct.setName("mydb");
        ct.setDriverClass("com.example.Driver");
        ct.setUrlTemplate("jdbc:example://{host}:{port}/{db}");
        config.setCustomTypes(List.of(ct));

        var types = DialectFactory.getAllTypes(config);
        assertTrue(types.containsKey("mysql"));
        assertEquals("builtin", types.get("mysql"));
        assertTrue(types.containsKey("mydb"));
        assertEquals("custom", types.get("mydb"));
    }
}
