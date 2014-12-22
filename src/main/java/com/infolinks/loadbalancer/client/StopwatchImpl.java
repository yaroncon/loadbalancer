package com.infolinks.loadbalancer.client;

import com.infolinks.loadbalancer.api.Stopwatch;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA. User: roeys Date: 1/23/14 Time: 5:50 PM To change this template use File | Settings |
 * File Templates.
 */
public class StopwatchImpl implements Stopwatch {

    private Date startTime = null;
    private Date stopTime = null;

    @Override
    public void start() {
        startTime = new Date();
    }

    @Override
    public void stop() {
        if (stopTime == null){
            stopTime = new Date();
        }
    }

    @Override
    public void reset() {
        startTime = null;
        stopTime = null;
    }

    @Override
    public long getDuration( TimeUnit timeUnit ) {
        long diffInMilliseconds = stopTime.getTime() - startTime.getTime();
        return TimeUnit.MILLISECONDS.convert(diffInMilliseconds,timeUnit);
    }

    @Override
    public long getDuration() {
        if (stopTime != null && startTime != null)
        {
            long diffInMilliseconds = stopTime.getTime() - startTime.getTime();
            return  diffInMilliseconds;
        }
        else
        {
            return -1;
        }
    }

    @Override
    public boolean wasStrated() {
        return (startTime!=null);
    }

    @Override
    public boolean wasStartedAndStopped() {
        return  (startTime!=null) && (stopTime!=null);
    }

}
