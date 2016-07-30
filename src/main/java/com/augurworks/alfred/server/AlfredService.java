package com.augurworks.alfred.server;

import java.util.concurrent.TimeUnit;

public interface AlfredService {
    void cancelJob(String jobName);
    boolean isShutdown();
    void shutdownAndAwaitTermination(long timeout, TimeUnit unit);
    void shutdownImmediate();
    void train(String name, String augtrain);
    String trainSynchronous(String netId, String augtrain);
    String getResult(String name);
    TrainStatus getStatus(String name);
    String getStats();
}
