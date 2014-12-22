package com.infolinks.loadbalancer.lb;

/**
 * Created by yaron
 * Class copied from the Netflix Ribbon project
 * Under Apache License 2.0
 */

import com.infolinks.loadbalancer.api.Server;

/**
 * Class that represents a typical Server (or an addressable Node) i.e. a
 * Host:port identifier
 */
public class ServerImpl implements Server {

    public static final String UNKNOWN_ZONE = "UNKNOWN";
    String host;
    int port = 80;
    String id;
    boolean isAliveFlag;
    private String zone = UNKNOWN_ZONE;

    public ServerImpl(String host, int port) {
        this.host = host;
        this.port = port;
        this.id = host + ":" + port;
        isAliveFlag = true;
    }

    @Override
    public synchronized void setAlive(boolean isAliveFlag) {
        this.isAliveFlag = isAliveFlag;
    }

    @Override
    public synchronized boolean isAlive() {
        return isAliveFlag;
    }

    @Override
    public void setPort(int port) {
        this.port = port;

        if (host != null) {
            id = host + ":" + port;
        }
    }

    @Override
    public void setHost(String host) {
        if (host != null) {
            this.host = host;
            id = host + ":" + port;
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getHostPort() {
        return host + ":" + port;
    }

    public String toString() {
        return this.getId();
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof ServerImpl))
            return false;
        ServerImpl svc = (ServerImpl) obj;
        return svc.getId().equals(this.getId());

    }

    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (null == this.getId() ? 0 : this.getId().hashCode());
        return hash;
    }

    @Override
    public final String getZone() {
        return zone;
    }

    @Override
    public final void setZone(String zone) {
        this.zone = zone;
    }

}
