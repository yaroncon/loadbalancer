package com.infolinks.loadbalancer.lb.pinging;

import java.util.UUID;
import java.util.concurrent.ThreadFactory;

/**
 * Created by roeys
 */
public class ExecutorsThreadFactory implements ThreadFactory {

    protected int counter = 0;

    protected String threadFactoryUniqueId = "";

    public ExecutorsThreadFactory() {

        this.threadFactoryUniqueId = generateFactoryUniqueId();
    }

    /* creates a unique prefix for */
    private String generateFactoryUniqueId() {
        Long uuid = UUID.randomUUID().getMostSignificantBits();
        return uuid.toString().substring(1,4);
    }

    @Override
    public Thread newThread( Runnable runnable ) {
        String counterPrefix;
        //we want that the number will always have 2 digits, so if it is under 10 we add a "0" before
        if( counter < 10 ) {
            counterPrefix = "0";
        } else {
            counterPrefix = "";
        }
        return new Thread( runnable, "Ping" + threadFactoryUniqueId + "-" + counterPrefix + counter++ );
    }
}