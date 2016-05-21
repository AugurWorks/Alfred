package com.augurworks.alfred.server;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import springfox.documentation.annotations.ApiIgnore;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

@Controller
public class AlfredEndpoint {

    private static final Logger log = LoggerFactory.getLogger(AlfredEndpoint.class);

    @Autowired
    private final AlfredService service;

    @Autowired
    public AlfredEndpoint(AlfredService service) {
        this.service = service;
    }

    @RequestMapping(value = "/status/{id}", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody TrainStatus getStatus(@PathVariable String id) {
        return service.getStatus(id);
    }

    @RequestMapping(value = "/result/{id}", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody String getResult(@PathVariable String id) {
        TrainStatus status = getStatus(id);
        if (status != TrainStatus.COMPLETE) {
            return "IN_PROGRESS";
        }
        return service.getResult(id);
    }

    @RequestMapping(value = "/train", method = RequestMethod.POST, produces = "application/json")
    public @ResponseBody String train(@RequestBody String net) {
        String id = UUID.randomUUID().toString();
        service.train(id, net);
        return id;
    }

    @RequestMapping(value = "/stats", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody String getStats() {
        return service.getStats();
    }

    @RequestMapping(value = "/logs/{id}", method = RequestMethod.GET, produces = "application/json")
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

    @ApiIgnore
    @RequestMapping(value="/", method = RequestMethod.GET)
    public @ResponseBody ModelAndView root() {
        return new ModelAndView(
                new RedirectView("/swagger-ui.html")
        );
    }

}
