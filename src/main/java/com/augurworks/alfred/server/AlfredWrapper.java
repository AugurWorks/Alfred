package com.augurworks.alfred.server;

import com.augurworks.alfred.RectNetFixed;
import com.augurworks.alfred.scaling.ScaleFunctions.ScaleFunctionType;
import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class AlfredWrapper {

    Logger log = LoggerFactory.getLogger(AlfredWrapper.class);

    private final ExecutorService exec;
    private final int timeoutSeconds;
    private final Semaphore semaphore;
    private final ScaleFunctionType sfType;

    public AlfredWrapper(int numThreads, int timeoutSeconds, ScaleFunctionType sfType) {
        this.exec = Executors.newCachedThreadPool();
        this.semaphore = new Semaphore(numThreads);
        this.timeoutSeconds = timeoutSeconds;
        this.sfType = sfType;
    }

    public static RectNetFixed trainStatic(String netId, String augtrain, Integer timeoutMillis) {
        ScaleFunctionType scaleFunctionType = ScaleFunctionType.SIGMOID;

        MDC.put("netId", netId);
        MDC.put("trainingTimeLimitSec", timeoutMillis / 1000);
        MDC.put("scaleFunctionType", scaleFunctionType.name());

        try {
            List<String> lines = Arrays.asList(augtrain.split("\n"));
            RectNetFixed net = new RectNetFixed(netId, lines, scaleFunctionType).train(timeoutMillis, 5);
            return net;
        } catch (Exception t) { }
        return null;
    }
}
