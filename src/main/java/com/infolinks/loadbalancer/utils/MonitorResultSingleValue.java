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
public class MonitorResultSingleValue implements MonitorResult {

    @XmlElement
    private int value;

    @XmlElement
    private String name;

    public MonitorResultSingleValue() {
    }

    public MonitorResultSingleValue(String name, int value) {
        this.value = value;
        this.name = name;
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
        return "value: " + this.value;
    }
}
