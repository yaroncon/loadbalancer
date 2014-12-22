package com.infolinks.loadbalancer.lb.pinging;

/**
 * Created with IntelliJ IDEA. User: roeys Date: 1/27/14 Time: 2:38 PM To change this template use File | Settings |
 * File Templates.
 */

import com.infolinks.loadbalancer.api.LoadBalancer;
import com.infolinks.loadbalancer.api.Server;
import com.infolinks.loadbalancer.client.LoadBalancedHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * Class that contains the mechanism to "ping" a {@link Server}
 * <p/>
 * Created by roeys
 */
public class ServerPinger {

    private static final Logger LOG = LoggerFactory.getLogger(ServerPinger.class);

    private static final String PATH_TO_IS_ALIVE_SERVICE = "/check-is-alive";
    private static final String SERVER_IS_ALIVE = "Running";

    private final LoadBalancer loadBalancer;

    private final Server server;

    private static final int PINGER_CONNECTION_TIMEOUT = 10000;

    public ServerPinger(LoadBalancer loadBalancer, Server server) {
        this.loadBalancer = loadBalancer;
        this.server = server;
    }

    public synchronized void runPinger() {
        try {

            /* 1. we check if the currentServer from the list should be marked as down
                          if it should, we call the load balancer to do it and also reset the statistics*/
            if (loadBalancer.getShouldMarkServerDown(server)) {
                loadBalancer.markServerDownByStatistics(server);
                LOG.info("ServerPinger marked server as down: {}", server.getId());
            }
            /* 2. we check if the currentServer from the list should be marked as up
                  if it should we call the load balancer to do it and reset the statistics*/
            else {
                if (loadBalancer.getShouldMarkServerUp(server)) {
                    loadBalancer.markServerUpByStatistics(server);
                    LOG.info("ServerPinger marked server as up: {}", server.getId());
                }
            }
            /* 3. no matter if we marked the currentServer as up or down or none of them - we should call isAlive:
                  for up servers (3.1) - it can bring them down faster
                  for down server (3.2) - it's crucial for setting it up (it cannot generate calls by itself)
                  */
            boolean pingSuccessful = checkIsAlive(server);
            if (LOG.isDebugEnabled())
                LOG.debug("ServerPinger sent \"isAlive\" request to server {}, and the result was: {}", server.getId(), pingSuccessful);
            if (server.isAlive() && !pingSuccessful) {
                /* 3.1 the server is UP and the ping indicates NOT ALIVE we update the server's stats
                   The logic behind this update is that the ServerPinger helps to bring down bad servers */
                loadBalancer.getLoadBalancerStatistics().updateIsAliveFailedRequest(server);
                if (LOG.isDebugEnabled())
                    LOG.debug("ServerPinger found that UP server {}, FAILED to response to isAlive request", server.getId());
            } else {
                // 3.2 the server is DOWN
                if (!server.isAlive()) {
                    // 3.2.1 the ping indicates ALIVE - we update the server's stats in order to bring him up
                    if (pingSuccessful) {
                        loadBalancer.getLoadBalancerStatistics().updateIsAliveSuccessfulRequest(server);
                        if (LOG.isDebugEnabled())
                            LOG.debug("ServerPinger found that DOWN server {}, SUCCEEDED to response to isAlive request", server.getId());
                    }
                            /* 3.2.2 the ping indicates NOT ALIVE - we update the server's stats
                                     (because we want X consecutive successful pings) */
                    else {
                        loadBalancer.getLoadBalancerStatistics().updateIsAliveFailedRequest(server);
                        if (LOG.isDebugEnabled())
                            LOG.debug("ServerPinger found that DOWN server {}, FAILED to response to isAlive request", server.getId());
                    }
                }
            }

        } catch (Throwable t) {
            LOG.warn("ServerPinger could not run", t);
        }
    }

    private boolean checkIsAlive(Server currentServer) {
        String response = null;
        try {
            response = invokeEndpoint("http://" + currentServer.getHostPort() + PATH_TO_IS_ALIVE_SERVICE);
            if (response.contains(SERVER_IS_ALIVE)) {
                return true;
            } else {
                return false;
            }

        } catch (IOException e) {
            return false;
        } catch (IllegalStateException ex) {
            return false;
        }
    }

    public String invokeEndpoint(String requestUrlStr) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL requestUrl = new URL( requestUrlStr );
            connection = ( HttpURLConnection ) requestUrl.openConnection();

            connection.setRequestMethod( "GET" );
            connection.setConnectTimeout(PINGER_CONNECTION_TIMEOUT);
            connection.connect();
            int responseCode = connection.getResponseCode();
            if( responseCode != HttpURLConnection.HTTP_OK ) {
                throw new IllegalStateException( String.format(
                        "Response from Pinger was %s",
                        connection.getResponseMessage() ) );
            }
            // parse the response XML and write the report ID to the log
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document doc = builder.parse( connection.getInputStream() );

            NodeList id = doc.getElementsByTagName( "data" );
            return id.item( 0 ).getTextContent();
        } catch( Exception ex ) {
            LOG.info("Pinger failed to call server: {}", requestUrlStr, ex);
            throw new IOException("Pinger failed", ex ); // Do something nicer here
        } finally {
            if( connection != null ) {
                connection.disconnect();
            }
        }
    }
}

