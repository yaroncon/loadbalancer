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
public class MonitorResultError implements MonitorResult {

    @XmlElement
    private String error;

    @XmlElement
    private String name;

    public MonitorResultError() {
    }

    public MonitorResultError(String name, String error) {
        this.error = error;
        this.name = name;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "error: " + this.error;
    }

}
