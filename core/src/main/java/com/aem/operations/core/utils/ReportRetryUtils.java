package com.aem.operations.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportRetryUtils {
    private static final Logger log = LoggerFactory.getLogger(ReportRetryUtils.class);

    public static interface CallToRetry {
        void process() throws Exception;
    }

    public static boolean withRetry(int maxTimes, long intervalWait, CallToRetry call) throws Exception {
        if (maxTimes <= 0) {
            throw new IllegalArgumentException("Must run at least one time");
        }
        if (intervalWait <= 0) {
            throw new IllegalArgumentException("Initial wait must be at least 1");
        }
        Exception thrown = null;
        for (int i = 0; i < maxTimes; i++) {
            try {
                call.process();
                if(i == (maxTimes-1)) {
                    return true;
                }
            } catch (Exception e) {
                thrown = e;
                log.info("Encountered failure on {} due to {}, attempt retry {} of {}", call.getClass().getName() , e.getMessage(), (i + 1), maxTimes, e);
            }
            try {
                Thread.sleep(intervalWait);
            } catch (InterruptedException wakeAndAbort) {
                break;
            }
        }
        throw thrown;
    }
}