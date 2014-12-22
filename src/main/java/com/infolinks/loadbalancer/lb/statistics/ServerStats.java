package com.infolinks.loadbalancer.lb.statistics;

import com.infolinks.loadbalancer.api.Server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by roeys
 */
public class ServerStats {

    private final Server server;

    AtomicLong totalRequests = new AtomicLong();
    AtomicLong failedRequests = new AtomicLong();

    List<RecentEventsCounter> failuersCounters = new ArrayList<RecentEventsCounter>();

    List<RecentEventsCounter> consecutiveSuccessCounters = new ArrayList<RecentEventsCounter>();

    AtomicInteger activeRequestsCount = new AtomicInteger(0);

    private long lastActiveRequestsCountChangeTimestamp;
    private long lastAccessedTimestamp;

    private long firstConnectionTimestamp = 0;

    private final SimpleDataCollector responseTimeCollector = new SimpleDataCollector();


    public ServerStats( Server server,
                        Map<Integer, Integer> failedRequestsCountersConfig,
                        Map<Integer, Integer> successfulRequestsCountersConfig ) {

        this.server =  server;
        createCountersByConfig(failuersCounters,failedRequestsCountersConfig, CounterType.MarkServerDown );
        createCountersByConfig(consecutiveSuccessCounters,successfulRequestsCountersConfig, CounterType.MarkServerUp );
    }


    public enum CounterType {
        MarkServerDown, MarkServerUp;
    }

    private void createCountersByConfig( List<RecentEventsCounter> recentEventsCounters,
                                         Map<Integer, Integer> recentEventsCountersConfig,
                                         CounterType counterType ) {
        for (Map.Entry<Integer, Integer> entry : recentEventsCountersConfig.entrySet()) {
            Integer numOfEventsAllowed = entry.getKey();
            Integer timeFrameForEventsInSeconds = entry.getValue();
            recentEventsCounters.add(
                    new RecentEventsCounter(numOfEventsAllowed,
                                            timeFrameForEventsInSeconds,
                                            this.server.getId(),
                                            counterType ));
        }
    }


    /** START - section to determine if mark server down **/

    public void addToFailuresCount() {
        failedRequests.incrementAndGet();
        for (RecentEventsCounter failuresCounter : failuersCounters)
        {
            failuresCounter.incEventsCount();
        }
    }

    //TODO - this is currently determined only by the addToFailuresCount, should also add usage of the Avg response time
    public boolean getShouldMarkServerAsDown() {
        boolean shouldMarkServerAsDown = false;
        for (RecentEventsCounter failuresCounter : failuersCounters)
        {
            shouldMarkServerAsDown = failuresCounter.isExceededEventsTimeFrame() || shouldMarkServerAsDown;
        }
        return shouldMarkServerAsDown;
    }

    public void clearShouldMarkServerDown()
    {
        for (RecentEventsCounter failureCounter : failuersCounters)
        {
            failureCounter.reset();
        }
    }

    /** END - section to determine if mark server down **/

    /** START - section to determine if mark server up **/

    public void updateIsAliveSuccessfulRequest()
    {
        for (RecentEventsCounter consecutiveSuccessCounter : consecutiveSuccessCounters )
        {
            consecutiveSuccessCounter.incEventsCount();
        }
    }

    public void updateIsAliveFailedRequest()
    {
        for (RecentEventsCounter successCounter : consecutiveSuccessCounters )
        {
            successCounter.reset();
        }
    }

    public boolean getShouldMarkServerAsUp() {
        boolean shouldMarkServerAsUp = false;
        for (RecentEventsCounter successCounter : consecutiveSuccessCounters )
        {
            shouldMarkServerAsUp = successCounter.isExceededEventsTimeFrame() || shouldMarkServerAsUp;
        }
        return shouldMarkServerAsUp;
    }


    public void clearShouldMarkServerUp()
    {
        for (RecentEventsCounter consecutiveSuccessCounter : consecutiveSuccessCounters )
        {
            consecutiveSuccessCounter.reset();
        }
    }

    /** END - section to determine if mark server up **/

    public void incrementActiveRequestsCount() {
        activeRequestsCount.incrementAndGet();
        long currentTime = System.currentTimeMillis();
        lastActiveRequestsCountChangeTimestamp = currentTime;
        lastAccessedTimestamp = currentTime;
        if (firstConnectionTimestamp == 0) {
            firstConnectionTimestamp = currentTime;
        }
    }

    public void decrementActiveRequestsCount() {
        if (activeRequestsCount.decrementAndGet() < 0) {
            activeRequestsCount.set(0);
        }
        lastActiveRequestsCountChangeTimestamp = System.currentTimeMillis();
    }

    public void incrementNumRequests() {
        totalRequests.incrementAndGet();
    }

    public void noteResponseTime(long responseTimeMilliseconds) {
        responseTimeCollector.noteValue(responseTimeMilliseconds);
    }

    public double getResponseTimeMax() {
        return responseTimeCollector.getMaximum();
    }

    public double getResponseTimeMin() {
        return responseTimeCollector.getMinimum();
    }

    public double getResponseTimeAvg() {
        return responseTimeCollector.getMean();
    }

    public AtomicInteger getActiveRequestsCount() {
        return activeRequestsCount;
    }

    public AtomicLong getTotalRequests() {
        return totalRequests;
    }

    public AtomicLong getFailedRequests() {
        return failedRequests;
    }
}
