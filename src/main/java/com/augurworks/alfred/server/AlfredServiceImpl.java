package com.augurworks.alfred.server;

import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.augurworks.alfred.scaling.ScaleFunctions.ScaleFunctionType;

public class AlfredServiceImpl implements AlfredService {

    private static final Logger log = LoggerFactory.getLogger(AlfredServiceImpl.class);
    private AlfredPrefs prefs;
    private FileAlterationMonitor fileAlterationMonitor;
    private FileAlterationObserver fileAlterationObserver;

    public void setPrefs(AlfredPrefs prefs) {
        this.prefs = prefs;
    }

    public void init() {
        prefs = new AlfredPrefsImpl();
        System.out.println("hello");
        String dir = prefs.getDirectory();
        int threads = prefs.getNumThreads();
        int timeout = prefs.getTimeout();
        ScaleFunctionType scaleFunction = prefs.getScaleFunction();

        fileAlterationMonitor = new FileAlterationMonitor();
        fileAlterationObserver = new FileAlterationObserver(dir);
        fileAlterationMonitor.addObserver(fileAlterationObserver);
        AlfredDirectoryListener alfredListener = new AlfredDirectoryListener(threads, timeout, scaleFunction);
        fileAlterationObserver.addListener(alfredListener);
        try {
            fileAlterationObserver.initialize();
            fileAlterationMonitor.start();
        } catch (Exception e) {
            log.error("Unable to initialize file observer. Server will exit now.", e);
            throw new RuntimeException("Unable to initialize file observer", e);
        }
        log.info(String.format("Alfred Server started on %s with %s threads and a job timeout of %s seconds",
                dir, threads, timeout));
    }

    public void destroy() {
        try {
            fileAlterationMonitor.stop();
            fileAlterationObserver.destroy();
        } catch (Throwable e) {
            // closing things. ignore errors.
            log.error("Error closing file listeners", e);
        }
    }
}
