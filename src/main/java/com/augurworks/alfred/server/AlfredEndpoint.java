package com.augurworks.alfred.server;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
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
	private AlfredPrefs prefs;
	
	public void setPrefs(AlfredPrefs prefs) {
		this.prefs = prefs;
	}

    @RequestMapping(value="/get/{id}", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody String getStatus(@PathVariable String id) {
        File fileForId = getFileForId(id);
        if (fileForId.exists()) {
        	try {
				return FileUtils.readFileToString(fileForId);
			} catch (IOException e) {
				log.error("Unable to read file " + fileForId, e);
				return null;
			}
        } else {
        	return null;
        }
    }
    
    @RequestMapping(value="/train", method = RequestMethod.POST, produces = "application/json")
    public @ResponseBody String train(@RequestBody String net) {
    	String id = UUID.randomUUID().toString();
    	File fileForId = getFileForId(id);
    	try {
			FileUtils.writeStringToFile(fileForId, net);
			return id;
		} catch (IOException e) {
			log.error("Unable to write file " + fileForId + " with contents:\n" + net, e);
			throw new IllegalStateException("Unable to store net.");
		}
    }

	private File getFileForId(String id) {
		return new File(prefs.getDirectory() + "/" + id);
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
