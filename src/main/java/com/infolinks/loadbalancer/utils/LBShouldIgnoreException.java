package com.infolinks.loadbalancer.utils;

import java.io.IOException;

/**
 * Created by yaron
 * This Exception marks to the load balancer to ignore this exception
 * It will not mark servers as down, as a result of this exception
 */
public class LBShouldIgnoreException extends IOException {

    @SuppressWarnings("UnusedDeclaration")
    public LBShouldIgnoreException(String message) {
        super(message);
    }

    @SuppressWarnings("UnusedDeclaration")
    public LBShouldIgnoreException(String message, Throwable cause) {
        super(message, cause);
    }

}
