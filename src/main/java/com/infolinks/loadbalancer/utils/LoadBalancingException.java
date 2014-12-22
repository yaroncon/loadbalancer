package com.infolinks.loadbalancer.utils;

import java.io.IOException;

/**
 * Created by: yaron
 */
public class LoadBalancingException extends IOException {

    @SuppressWarnings("UnusedDeclaration")
    public LoadBalancingException(String message) {
        super(message);
    }

    @SuppressWarnings("UnusedDeclaration")
    public LoadBalancingException(String message, Throwable cause) {
        super(message, cause);
    }

}

