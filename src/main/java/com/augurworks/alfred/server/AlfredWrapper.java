package com.augurworks.alfred.server;

import com.augurworks.alfred.RectNetFixed;
import com.augurworks.alfred.scaling.ScaleFunctions.ScaleFunctionType;
import org.apache.log4j.MDC;

import java.util.Arrays;
import java.util.List;

public class AlfredWrapper {

    public static RectNetFixed trainStatic(String netId, String augtrain, Integer timeoutMillis) {
        ScaleFunctionType scaleFunctionType = ScaleFunctionType.SIGMOID;

        MDC.put("netId", netId);
        MDC.put("trainingTimeLimitSec", timeoutMillis / 1000);
        MDC.put("scaleFunctionType", scaleFunctionType.name());

        try {
            List<String> lines = Arrays.asList(augtrain.split("\n"));
            return new RectNetFixed(netId, lines, scaleFunctionType).train(timeoutMillis, 5);
        } catch (Exception t) { }
        return null;
    }
}
