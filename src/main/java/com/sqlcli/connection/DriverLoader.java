package com.sqlcli.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DriverLoader {

    private static final Logger log = LoggerFactory.getLogger(DriverLoader.class);

    private final File driverDir;
    private final Map<String, Driver> loadedDrivers = new ConcurrentHashMap<>();

    public DriverLoader(File driverDir) {
        this.driverDir = driverDir;
    }

    /**
     * Load a JDBC driver from the driver directory.
     */
    public Driver loadDriver(String driverJar, String driverClassName) {
        String cacheKey = driverJar + "#" + driverClassName;
        Driver cached = loadedDrivers.get(cacheKey);
        if (cached != null) return cached;

        try {
            File jarFile = new File(driverDir, driverJar);
            if (!jarFile.exists()) {
                jarFile = new File(driverJar); // Try absolute path
            }
            if (!jarFile.exists()) {
                throw new RuntimeException("Driver jar not found: " + driverJar
                        + " (searched in " + driverDir + ")");
            }

            URL jarUrl = jarFile.toURI().toURL();
            // Don't use try-with-resources - classloader must stay open for driver to load internal classes
            URLClassLoader classLoader = new URLClassLoader(
                    new URL[]{jarUrl},
                    DriverLoader.class.getClassLoader());
            Class<?> clazz = Class.forName(driverClassName, true, classLoader);
            Driver driver = (Driver) clazz.getDeclaredConstructor().newInstance();
            loadedDrivers.put(cacheKey, driver);
            log.debug("Loaded driver: {} from {}", driverClassName, jarFile);
            return driver;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load driver: " + driverClassName
                    + " from " + driverJar, e);
        }
    }

    /**
     * List all jar files in the driver directory.
     */
    public List<File> listDrivers() {
        if (!driverDir.exists() || !driverDir.isDirectory()) {
            return List.of();
        }
        File[] jars = driverDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jars == null) return List.of();
        Arrays.sort(jars, Comparator.comparing(File::getName));
        return Arrays.asList(jars);
    }

    /**
     * Remove a driver jar from the driver directory.
     */
    public boolean removeDriver(String driverJar) {
        File jarFile = new File(driverDir, driverJar);
        if (jarFile.exists()) {
            return jarFile.delete();
        }
        return false;
    }
}
