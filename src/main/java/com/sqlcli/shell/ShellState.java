package com.sqlcli.shell;

import com.sqlcli.output.OutputFormatter;

/**
 * Mutable session state for the interactive shell.
 * Contains current settings that can be changed via slash commands.
 */
public class ShellState {

    private OutputFormatter.Format outputFormat;
    private int maxRows;
    private int queryTimeout;
    private boolean autoLimit;
    private boolean exitRequested = false;
    private String switchToConnection = null;  // For \c command

    public ShellState(OutputFormatter.Format defaultFormat, int defaultMaxRows, boolean autoLimit) {
        this.outputFormat = defaultFormat;
        this.maxRows = defaultMaxRows;
        this.autoLimit = autoLimit;
        this.queryTimeout = 0;
    }

    public OutputFormatter.Format getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(OutputFormatter.Format format) {
        this.outputFormat = format;
    }

    public int getMaxRows() {
        return maxRows;
    }

    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }

    public int getQueryTimeout() {
        return queryTimeout;
    }

    public void setQueryTimeout(int timeout) {
        this.queryTimeout = timeout;
    }

    public boolean isAutoLimit() {
        return autoLimit;
    }

    public void setAutoLimit(boolean autoLimit) {
        this.autoLimit = autoLimit;
    }

    public void disableLimit() {
        this.autoLimit = false;
        this.maxRows = 0;
    }

    public boolean isExitRequested() {
        return exitRequested;
    }

    public void requestExit() {
        this.exitRequested = true;
    }

    public String getSwitchToConnection() {
        return switchToConnection;
    }

    public void requestSwitchConnection(String connectionName) {
        this.switchToConnection = connectionName;
    }

    public void clearSwitchConnection() {
        this.switchToConnection = null;
    }

    public boolean isSwitchConnectionRequested() {
        return switchToConnection != null;
    }

    public OutputFormatter createFormatter() {
        return OutputFormatter.create(outputFormat);
    }
}
