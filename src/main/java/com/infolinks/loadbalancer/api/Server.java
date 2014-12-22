package com.infolinks.loadbalancer.api;

/**
 * Created by yaron
 */
public interface Server {
    void setAlive(boolean isAliveFlag);

    boolean isAlive();

    void setPort(int port);

    void setHost(String host);

    String getId();

    String getHost();

    int getPort();

    String getHostPort();

    String getZone();

    void setZone(String zone);
}
