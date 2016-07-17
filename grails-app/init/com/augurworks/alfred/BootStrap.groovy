package com.augurworks.alfred

import com.augurworks.alfred.services.MessagingService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BootStrap {

    Logger log = LoggerFactory.getLogger(BootStrap.class);

    MessagingService messagingService

    def init = { servletContext ->
        log.info "Starting up Alfred"
        messagingService.init()
    }

    def destroy = {
        log.warn "Shutting down Alfred"
    }
}
