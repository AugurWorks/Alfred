package com.augurworks.alfred.server;

import com.augurworks.alfred.scaling.ScaleFunctions.ScaleFunctionType;

public interface AlfredPrefs {

	String getDirectory();

	int getNumThreads();

	int getTimeout();

	ScaleFunctionType getScaleFunction();

}