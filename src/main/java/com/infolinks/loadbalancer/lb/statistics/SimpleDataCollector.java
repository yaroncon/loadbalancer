package com.infolinks.loadbalancer.lb.statistics;

import com.infolinks.loadbalancer.api.DataCollector;

/**
 * Created by roeys
 */
public class SimpleDataCollector implements DataCollector
{
    private long numValues;
    private double sumValues;
    private double sumSquareValues;
    private double minValue;
    private double maxValue;


      public SimpleDataCollector()
      {
          numValues = 0L;
          sumValues = 0.0;
          sumSquareValues = 0.0;
          minValue = 0.0;
          maxValue = 0.0;
      }

    public void noteValue(double val) {
        numValues++;
        //TODO I don't want to eanble this on production to not pass the double limit!!!
        //sumValues += val;
        //sumSquareValues += val * val;
        if (numValues == 1) {
            minValue = val;
            maxValue = val;
        } else if (val < minValue) {
            minValue = val;
        } else if (val > maxValue) {
            maxValue = val;
        }
    }

    public void clear() {
        numValues = 0L;
        sumValues = 0.0;
        sumSquareValues = 0.0;
        minValue = 0.0;
        maxValue = 0.0;
    }



    public long getNumValues() {
        return numValues;
    }


    public double getMean() {
        if (numValues < 1) {
            return 0.0;
        } else {
            return sumValues / numValues;
        }
    }


    public double getVariance() {
        if (numValues < 2) {
            return 0.0;
        } else if (sumValues == 0.0) {
            return 0.0;
        } else {
            double mean = getMean();
            return (sumSquareValues / numValues) - mean * mean;
        }
    }


    public double getStdDev() {
        return Math.sqrt(getVariance());
    }


    public double getMinimum() {
        return minValue;
    }


    public double getMaximum() {
        return maxValue;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{Distribution:")
                .append("N=").append(getNumValues())
                .append(": ").append(getMinimum())
                .append("..").append(getMean())
                .append("..").append(getMaximum())
                .append("}")
                .toString();
    }

}
