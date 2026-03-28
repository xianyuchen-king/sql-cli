package com.sqlcli.dialect;

import org.junit.jupiter.api.Test;
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
}
