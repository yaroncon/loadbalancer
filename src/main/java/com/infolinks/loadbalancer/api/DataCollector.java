package com.infolinks.loadbalancer.api;

/**
 * Created with IntelliJ IDEA. User: roeys Date: 1/26/14 Time: 5:44 PM To change this template use File | Settings |
 * File Templates.
 */
public interface DataCollector {

    public void noteValue(double val);

    public void clear();

    public long getNumValues();

    public double getMean();

    public double getVariance();

    public double getMinimum();


    public double getMaximum();

    public double getStdDev();
}
