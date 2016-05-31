package com.augurworks.alfred.server;

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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@Controller
public class AlfredEndpoint {

    @Autowired
    private final AlfredService service;

    @Autowired
    public AlfredEndpoint(AlfredService service) {
        this.service = service;
    }

    @RequestMapping(value = "/status/{id}", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody TrainStatus getStatus(@PathVariable String id) {
        System.out.println("Getting status for " + id);
        return service.getStatus(id);
    }

    @RequestMapping(value = "/result/{id}", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody String getResult(@PathVariable String id) {
        System.out.println("Getting result for " + id);
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
    public @ResponseBody String getLogs(@PathVariable String id) throws IOException {
        System.out.println("Getting logs for " + id);
        return new String(Files.readAllBytes(Paths.get("logs/" + id + ".log")));
    }

    @ApiIgnore
    @RequestMapping(value="/", method = RequestMethod.GET)
    public @ResponseBody ModelAndView root() {
        return new ModelAndView(
                new RedirectView("/swagger-ui.html")
        );
    }

}
