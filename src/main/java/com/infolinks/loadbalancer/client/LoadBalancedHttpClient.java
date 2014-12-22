package com.infolinks.loadbalancer.client;

import com.infolinks.loadbalancer.api.LoadBalancer;
import com.infolinks.loadbalancer.api.LoadBalancerStatistics;
import com.infolinks.loadbalancer.api.Server;
import com.infolinks.loadbalancer.lb.statistics.DummyLoadBalancerStatisticsImpl;
import com.infolinks.loadbalancer.utils.LBShouldIgnoreException;
import com.infolinks.loadbalancer.utils.LoadBalancingException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by: yaron
 */
@Component
public class LoadBalancedHttpClient{

    private static final Logger LOG = LoggerFactory.getLogger(LoadBalancedHttpClient.class);
    private ServerGroupManager serverGroupManager;
    private ConnectionManager connectionManager;
    private HttpRequestRetryHandler retryHandler;
    private Set<String> endpointsThatDontUseLoadBalancer = Collections.synchronizedSet(new HashSet<String>());

    @Autowired
    public void setConnectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Autowired
    public void setServerGroupManager(ServerGroupManager serverGroupManager) {
        this.serverGroupManager = serverGroupManager;
    }

    public CloseableHttpResponse execute(HttpRequestBase httpRequest) throws IOException, ClientProtocolException {
        return execute(httpRequest, true);
    }

    public CloseableHttpResponse execute(HttpRequestBase httpRequest, boolean useLoadBalancer) throws IOException, ClientProtocolException {
        HttpContext httpContext = new BasicHttpContext();
        return execute(httpRequest, httpContext);
    }

    public CloseableHttpResponse execute(HttpRequestBase httpRequest, HttpContext httpContext) throws IOException, ClientProtocolException {
        return execute(httpRequest, httpContext, true);
    }

    public CloseableHttpResponse execute(HttpRequestBase httpRequest, HttpContext httpContext, boolean useLoadBalancer) throws IOException, ClientProtocolException {

        CloseableHttpClient httpClient = null;
        CloseableHttpResponse closableResponse = null;

        LoadBalancer loadBalancer = null;
        Server chosenServer = null;
        LoadBalancerStatistics loadBalancerStats = new DummyLoadBalancerStatisticsImpl();
        StopwatchImpl requestStopwatch = new StopwatchImpl();
        if (useLoadBalancer) {
            loadBalancer = this.serverGroupManager.getLoadBalancerForBaseUri(httpRequest.getURI().getHost());
            if (loadBalancer == null) {
                // if we don't have a load balancer configured for this endpoint
                // we keep the url for reference
                if (!this.endpointsThatDontUseLoadBalancer.contains(httpRequest.getURI().getHost())) {
                    this.endpointsThatDontUseLoadBalancer.add(httpRequest.getURI().getHost());
                }
                useLoadBalancer = false;
            } else {
                chosenServer = loadBalancer.chooseServer();
                loadBalancerStats = loadBalancer.getLoadBalancerStatistics();
            }
        }
        try {
            httpClient = HttpClients.custom()
                    .setConnectionManager(connectionManager.getPooledHttpConnection())
                    .setRetryHandler(getRetryHandler())
                    .setDefaultRequestConfig(connectionManager.getRequestConfig())
                    .build();

            // 0. override serverHost if necessary
            if (useLoadBalancer) {
                httpRequest = createLoadBalancedHttpMethodFromRequest(httpRequest, chosenServer);
            }

            // 1. execute the request
            loadBalancerStats.updateServerStatsBeforeExecute(chosenServer);
            requestStopwatch.start();
            closableResponse = httpClient.execute(httpRequest, httpContext);

            requestStopwatch.stop();
            loadBalancerStats.updateServerStatsSuccessfulRequest(chosenServer, requestStopwatch);
            return closableResponse;

        } catch (LBShouldIgnoreException ex) {
            loadBalancerStats.updateServerStatsSuccessfulRequest(chosenServer, requestStopwatch);
            logInvocationFailure(false, httpRequest, ex, requestStopwatch);
            if (ex.getCause() != null && ex.getCause() instanceof Exception) {
                throw new LoadBalancingException("LB: See underlying exception", ex.getCause());
            } else {
                throw ex;
            }
        } catch (Exception ex) {

            /* the load balancer stats collect the server's failures statistics
               and according to them will decide  if the server should be marked as down*/
            loadBalancerStats.updateServerStatsFailedRequest(chosenServer);
            logInvocationFailure(true, httpRequest, ex, requestStopwatch);
            throw new LoadBalancingException("LB: See underlying exception", ex);
        }
    }

    private HttpRequestBase createLoadBalancedHttpMethodFromRequest(HttpRequestBase method, Server chosenServer) throws LBShouldIgnoreException {
        try {
            method.setURI(overrideHostPort(method.getURI(), chosenServer));
            return method;
        } catch (URISyntaxException ex) {
            throw new LBShouldIgnoreException("wrong uri syntax, load balancer should ignore this", ex);
        }
    }

    private URI overrideHostPort(URI uri, Server chosenServer) throws URISyntaxException {
        String query = (uri.getQuery() != null && uri.getQuery() != "") ? "?" + uri.getRawQuery() : "";
        String url = String.format("%s://%s:%d%s%s", uri.getScheme(), chosenServer.getHost(), chosenServer.getPort(), uri.getPath(), query);
        return new URI(url);
    }

    private HttpRequestRetryHandler getRetryHandler() {
        if (this.retryHandler == null) {
            this.retryHandler = new HttpRequestRetryHandler() {

                public boolean retryRequest(
                        IOException exception,
                        int executionCount,
                        HttpContext context) {
                    return false;
                }
            };
        }
        return this.retryHandler;
    }

    private void logInvocationFailure(Boolean markServerIsDown, HttpRequestBase request, Exception ex, StopwatchImpl requestStopwatch) {
        try {
            requestStopwatch.stop();
            if (markServerIsDown) {
                LOG.info("LBServiceInvoker [serverDown={}] error to '{}' : {}. request duration: {}", markServerIsDown, request.getURI(), ex.getMessage(), requestStopwatch.getDuration(), ex);
            } else {
                LOG.info("LBServiceInvoker [serverDown={}] error to '{}' : {}. request duration: {}", markServerIsDown, request.getURI(), ex.getMessage(), requestStopwatch.getDuration());
            }
        } catch (Exception e) {
            LOG.info("LBServiceInvoker [serverDown={}] : {}", markServerIsDown, ex.getMessage(), ex);
        }
    }

}
