package com.infolinks.loadbalancer.client;

import com.infolinks.loadbalancer.api.LoadBalancer;
import com.infolinks.loadbalancer.api.MonitorResult;
import com.infolinks.loadbalancer.api.Server;
import com.infolinks.loadbalancer.lb.BaseLoadBalancer;
import com.infolinks.loadbalancer.lb.ServerImpl;
import com.infolinks.loadbalancer.utils.MonitorResultRange;
import com.infolinks.loadbalancer.utils.PropertiesAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by yaron
 */
@Component
public class ServerGroupManager {

    private static final String DELIMITER_FOR_COUNTERS_CONFIG_PAIRS = ",";

    private static final String REGULAR_EXPRESSION_TO_CHECK_CONFIG_VALIDITY = "\\[[0-9]+,[0-9]+\\](\\s*,\\s*\\[[0-9]+,[0-9]+\\])*";

    private static final String REGULAR_EXPRESSION_TO_PREFIX_AND_SUFFIX = "(^.*?\\[|\\]\\s*$)";

    private static final String REGULAR_EXPRESSION_FOR_DELIMITER = "\\]\\s*,\\s*\\[";

    private static final String DEFAULT_FAILED_REQUESTS_COUNTER_CONFIG = "[5,60],[10,300],[20,900]";
    private static final String DEFAULT_SUCCESSFUL_REQUESTS_COUNTER_CONFIG = "[10,600]";
    private static final String FAILED_REQUESTS_COUNTERS_CONFIG_DB_KEY = "requests.counters.failed";
    private static final String SUCCESSFUL_REQUESTS_COUNTERS_CONFIG_DB_KEY = "requests.counters.successful.isAlive";
    private static final String CREATE_SERVERS_PINGERS = "create.servers.pingers";

    private static final String SERVER_IDENTIFIER = "lbServer.";

    private HashMap<String, LoadBalancer> balancers = new HashMap<String, LoadBalancer>();

    private static final Logger LOG = LoggerFactory.getLogger(ServerGroupManager.class);

    public synchronized void configure(PropertiesAccessor config) {
        HashMap<String, LoadBalancer> tempBalancers = new HashMap<String, LoadBalancer>();

        //extract the configurations for failed requests counters from the DB
        String failedRequestsCountersConfigFromDB = config.get( FAILED_REQUESTS_COUNTERS_CONFIG_DB_KEY, String.class , DEFAULT_FAILED_REQUESTS_COUNTER_CONFIG );
        Map<Integer,Integer> failedRequestsCountersConfig =
                parseCountersConfig( failedRequestsCountersConfigFromDB, FAILED_REQUESTS_COUNTERS_CONFIG_DB_KEY, DEFAULT_FAILED_REQUESTS_COUNTER_CONFIG);

        //extract the configurations for successful "isAlive" requests counters from the DB
        String successfulRequestsCountersConfigFromDB = config.get( SUCCESSFUL_REQUESTS_COUNTERS_CONFIG_DB_KEY, String.class, DEFAULT_SUCCESSFUL_REQUESTS_COUNTER_CONFIG );
        Map<Integer,Integer> successfulRequestsCountersConfig =
                parseCountersConfig( successfulRequestsCountersConfigFromDB, SUCCESSFUL_REQUESTS_COUNTERS_CONFIG_DB_KEY, DEFAULT_SUCCESSFUL_REQUESTS_COUNTER_CONFIG);

        boolean createServersPingers = config.get( CREATE_SERVERS_PINGERS, Boolean.class, false );

        for (String key : config.keySet()) {
            try {
                if (key.startsWith(SERVER_IDENTIFIER)) {
                    String baseUri = key.substring(SERVER_IDENTIFIER.length());
                    LOG.info("loading/reloading Load Balancer list of servers for {}", baseUri);
                    LoadBalancer lb =
                            new BaseLoadBalancer(config, failedRequestsCountersConfig, successfulRequestsCountersConfig, createServersPingers);
                    lb.setServers(extractServerListFrom(config.get(key, String.class)));
                    tempBalancers.put(baseUri, lb);
                }
            } catch (Exception e) {
                LOG.error("error when reloading Load Balancer list of servers", e);
            }
        }
        // saveing the old balancer so we can kill the ServerPinger
        HashMap<String, LoadBalancer> oldBalancers = this.balancers;
        // replacing the old balancer list with the new
        this.balancers = tempBalancers;
        // now killking the ServerPinger for the old balancers
        String problematicKey = "NA";
        try
        {
            for (String key : oldBalancers.keySet())
            {
                LoadBalancer loadBalancer = oldBalancers.get(key);
                problematicKey = key;
                loadBalancer.killPingTasks();
            }
        }
        catch( Exception ex)
        {
             LOG.warn("unable to kill the Ping task for balancer: {}", problematicKey, ex);
        }
    }

