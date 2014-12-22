package com.infolinks.loadbalancer.lb;

import com.infolinks.loadbalancer.api.LoadBalancer;
import com.infolinks.loadbalancer.api.LoadBalancerStatistics;
import com.infolinks.loadbalancer.api.Rule;
import com.infolinks.loadbalancer.api.Server;
import com.infolinks.loadbalancer.client.LoadBalancedHttpClient;
import com.infolinks.loadbalancer.lb.pinging.ExecutorsThreadFactory;
import com.infolinks.loadbalancer.lb.pinging.PingTask;
import com.infolinks.loadbalancer.lb.statistics.LoadBalancerStatisticsImpl;
import com.infolinks.loadbalancer.api.MonitorResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by yaron
 */

public class BaseLoadBalancer implements LoadBalancer {

    private static final Logger LOG = LoggerFactory.getLogger( BaseLoadBalancer.class );

    private final boolean createServersPingers;

    private ArrayList<Server> servers = new ArrayList<Server>();

    private Rule rule = new RoundRobinRule(this);

    private ArrayList<Server> availableServers = new ArrayList<Server>();
    private ArrayList<Server> allServers = new ArrayList<Server>();

    protected LoadBalancerStatistics lbStats;

    protected ScheduledExecutorService scheduler = null;

    protected int PING_INTERVAL_SECONDS = 10;

    protected int PING_INITIAL_DELAY_SECONDS = 30;

    private static final String PINGER_THREAD_NAME_PREFIX = "LoadBalancer-ServerPinger-";

    public BaseLoadBalancer( Map<Integer, Integer> failedRequestsCountersConfig,
                             Map<Integer, Integer> successfulRequestsCountersConfig, boolean createServersPingers ) {

        lbStats = new LoadBalancerStatisticsImpl(failedRequestsCountersConfig, successfulRequestsCountersConfig);
        this.createServersPingers = createServersPingers;
    }

    private void setupPingTask( ArrayList<Server> servers ) {
        if (createServersPingers)
        {
            this.killPingTasks();
            //TODO - investigate about Netflix's ShutdownEnabledTimer, and when it is shutdown?

            scheduler = Executors.newScheduledThreadPool(
                    servers.size(), new ExecutorsThreadFactory());
            for (Server currentServer : servers) {
                scheduler.scheduleWithFixedDelay(
                        new PingTask( this, currentServer ),
                        PING_INITIAL_DELAY_SECONDS,
                        PING_INTERVAL_SECONDS,
                        TimeUnit.SECONDS );
            }
        }
    }


    @Override
    public void killPingTasks() {
        if( scheduler != null ){
            scheduler.shutdown();
        }
    }

    @Override
    public synchronized void setServers(List<Server> newServers) {
        ArrayList<Server> tempServers = new ArrayList<Server>();
        if (newServers != null) {
            tempServers.addAll(newServers);
        }
        this.servers = tempServers;
        this.lbStats.updateServerStatsList(this.servers);
        this.setupPingTask(this.servers);
        evaluateActiveServers();

    }

    @Override
    public Server chooseServer() {
        return rule.choose();
    }

    @Override
    public synchronized boolean getShouldMarkServerDown( Server currentServer )
    {
        /* In 2 conditions:
           1. the server is currently alive (NOT marked as down)
           2. statistics indicates it should be marked as down */
        return currentServer.isAlive() && lbStats.getShouldMarkServerDown(currentServer);
    }

    @Override
    public synchronized boolean getShouldMarkServerUp( Server currentServer ) {
          /* In 2 conditions:
           1. the server is currently down (NOT Alive)
           2. statistics indicates it should be marked as up */
        return (!currentServer.isAlive() && lbStats.getShouldMarkServerUp(currentServer));
    }

    @Override
    public void markServerDown(Server server) {
        server.setAlive(false);
        evaluateActiveServers();
    }

    @Override
    public synchronized void markServerDownByStatistics( Server currentServer ) {
        markServerDown(currentServer);
        /** we reset the Server Down marker, so next time the {@link ServerPinger} will run we will not mark it as down again **/
        lbStats.clearShouldMarkServerDown( currentServer );
    }

    @Override
    public synchronized void markServerUp( Server server )
    {
        server.setAlive(true);
        evaluateActiveServers();
    }

    @Override
    public synchronized void markServerUpByStatistics( Server currentServer )
    {
        markServerUp(currentServer);
        /** we reset Server up marker, so next time the {@link ServerPinger} will run we will not mark it as up again **/
        lbStats.clearShouldMarkServerUp( currentServer );
    }


    @Override
    public List<Server> getServerList(boolean availableOnly) {
        if (availableOnly) {
            return this.availableServers;
        } else {
            return this.allServers;
        }
    }

    @Override
    public LoadBalancerStatistics getLoadBalancerStatistics() {
        return lbStats;
    }

    @Override
    public String reportStatus() {
        return this.lbStats.reportStatus();
    }

    @Override
    public MonitorResult getSystemMonitor()
    {
        return this.lbStats.getSystemMonitor();
    }

    private void evaluateActiveServers() {
        ArrayList<Server> tempAvailableServers = new ArrayList<Server>();
        ArrayList<Server> tempAllServers = new ArrayList<Server>();
        for (Server server : this.servers) {
            tempAllServers.add(server);
            if (server.isAlive()) {
                tempAvailableServers.add(server);
            }
        }
        this.availableServers = tempAvailableServers;
        this.allServers = tempAllServers;
    }
}
