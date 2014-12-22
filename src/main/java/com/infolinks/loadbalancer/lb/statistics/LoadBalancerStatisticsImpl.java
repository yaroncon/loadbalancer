package com.infolinks.loadbalancer.lb.statistics;

import com.infolinks.loadbalancer.api.LoadBalancer;
import com.infolinks.loadbalancer.api.LoadBalancerStatistics;
import com.infolinks.loadbalancer.api.Server;
import com.infolinks.loadbalancer.api.Stopwatch;
import com.infolinks.loadbalancer.api.MonitorResult;
import com.infolinks.loadbalancer.utils.MonitorResultRange;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA. User: roeys Date: 1/23/14 Time: 6:22 PM To change this template use File | Settings |
 * File Templates.
 */
public class LoadBalancerStatisticsImpl implements LoadBalancerStatistics {

    private final Map<Integer, Integer> failedRequestsCountersConfig;

    private final Map<Integer, Integer> successfulRequestsCountersConfig;

    public LoadBalancerStatisticsImpl( Map<Integer, Integer> failedRequestsCountersConfig,
                                       Map<Integer, Integer> successfulRequestsCountersConfig ) {
        this.failedRequestsCountersConfig = failedRequestsCountersConfig;
        this.successfulRequestsCountersConfig = successfulRequestsCountersConfig;
    }

    HashMap<String, ServerStats> serverStatsMap = new HashMap<String, ServerStats>();

    @Override
    public void updateServerStatsList(List<Server> servers) {
        for (Server server : servers) {
            if (!serverStatsMap.containsKey(server.getId())) {

                serverStatsMap.put(server.getId(), new ServerStats( server, failedRequestsCountersConfig, successfulRequestsCountersConfig));
            }
        }
    }

    private ServerStats getServerStats(Server server) {
        if (server != null) {
            return serverStatsMap.get(server.getId());
        }
        return null;
    }

    @Override
    public synchronized void updateServerStatsBeforeExecute(Server server) {
        ServerStats serverStats = getServerStats(server);
        if (serverStats != null) {
            serverStats.incrementActiveRequestsCount();
        }
    }

    @Override
    public synchronized void updateServerStatsSuccessfulRequest(Server server, Stopwatch requestStopwatch) {
        ServerStats serverStats = getServerStats(server);
        if (serverStats != null && requestStopwatch.wasStrated()) {
            /* if the stopwatch was started it means the incrementActiveRequestsCount was called,
               so we should call decrementActiveRequestsCount */
            serverStats.decrementActiveRequestsCount();
            if (requestStopwatch.wasStartedAndStopped()) {
                /* only if the stopwatch was started and stopped we should update the num or requests and
                 response time stats */
                serverStats.incrementNumRequests();
                serverStats.noteResponseTime(requestStopwatch.getDuration());
            }
        }
        //TODO - here we can add a logic to markServerDown if the the AVg response time is below X
    }

    /**
     * note: if after this failure the server will pass its failures amount per time unit
     * will be marked as "should be shut down" and next time {@link com.infolinks.loadbalancer.lb.pinging.ServerPinger} will detect it and will instruct
     * the loadbalancer to mark at as down *
     */
    @Override
    public synchronized void updateServerStatsFailedRequest(Server server) {
        ServerStats serverStats = getServerStats(server);
        if (serverStats != null) {
            serverStats.decrementActiveRequestsCount();
            serverStats.addToFailuresCount();
        }
    }

    @Override
    public synchronized void updateIsAliveFailedRequest( Server server ) {
        ServerStats serverStats = getServerStats( server );
        if( serverStats != null )
        {
            if (server.isAlive())
            {
                serverStats.addToFailuresCount();
            }
            else
            {
                serverStats.updateIsAliveFailedRequest();
            }
        }
    }

    @Override
    public synchronized void updateIsAliveSuccessfulRequest( Server chosenServer)
    {
        ServerStats serverStats = getServerStats(chosenServer);
        if (serverStats != null)
        {
            serverStats.updateIsAliveSuccessfulRequest();
        }
    }

    @Override
    public synchronized boolean getShouldMarkServerDown( Server server ) {
        ServerStats serverStats = getServerStats( server );
        if( serverStats != null ) {
            return serverStats.getShouldMarkServerAsDown();
        }
        return false;
    }

