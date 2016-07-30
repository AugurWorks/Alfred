package com.augurworks.alfred

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BootStrap {

    Logger log = LoggerFactory.getLogger(BootStrap.class);

    def init = { servletContext ->
        log.info "Starting up Alfred"
    }

    def destroy = {
        log.warn "Shutting down Alfred"
    }
}
