package com.infolinks.loadbalancer.lb.statistics;

import com.infolinks.loadbalancer.api.LoadBalancerStatistics;
import com.infolinks.loadbalancer.api.Server;
import com.infolinks.loadbalancer.api.Stopwatch;
import com.infolinks.loadbalancer.api.MonitorResult;

import java.util.List;

/**
 * Created with IntelliJ IDEA. User: roeys Date: 1/27/14 Time: 7:45 PM To change this template use File | Settings |
 * File Templates.
 */
public class DummyLoadBalancerStatisticsImpl implements LoadBalancerStatistics {

    @Override
    public void updateServerStatsBeforeExecute( Server chosenServer )
    {
    }

    @Override
    public void updateServerStatsSuccessfulRequest( Server chosenServer, Stopwatch requestStopwatch )
    {
    }

    @Override
    public void updateServerStatsFailedRequest( Server chosenServer )
    {
    }

    @Override
    public boolean getShouldMarkServerDown( Server chosenServer )
    {
        return false;
    }

    @Override
    public String reportStatus() {
        return "NA";
    }

    @Override
    public MonitorResult getSystemMonitor() {

        return null;
    }

    @Override
    public void updateIsAliveFailedRequest( Server chosenServer ) {

    }

    @Override
    public void updateIsAliveSuccessfulRequest( Server chosenServer ) {

    }

    @Override
    public boolean getShouldMarkServerUp( Server chosenServer ) {
        return false;
    }

    @Override
    public void clearShouldMarkServerDown( Server currentServer ) {

    }

    @Override
    public void clearShouldMarkServerUp( Server currentServer ) {

    }

    @Override
    public void updateServerStatsList( List<Server> servers ) {


    }
}
