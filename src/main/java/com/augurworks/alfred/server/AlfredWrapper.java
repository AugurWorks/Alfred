package com.augurworks.alfred.server;

import com.augurworks.alfred.RectNetFixed;
import com.augurworks.alfred.messaging.TrainingMessage;
import com.augurworks.alfred.scaling.ScaleFunctions.ScaleFunctionType;
import org.apache.log4j.MDC;

import java.util.Arrays;
import java.util.List;

public class AlfredWrapper {

    public static RectNetFixed trainStatic(TrainingMessage trainingMessage, Integer timeoutMillis) {
        ScaleFunctionType scaleFunctionType = ScaleFunctionType.SIGMOID;

        MDC.put("netId", trainingMessage.getNetId());
        MDC.put("trainingTimeLimitSec", timeoutMillis / 1000);
        MDC.put("scaleFunctionType", scaleFunctionType.name());

        try {
            List<String> lines = Arrays.asList(trainingMessage.getData().split("\n"));
            return new RectNetFixed(trainingMessage.getNetId(), lines, scaleFunctionType).train(timeoutMillis, 5);
        } catch (Exception t) { }
        return null;
    }
}
