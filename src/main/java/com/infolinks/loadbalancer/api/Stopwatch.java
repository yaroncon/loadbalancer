package com.infolinks.loadbalancer.api;

import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA. User: roeys Date: 1/23/14 Time: 5:48 PM To change this template use File | Settings |
 * File Templates.
 */
public interface Stopwatch {

    /** Mark the start time. */
    void start();

    /** Mark the end time. */
    void stop();

    /** Reset the stopwatch so that it can be used again. */
    void reset();

    /** Returns the duration in the specified time unit. */
    long getDuration(TimeUnit timeUnit);

    /** Returns the duration in millisecond.
     * */
    long getDuration();

    /** Returns whether or the stop watch was started - meaning start was called */
    boolean wasStrated();

    /** Returns whether or the stop watch was fully used - meaning start was called and then stop,
     *  without calling reset afterwards
     * */
    boolean wasStartedAndStopped();
}
