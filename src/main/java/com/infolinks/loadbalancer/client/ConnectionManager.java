package com.infolinks.loadbalancer.client;
import com.infolinks.loadbalancer.utils.PropertiesAccessor;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


/**
 * Created by yaron
 */
@Component
public class ConnectionManager {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionManager.class);

    private PoolingHttpClientConnectionManager cm = null;
    private RequestConfig requestConfig;
    private static final int DEFAULT_MAX_PER_ROUTE = 100;
    private static final int DEFAULT_MAX_TOTAL_CONNECTIONS = 200;
    private static final int DEFAULT_TIMEOUT = 10 * 1000;

    public synchronized void configure(PropertiesAccessor config) {
        int maxTotalConnections = config.get("client.max.total.connections", Integer.class, DEFAULT_MAX_TOTAL_CONNECTIONS);
        int maxConnectionsPerRoute = config.get("client.max.per.route", Integer.class, DEFAULT_MAX_PER_ROUTE);

        PoolingHttpClientConnectionManager tempCm = new PoolingHttpClientConnectionManager();
        tempCm.setMaxTotal(maxTotalConnections);
        tempCm.setDefaultMaxPerRoute(maxConnectionsPerRoute);
        this.cm = tempCm;

        Integer socketTimeout = config.get("client.conn.socketTimeout", Integer.class, DEFAULT_TIMEOUT);
        Integer connectTimeOut = config.get("client.conn.connectTimeout", Integer.class, DEFAULT_TIMEOUT);
        Integer requestTimeout = config.get("client.conn.requestTimeout", Integer.class, DEFAULT_TIMEOUT);

        this.requestConfig = createRequestConfig(socketTimeout, connectTimeOut, requestTimeout);
        LOG.info("changing the load balanced http client configuration: {}, {}, {}", socketTimeout, connectTimeOut, requestTimeout);
    }

    public PoolingHttpClientConnectionManager getPooledHttpConnection() throws Exception {
        if (this.cm == null) {
            throw new Exception("no connection manager is available");
        }
        return this.cm;
    }

    public RequestConfig getRequestConfig() {
        if (this.requestConfig == null) {
            this.requestConfig = createRequestConfig(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT);
        }
        return this.requestConfig;
    }

    private RequestConfig createRequestConfig(int socketTimeout, int connectTimeOut, int requestTimeout) {
        return RequestConfig.custom()
                        /* sets the timeout for waiting for data
                            (or, put differently, a maximum period inactivity between two consecutive data packets) */
                .setSocketTimeout(socketTimeout)
                        // Determines the timeout in milliseconds until a connection is established
                .setConnectTimeout(connectTimeOut)
                        //sets the timeout in milliseconds used when requesting a connection from the connection manager
                .setConnectionRequestTimeout(requestTimeout)
                        /* Determines whether stale connection check is to be used.
                            The stale connection check can cause up to 30 millisecond overhead per request and
                            should be used only when appropriate.
                            For performance critical operations this check should be disabled.*/
                .setStaleConnectionCheckEnabled(true)
                .build();
    }

}
