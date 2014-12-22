package com.infolinks.loadbalancer.api;

import java.util.List;

/**
 * Created by yaron
 * Interface copied from the Netflix Ribbon project
 * Under Apache License 2.0
 */

/**
 * Interface that defines the operations for a software loadbalancer. A typical
 * loadbalancer minimally need a set of servers to loadbalance for, a method to
 * mark a particular server to be out of rotation and a call that will choose a
 * server from the existing list of server.
 */
public interface LoadBalancer {

    /**
     * Initial list of servers.
     * The same logical server (host:port) could essentially be added multiple times
     * (helpful in cases where you want to give more "weightage" perhaps ..)
     *
     * @param newServers new servers to add
     */
    public void setServers(List<Server> newServers);

    /**
     * Choose a server from load balancer.
     *
     * @return server chosen
     */
    public Server chooseServer();

    /**
     * To be called by the clients of the load balancer to notify that a Server is down
     * else, the LB will think its still Alive until the next Ping cycle - potentially
     * (assuming that the LB Impl does a ping)
     *
     * @param server Server to mark as down
     */
    public void markServerDown(Server server);

    /**
     * To be called by the clients of the load balancer to notify that a Server is up
     * else, the LB will think its still Alive until the next Ping cycle - potentially
     * (assuming that the LB Impl does a ping)
     *
     * @param server Server to mark as up
     */
    public void markServerUp(Server server);

    /**
     * To be called when {@link LoadBalancerStatistics#getShouldMarkServerDown(Server)}
     * is true
     * It does the same functionality as {@link this#markServerDown(Server)}, but also clear the statistics that led to
     * the server to be marked as down
     *
     * @param server Server to mark as down
     */
    public void markServerDownByStatistics(Server server);

    /**
     * To be called when {@link LoadBalancerStatistics#getShouldMarkServerUp(Server)}}
     * is true
     * It does the same functionality as {@link this#markServerUp(Server)}, but also clear the statistics that led to
     * the server to be marked as down
     *
     * @param server Server to mark as up
     */
    void markServerUpByStatistics(Server server);


    /**
     * Get the current list of servers.
     *
     * @param availableOnly if true, only live and available servers should be returned
     */
    public List<Server> getServerList(boolean availableOnly);


    /**
     * Get the object which is responsible for managing the server's statistics
     *
     * @return  LoadBalancerStatistics
     */
    public LoadBalancerStatistics getLoadBalancerStatistics();

    /**
     * checks whether or not this server should be marked as down
     *
     * @param server
     *
     * @return should this server be marked as down
     */
    boolean getShouldMarkServerDown(Server server);

    /**
     * checks whether or not this server should be marked as up
     *
     * @param server
     *
     * @return should this server be marked as up
     */
    boolean getShouldMarkServerUp(Server server);

    public void killPingTasks();

    public String reportStatus();

    public MonitorResult getSystemMonitor();
}
