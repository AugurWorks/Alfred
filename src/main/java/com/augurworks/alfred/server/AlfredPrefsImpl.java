package com.augurworks.alfred.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.augurworks.alfred.scaling.ScaleFunctions.ScaleFunctionType;

public class AlfredPrefsImpl implements AlfredPrefs {

	private static final Logger log = LoggerFactory.getLogger(AlfredPrefsImpl.class);
	private static final String FILENAME = "alfred.prefs";
	private static final String DIRECTORY_PREF = "LISTEN_DIRECTORY";
	private static final String DIRECTORY_DEFAULT = "nets";
	private static final String NUM_THREADS_PREF = "NUM_THREADS";
	private static final String NUM_THREADS_DEFAULT = "16";
	private static final String TIMEOUT_PREF = "TRAINING_TIMEOUT_SEC";
	private static final String TIMEOUT_DEFAULT = "3600";
	private static final String SCALE_FUNC_PREF = "SCALE_FUNCTION";
	private static final String SCALE_FUNC_DEFAULT = "SIGMOID";
	
	private Properties reload() {
		Properties properties = new Properties();
		FileInputStream in = null;
		try {
			in = new FileInputStream(FILENAME);
			properties.load(in);
		} catch (IOException e) {
			log.error("Unable to read prefs file " + FILENAME, e);
		} finally {
			IOUtils.closeQuietly(in);
		}
		return properties;
	}
	
	@Override
	public String getDirectory() {
		return new File(reload().getProperty(DIRECTORY_PREF, DIRECTORY_DEFAULT)).getAbsolutePath();
	}
	
	@Override
	public int getNumThreads() {
		return parseInt(reload().getProperty(NUM_THREADS_PREF, NUM_THREADS_DEFAULT), NUM_THREADS_DEFAULT);
	}
	
	@Override
	public int getTimeout() {
		return parseInt(reload().getProperty(TIMEOUT_PREF, TIMEOUT_DEFAULT), TIMEOUT_DEFAULT);
	}
	
	@Override
	public ScaleFunctionType getScaleFunction() {
		return ScaleFunctionType.fromString(reload().getProperty(SCALE_FUNC_PREF, SCALE_FUNC_DEFAULT));
	}
	
	private int parseInt(String pref, String defaultValue) {
		try {
			return Integer.parseInt(pref);
		} catch (NumberFormatException e) {
			log.error("Unable to parse integer from " + pref, e);
		}
		// this should throw if it fails.
		return Integer.parseInt(defaultValue);
	}

}