    @Override
    public synchronized boolean getShouldMarkServerUp( Server chosenServer ) {
        ServerStats serverStats = getServerStats(chosenServer);
        if (serverStats != null) {
            return serverStats.getShouldMarkServerAsUp();
        }
        return false;
    }

    @Override
    public synchronized void clearShouldMarkServerDown( Server chosenServer )
    {
        ServerStats serverStats = getServerStats(chosenServer);
        if (serverStats != null)
        {
            serverStats.clearShouldMarkServerDown();
            /** we also clear the mark server as down data, although I didn't find a scenario in which the data
                should indicate the server to be mark as down and by mistake also as up.
                (because the {@link com.infolinks.rinku.loadbalancer.lb.impl.pinging.ServerPinger} is the only object that creates requests that change a server's status
                from down to up, the only object that can mark server's as up, and it runs in one thread) **/
            serverStats.clearShouldMarkServerUp();
        }
    }

    @Override
    public synchronized void clearShouldMarkServerUp( Server chosenServer )
    {
        ServerStats serverStats = getServerStats(chosenServer);
        if (serverStats != null)
        {
            serverStats.clearShouldMarkServerUp();
            /** we also clear the mark server because there are two objects in the system which can cause the server
                to be marked as down - {@link ServiceInvoker} and {@link com.infolinks.rinku.loadbalancer.lb.impl.pinging.ServerPinger} and the following scenario can happen:
                1. the {@link com.infolinks.rinku.loadbalancer.lb.impl.pinging.ServerPinger}  call to {@link #getShouldMarkServerDown} is "true" because there were x failed
                   requests in the defined time frame.
                2. the server was marked as down by calling {@link LoadBalancer#markServerDownByStatistics}, and in the
                   process {@link #clearShouldMarkServerDown} was called.
                3. Requests to this server sent by {@link ServiceInvoker} before (1) return after (2) has finished.
                   They all fail, so {@link ServerStats#addToFailuresCount()} is called, and again we have a situation
                   were a call to {@link #getShouldMarkServerDown} is true.
                This DOESN'T EFFECT the {@link com.infolinks.rinku.loadbalancer.lb.impl.pinging.ServerPinger#runPinger()}, because next time it will be called, the updated
                server instance in the servers list will return "false" for {@link Server#isAlive()} so the call to
                {@link LoadBalancer#getShouldMarkServerDown} will return "false".
                But it CAN create a situation in some configurations, that this server suddenly response very good, so
                the {@link com.infolinks.rinku.loadbalancer.lb.impl.pinging.ServerPinger} mark him up, and immediately mark him down again because this "old" down data that
                wasn't cleared and is still relevant **/
            serverStats.clearShouldMarkServerDown();
        }
    }

    @Override
    public String reportStatus() {
        StringBuilder sb = new StringBuilder();
        HashMap<String, ServerStats> tempServerStatsMap = this.serverStatsMap;
        for (String serverId : tempServerStatsMap.keySet())
        {
            ServerStats serverStats = tempServerStatsMap.get(serverId);
            sb.append("Server: ").append(serverId)
                    .append(" isDown: ").append(serverStats.getShouldMarkServerAsDown())
                    .append("\r\nActive Requests: ").append(serverStats.getActiveRequestsCount())
                    .append("\r\nTotal Requests: ").append(serverStats.getTotalRequests())
                    .append("\r\nFailed requests: ").append(serverStats.getFailedRequests())
                    .append(" \r\n\r\n");
        }
        return sb.toString();
    }

    @Override
    public MonitorResult getSystemMonitor()
    {
        int okServers = 0;
        int totalServers = 0;
        HashMap<String, ServerStats> tempServerStatsMap = this.serverStatsMap;
        for (String serverId : tempServerStatsMap.keySet())
        {
            ServerStats serverStats = tempServerStatsMap.get(serverId);
            if (!serverStats.getShouldMarkServerAsDown())
            {
                okServers++;
            }
            totalServers++;
        }
        return new MonitorResultRange("LoadBalancer.Servers", totalServers, okServers);
    }
}
