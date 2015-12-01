package com.augurworks.alfred.server;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AlfredEndpoint {

    @RequestMapping(value="/get/{id}", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody String getStatus(@PathVariable String id) {
        System.out.println(id);
        return "Hello, world! Your input was " + id;
    }

    @RequestMapping(value="/hello", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody String hello() {
    	System.out.println("hello!");
    	return "{\"message\":\"Hello, world!\"}";
    }

}
