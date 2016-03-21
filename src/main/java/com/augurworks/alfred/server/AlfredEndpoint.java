package com.augurworks.alfred.server;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AlfredEndpoint {

    private static final Logger log = LoggerFactory.getLogger(AlfredEndpoint.class);

    public enum TrainStatus {
        UNKNOWN,
        IN_PROGRESS,
        COMPLETE;
    }

    @RequestMapping(value="/status/{id}", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody TrainStatus getStatus(@PathVariable String id) {
        File trainFileForId = getTrainFileForId(id);
        File outFileForId = getOutputFileForId(id);
        if (outFileForId.exists()) {
            return TrainStatus.COMPLETE;
        } else if (trainFileForId.exists()) {
            return TrainStatus.IN_PROGRESS;
        } else {
            return TrainStatus.UNKNOWN;
        }
    }

    @RequestMapping(value="/result/{id}", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody String getResult(@PathVariable String id) {
        TrainStatus status = getStatus(id);
        if (status != TrainStatus.COMPLETE) {
            return "IN_PROGRESS";
        }
        File outFileForId = getOutputFileForId(id);
        try {
            return FileUtils.readFileToString(outFileForId);
        } catch (IOException e) {
            log.error("Unable to read result file for ID " + id, e);
            return "UNKNOWN";
        }
    }

    @RequestMapping(value="/train", method = RequestMethod.POST, produces = "application/json")
    public @ResponseBody String train(@RequestBody String net) {
        String id = UUID.randomUUID().toString();
        File fileForId = getTrainFileForId(id);
        try {
            FileUtils.writeStringToFile(fileForId, net);
            return id;
        } catch (IOException e) {
            log.error("Unable to write file " + fileForId + " with contents:\n" + net, e);
            throw new IllegalStateException("Unable to store net.");
        }
    }

    @RequestMapping(value="/logs/{id}", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody String getLogs(@PathVariable String id) {
        Collection<File> logFiles = getLogsForId(id);
        JSONObject object = new JSONObject();
        for (File file : logFiles) {
            addFile(object, file);
        }
        return object.toString(2);
    }

    private void addFile(JSONObject object, File file) {
        try {
            if (file.isFile()) {
                object.put(file.getName(), FileUtils.readFileToString(file));
            }
        } catch (IOException e) {
            log.error("Unable to read file " + file + ". Skipping it and moving on.", e);
            object.put(file.getName(), "ERROR: Unable to read file: " + e.getMessage());
        }
    }

    private Collection<File> getLogsForId(String id) {
        return FileUtils.listFiles(new File("logs/" + id + "/"), null /* accept all file extensions */, true /* recurse */);
    }

    private File getTrainFileForId(String id) {
        return new File("nets/" + id + ".augtrain");
    }

    private File getOutputFileForId(String id) {
        return new File("nets/" + id + ".augtrain.augout");
    }

    @RequestMapping(value="/hello", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody String hello() {
        System.out.println("hello!");
        return "{\"message\":\"Hello, world!\"}";
    }

    @RequestMapping(value="/", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody String root() {
        return "{\"message\":\"Alfred is working!\"}";
    }

}
