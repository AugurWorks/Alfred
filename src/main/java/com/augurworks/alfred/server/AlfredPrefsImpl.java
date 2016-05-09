package com.augurworks.alfred.server;

import com.augurworks.alfred.scaling.ScaleFunctions.ScaleFunctionType;

public class AlfredPrefsImpl implements AlfredPrefs {

    private static final String STATS_HISTORY_PREF = "STATS_HISTORY";
    private static final String STATS_HISTORY_DEFAULT = "1000";
    private static final String NUM_THREADS_PREF = "NUM_THREADS";
    private static final String NUM_THREADS_DEFAULT = "16";
    private static final String TIMEOUT_PREF = "TRAINING_TIMEOUT_SEC";
    private static final String TIMEOUT_DEFAULT = "3600";
    private static final String VERBOSE_PREF = "VERBOSE";
    private static final boolean VERBOSE_DEFAULT = false;
    private static final String SCALE_FUNC_PREF = "SCALE_FUNCTION";
    private static final String SCALE_FUNC_DEFAULT = "SIGMOID";

    @Override
    public int getNumThreads() {
        String environmentPath = System.getProperty(NUM_THREADS_PREF) == null ? System.getenv(NUM_THREADS_PREF) : System.getProperty(NUM_THREADS_PREF);
        return parseInt(environmentPath, NUM_THREADS_DEFAULT);
    }

    @Override
    public int getTimeout() {
        String environmentPath = System.getProperty(TIMEOUT_PREF) == null ? System.getenv(TIMEOUT_PREF) : System.getProperty(TIMEOUT_PREF);
        return parseInt(environmentPath, TIMEOUT_DEFAULT);
    }

    @Override
    public boolean getVerbose() {
        String environmentPath = System.getProperty(VERBOSE_PREF) == null ? System.getenv(VERBOSE_PREF) : System.getProperty(VERBOSE_PREF);
        return environmentPath == null ? VERBOSE_DEFAULT : Boolean.valueOf(environmentPath);
    }

    @Override
    public ScaleFunctionType getScaleFunction() {
        String environmentPath = System.getProperty(SCALE_FUNC_PREF) == null ? System.getenv(SCALE_FUNC_PREF) : System.getProperty(SCALE_FUNC_PREF);
        return ScaleFunctionType.fromString(environmentPath == null ? SCALE_FUNC_DEFAULT : environmentPath);
    }

    private int parseInt(String pref, String defaultValue) {
        try {
            return Integer.parseInt(pref);
        } catch (NumberFormatException e) { }
        // this should throw if it fails.
        return Integer.parseInt(defaultValue);
    }

    @Override
    public int getStatsHistory() {
        String environmentPath = System.getProperty(STATS_HISTORY_PREF) == null ? System.getenv(STATS_HISTORY_PREF) : System.getProperty(STATS_HISTORY_PREF);
        return parseInt(environmentPath, STATS_HISTORY_DEFAULT);
    }

}
