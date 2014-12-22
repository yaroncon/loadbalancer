package com.infolinks.loadbalancer.utils;

import com.infolinks.loadbalancer.api.MonitorResult;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by yaron
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class MonitorResultRange implements MonitorResult {

    @XmlElement
    private int range;

    @XmlElement
    private int value;

    @XmlElement
    private String name;

    public MonitorResultRange() {
    }

    public MonitorResultRange(String name, int range, int value) {
        this.range = range;
        this.value = value;
        this.name = name;
    }

    public int getRange() {
        return range;
    }

    public void setRange(int range) {
        this.range = range;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return value + " out of [" + range + "]";
    }

    public void addRange(int range) {
        this.range += range;
    }

    public void addValue(int value) {
        this.value += value;
    }
}
