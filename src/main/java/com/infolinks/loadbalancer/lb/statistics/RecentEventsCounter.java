package com.infolinks.loadbalancer.lb.statistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;

/**
 * Created by roeys
 */
public class RecentEventsCounter {

    private static final Logger LOG = LoggerFactory.getLogger( RecentEventsCounter.class );

    private final String serverId;

    private final ServerStats.CounterType counterType;

    private int eventsAllowed;
    private int timeFrameForEventsInSeconds;
    private final static long SECONDS_TO_MILISECONDS = 1000, MILISECONDS_TO_SECONDS = 1000;
    private Deque<Date> eventsTimeStamps = new LinkedList<Date>();

    private final static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z");

    RecentEventsCounter( int numOfEventsAllowed, int timeFrameForEventsInSeconds, String serverId, ServerStats.CounterType counterType )
    {
        this.eventsAllowed = numOfEventsAllowed;
        this.timeFrameForEventsInSeconds = timeFrameForEventsInSeconds;
        this.serverId = serverId;
        this.counterType = counterType;
    }

    /**
     * we increase the events count
     * 1. if currently we don't have more events than we allow - we add a new timestamp
     * 2. if we reached the limit of events allowed
     *  2.1 if those events happened in more than the time frame allowed
     *      2.1.1 we remove the time stamp of the event that happened the longest time ago
     *      2.1.2 we add the time stamp of the new event
     *  2.2 if those events happened in less or equal to the time frame we allow - we don't add new anymore
     */
    public synchronized void incEventsCount()
    {
        if ( eventsTimeStamps.size()< eventsAllowed)
        {
            printLog("incEventsCount");
            Date currentEventTimeStamp = new Date();
            eventsTimeStamps.add(currentEventTimeStamp);
        }
         /* there are more events than we allow - need to check if it happened in the time frame
           if it did - we don't add the new event, because we already passed the events allowed in time period
           it it didn't - we remove the head of the list and add the current event's time stamp*/
        else
        {
            Date firstEventTimeStamp = eventsTimeStamps.getFirst();
            Date lastEventTimeStamp  = eventsTimeStamps.getLast();
            long currentEventTimeFrame = lastEventTimeStamp.getTime() - firstEventTimeStamp.getTime();
            if (currentEventTimeFrame > (timeFrameForEventsInSeconds * SECONDS_TO_MILISECONDS) )
            {
                //we remove the head - if it was to long ago for the current timeStamp, it MUST be to long for even
                //more distant timeStamps
                eventsTimeStamps.poll();
                //and add the new value
                Date currentEventTimeStamp = new Date();
                eventsTimeStamps.add(currentEventTimeStamp);
                printLog(true, lastEventTimeStamp,firstEventTimeStamp, "MORE", "isExceededEventsTimeFrame");
            }
            else
            {
                //there were more events than we allow in the Time frame - we don't add anymore
                printLog(true, lastEventTimeStamp, firstEventTimeStamp, "LESS", "incEventsCount" );
            }

        }
    }

    public synchronized boolean isExceededEventsTimeFrame()
    {
        if ( eventsTimeStamps.size()< eventsAllowed)
        {
            printLog("isExceededEventsTimeFrame");
            return false;
        }
        /* there are more events than we allow - need to check if it happened in the time frame
           if they did - we return "true"
           it they didn't - we return "false"
        */
        else
        {
            Date firstEventTimeStamp = eventsTimeStamps.getFirst();
            Date lastEventTimeStamp  = eventsTimeStamps.getLast();
            long currentEventTimeFrame = lastEventTimeStamp.getTime() - firstEventTimeStamp.getTime();
            if (currentEventTimeFrame > (timeFrameForEventsInSeconds * SECONDS_TO_MILISECONDS) )
            {
                printLog(true, lastEventTimeStamp,firstEventTimeStamp, "MORE", "isExceededEventsTimeFrame");
                return false;
            }
            else
            {
                printLog(true, lastEventTimeStamp, firstEventTimeStamp, "LESS", "isExceededEventsTimeFrame" );
                return true;
            }
        }
    }

    public synchronized void reset()
    {
        eventsTimeStamps = new LinkedList<Date>();
    }

    private void printLog(boolean isCounterFull,  Date lastEventTimeStamp, Date firstEventTimeStamp, String quantifier, String functionName )
    {

        if (isCounterFull)
        {
            if (LOG.isDebugEnabled()){
                LOG.debug("In Server {} In function {} , {} Counter indicates there were {} events in {} than the time frame allowed:" +
                             " current Event Time Stamp - {} |" +
                             " first Event Time Stamp {} |" +
                             " Events Time Frame in seconds - {} |"+
                             " Allowed time frame in seconds - {}",
                     this.serverId, functionName, this.counterType, eventsTimeStamps.size(), quantifier,
                     simpleDateFormat.format(lastEventTimeStamp) ,
                     simpleDateFormat.format(firstEventTimeStamp),
                     (lastEventTimeStamp.getTime() - firstEventTimeStamp.getTime())/MILISECONDS_TO_SECONDS,
                     this.timeFrameForEventsInSeconds);
            }
        }
        else
        {
            if (LOG.isDebugEnabled()){
                LOG.debug( "In Server {} In function {} , {} Counter counted only {} events , less than the {} we allow",
                           this.serverId, functionName, this.counterType, eventsTimeStamps.size(), eventsAllowed );
            }
        }
    }

    private void printLog( String functionName )
    {
        printLog(false,null,null,null,functionName );
    }
}
