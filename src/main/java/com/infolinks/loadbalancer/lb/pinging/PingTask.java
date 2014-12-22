package com.infolinks.loadbalancer.lb.pinging;

import com.infolinks.loadbalancer.api.LoadBalancer;
import com.infolinks.loadbalancer.api.Server;

import java.util.TimerTask;

/**
 * Created by roeys
 */
public class PingTask extends TimerTask {

    private final LoadBalancer loadBalancer;

    private final Server server;

    private ServerPinger serverPinger = null;

    public PingTask( LoadBalancer loadBalancer, Server server ) {

        this.loadBalancer = loadBalancer;
        this.server = server;
    }

    public void run() {
        if( this.serverPinger == null ) {
            this.serverPinger = new ServerPinger( loadBalancer, server );
        }
        this.serverPinger.runPinger();
    }
}