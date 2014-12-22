package com.infolinks.loadbalancer.api;

/**
 * Created by yaron
 */
/**
 * Interface that defines a "Rule" for a LoadBalancer. A Rule can be thought of
 * as a Strategy for loadbalacing. Well known loadbalancing strategies include
 * Round Robin, Response Time based etc.
 */
public interface Rule{
    /*
     * choose one alive server from lb.allServers or
     * lb.upServers according to key
     *
     * @return choosen Server object. NULL is returned if none
     *  server is available
     */

    public Server choose();

    public void setLoadBalancer(LoadBalancer lb);

    public LoadBalancer getLoadBalancer();
}