    /**
     * we turn a string which looks like  "[5,60],[10,300],[20,900]"
     * into a HashMap in which each first element in the pair is the key and the second element in the pair is the value
     *
     *
     * @param countersConfigRaw
     * @param dataKeyInDB
     * @param defaultValue
     * @return
     */
    private Map<Integer, Integer> parseCountersConfig( String countersConfigRaw,
                                                       String dataKeyInDB,
                                                       String defaultValue ) {
        Map<Integer, Integer> countersConfig =  new Hashtable<Integer, Integer>();
        //remove leading and trailing whitespace
        countersConfigRaw =  countersConfigRaw.trim();
        if (!Pattern.matches( REGULAR_EXPRESSION_TO_CHECK_CONFIG_VALIDITY, countersConfigRaw))
        {
            LOG.warn("Failed to parse the counters config with data key: {} because it's format was invalid: {}." +
                             " INSTEAD WE USE THE DEFAULT CONFIG",dataKeyInDB, countersConfigRaw);
            countersConfigRaw = defaultValue;
        }

        String countersConfigWithoutPrefixAndSuffix = countersConfigRaw.replaceAll(REGULAR_EXPRESSION_TO_PREFIX_AND_SUFFIX,"");
        String[] configPairs = countersConfigWithoutPrefixAndSuffix.split(REGULAR_EXPRESSION_FOR_DELIMITER);
        for (String currentConfigPair : configPairs)
        {
            try
            {
                String[] keyVal =  currentConfigPair.split(DELIMITER_FOR_COUNTERS_CONFIG_PAIRS);
                /* we check this although expressions that had passed the regular
                    expressions MUST have valid key & val separated by DELIMITER */
                if (!keyVal[0].equals("") &&  !keyVal[1].equals(""))
                {
                    countersConfig.put( new Integer( keyVal[ 0 ]), new Integer(keyVal[ 1 ]) );
                    LOG.info("Added the following COUNTERS CONFIG from {} : number Of Events Allowed: {} ," +
                                     " time Frame For Events InSeconds: {} ", dataKeyInDB , keyVal[ 0 ], keyVal[ 1 ]);
                }
                else
                {
                    LOG.warn( "Didn't add the current counter config, because its format was invalid: {}", currentConfigPair );
                }
            }
            catch(Exception exception)
            {
                LOG.warn("Failed to parse the current pair of counters config: {}",currentConfigPair,exception);
            }

        }
        return countersConfig;
    }

    public LoadBalancer getLoadBalancerForBaseUri(String baseUri) {
        LoadBalancer loadBalancer = this.balancers.get(baseUri);
        if (loadBalancer == null) {
            try {
                URI uri = new URI(baseUri);
                loadBalancer = this.balancers.get(uri.getHost());
            } catch (Exception ex) {
                // do nothing
            }
        }
        return loadBalancer;
    }

    private ArrayList<Server> extractServerListFrom(String serverListStr) {
        ArrayList<Server> tempServers = new ArrayList<Server>();

        for (String server : serverListStr.split(",")) {
            String hostPort[] = server.split(":");
            tempServers.add(new ServerImpl(hostPort[0], Integer.valueOf(hostPort[1])));
        }
        return tempServers;
    }

    public String reportStatus() {
        HashMap<String, LoadBalancer> statusBalancers = this.balancers;
        StringBuilder sb = new StringBuilder();
        for (LoadBalancer loadBalancer : statusBalancers.values())
        {
            sb.append(loadBalancer.reportStatus());
        }
        return sb.toString();
    }

    public MonitorResult getMonitorStatus() {
        HashMap<String, LoadBalancer> statusBalancers = this.balancers;
        MonitorResultRange monitorResult = new MonitorResultRange("LoadBalancer.Servers", 0,0);
        for (LoadBalancer loadBalancer : statusBalancers.values())
        {
            MonitorResult groupStatus = loadBalancer.getSystemMonitor();
            if (groupStatus instanceof MonitorResultRange)
            {
                MonitorResultRange range = (MonitorResultRange)groupStatus;
                monitorResult.addRange(range.getRange());
                monitorResult.addValue(range.getValue());
            }

        }
        return monitorResult;
    }
}
