package com.augurworks.alfred.server;

import com.augurworks.alfred.scaling.ScaleFunctions.ScaleFunctionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class AlfredPrefsImpl implements AlfredPrefs {

    private static final Logger log = LoggerFactory.getLogger(AlfredPrefsImpl.class);
    private static final String DIRECTORY_PREF = "LISTEN_DIRECTORY";
    private static final String DIRECTORY_DEFAULT = "nets";
    private static final String NUM_THREADS_PREF = "NUM_THREADS";
    private static final String NUM_THREADS_DEFAULT = "16";
    private static final String TIMEOUT_PREF = "TRAINING_TIMEOUT_SEC";
    private static final String TIMEOUT_DEFAULT = "3600";
    private static final String SCALE_FUNC_PREF = "SCALE_FUNCTION";
    private static final String SCALE_FUNC_DEFAULT = "SIGMOID";

    @Override
    public String getDirectory() {
        String environmentPath = System.getProperty(DIRECTORY_PREF) == null ? System.getenv(DIRECTORY_PREF) : System.getProperty(DIRECTORY_PREF);
        return new File(environmentPath == null ? DIRECTORY_DEFAULT : environmentPath).getAbsolutePath();
    }

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

}
