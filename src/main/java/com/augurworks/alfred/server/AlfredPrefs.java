package com.augurworks.alfred.server;

import com.augurworks.alfred.scaling.ScaleFunctions.ScaleFunctionType;

public interface AlfredPrefs {

    int getNumThreads();

    int getTimeout();

    boolean getVerbose();

    ScaleFunctionType getScaleFunction();

}