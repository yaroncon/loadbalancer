package com.infolinks.loadbalancer.api;

import java.util.List;

/**
 * Created with IntelliJ IDEA. User: roeys Date: 1/26/14 Time: 4:17 PM To change this template use File | Settings |
 * File Templates.
 */
public interface LoadBalancerStatistics {

    /**
     * update statistics on this server before the execution of a new request to it
     *
     * @param server
     */
    public void updateServerStatsBeforeExecute(Server server);

    /**
     * update statistics on this server after the execution of a new request to it had returned successfully
     *
     * @param server
     * @param requestStopwatch
     */
    public void updateServerStatsSuccessfulRequest(Server server, Stopwatch requestStopwatch);

    /**
     * update statistics on this server after the execution of a new request to it had failed
     *
     * @param server
     */
    public void updateServerStatsFailedRequest(Server server);

    /**
     * checks whether or not this server should be marked as down, according to the data collected about it's behavior
     *
     * @param server
     *
     * @return should this server be marked as down
     */
    public boolean getShouldMarkServerDown(Server server);

    /**
     * clears the data collected about this server's behavior that indicated if it should be marked as down
     *
     * @param server
     */
    void clearShouldMarkServerDown(Server server);

    /**
     * checks whether or not this server should be marked as up, according to the data collected about it's behavior
     *
     * @param server
     *
     * @return should this server be marked as up
     */
    boolean getShouldMarkServerUp(Server server);

    /**
     * clears the data collected about this server's behavior that indicated if it should be marked as up
     *
     * @param server
     */
    void clearShouldMarkServerUp(Server server);

    /**
     * updates that a request sent in order to check if this server can be mark as up after it has been marked as down
     * had returned successfully
     *
     * @param server
     */
    void updateIsAliveSuccessfulRequest(Server server);

    /**
     * updates that a request sent in order to check if this server can be mark as up after it has been marked as down
     * had failed
     *
     * @param server
     */
    void updateIsAliveFailedRequest(Server server);

    /**
     *
     * @param newServers
     */
    void updateServerStatsList(List<Server> newServers);

    public String reportStatus();

    public MonitorResult getSystemMonitor();
}
