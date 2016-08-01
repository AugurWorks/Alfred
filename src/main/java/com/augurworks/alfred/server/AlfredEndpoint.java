package com.augurworks.alfred.server;

import org.apache.log4j.MDC;
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

import java.util.UUID;

@Controller
public class AlfredEndpoint {

    Logger log = LoggerFactory.getLogger(AlfredEndpoint.class);

    @Autowired
    private final AlfredService service;

    @Autowired
    public AlfredEndpoint(AlfredService service) {
        this.service = service;
    }

    @RequestMapping(value = "/status/{id}", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody TrainStatus getStatus(@PathVariable String id) {
        MDC.put("netId", id);
        log.debug("Getting status for {}", id);
        return service.getStatus(id);
    }

    @RequestMapping(value = "/result/{id}", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody String getResult(@PathVariable String id) {
        MDC.put("netId", id);
        log.debug("Getting result for {}", id);
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

    @RequestMapping(value="/", method = RequestMethod.GET)
    public @ResponseBody ModelAndView root() {
        log.debug("Home page requested");
        return new ModelAndView(
                new RedirectView("/swagger-ui.html")
        );
    }

}
