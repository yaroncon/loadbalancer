package com.infolinks.loadbalancer.lb;

/**
 * Created by yaron
 */

import com.infolinks.loadbalancer.api.LoadBalancer;
import com.infolinks.loadbalancer.api.Rule;
import com.infolinks.loadbalancer.api.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The most well known and basic loadbalacing strategy, i.e. Round Robin Rule.
 */
public class RoundRobinRule implements Rule {
    AtomicInteger nextIndexAI;

    private LoadBalancer lb;

    protected final Logger LOG = LoggerFactory.getLogger(RoundRobinRule.class);

    public RoundRobinRule() {
        nextIndexAI = new AtomicInteger(0);
    }

    public RoundRobinRule(LoadBalancer lb) {
        this();
        setLoadBalancer(lb);
    }

    /*
     * Rotate over all known servers.
     */
    final static boolean availableOnly = false;

    public Server choose(LoadBalancer lb) {
        if (lb == null) {
            LOG.warn("no load balancer");
            return null;
        }
        Server server = null;
        int index = 0;

        int count = 0;
        while (server == null && count++ < 10) {
            List<Server> upList = lb.getServerList(true);
            List<Server> allList = lb.getServerList(false);
            int upCount = upList.size();
            int serverCount = allList.size();

            if (serverCount == 0) {
                LOG.error("LOAD BALANCER IS NOT CONFIGURED. SERVER COUNT IS ZERO!!!");
                return null;
            }
            if (upCount == 0) {
                LOG.warn("No up servers available from load balancer: " + lb + " resulting to random server");
                index = new Random().nextInt(allList.size());
                return allList.get(index);
            }

            index = nextIndexAI.incrementAndGet() % serverCount;
            server = allList.get(index);

            if (server.isAlive()) {
                return (server);
            }

            // Next.
            server = null;
        }

        if (count >= 10) {
            LOG.warn("No available alive servers after 10 tries from load balancer: "
                    + lb);
        }
        return server;
    }

    @Override
    public Server choose() {
        return choose(getLoadBalancer());
    }

    @Override
    public void setLoadBalancer(LoadBalancer lb) {
        this.lb = lb;
    }

    @Override
    public LoadBalancer getLoadBalancer() {
        return this.lb;
    }

}
