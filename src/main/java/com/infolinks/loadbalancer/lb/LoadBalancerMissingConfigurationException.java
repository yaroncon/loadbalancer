package com.infolinks.loadbalancer.lb;

/**
 * Created by yaron
 */
public class LoadBalancerMissingConfigurationException extends Exception {

    @SuppressWarnings("UnusedDeclaration")
    public LoadBalancerMissingConfigurationException(String message) {
        super(message);
    }

    @SuppressWarnings("UnusedDeclaration")
    public LoadBalancerMissingConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
