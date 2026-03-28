package com.sqlcli;

import picocli.CommandLine.IVersionProvider;

public final class Version implements IVersionProvider {

    private static final String VERSION = initVersion();

    private Version() {}

    private static String initVersion() {
        try {
            String v = Version.class.getPackage().getImplementationVersion();
            if (v != null && !v.isEmpty()) return v;
        } catch (Exception ignored) {}
        return "dev";
    }

    public static String get() {
        return VERSION;
    }

    public static String fullName() {
        return "sql-cli " + VERSION;
    }

    @Override
    public String[] getVersion() {
        return new String[] { fullName() };
    }
}
