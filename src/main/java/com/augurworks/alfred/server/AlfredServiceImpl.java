package com.augurworks.alfred.server;

import com.augurworks.alfred.scaling.ScaleFunctions.ScaleFunctionType;

public class AlfredServiceImpl implements AlfredService {

    private AlfredPrefs prefs;

    public void setPrefs(AlfredPrefs prefs) {
        this.prefs = prefs;
    }

    public void init() {
        prefs = new AlfredPrefsImpl();
        int threads = prefs.getNumThreads();
        int timeout = prefs.getTimeout();
        ScaleFunctionType scaleFunction = prefs.getScaleFunction();

        AlfredDirectoryListener alfredListener = new AlfredDirectoryListener(threads, timeout, scaleFunction);
    }

}
