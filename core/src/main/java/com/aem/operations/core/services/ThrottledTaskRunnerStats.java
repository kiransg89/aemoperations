package com.aem.operations.core.services;

import javax.management.InstanceNotFoundException;
import javax.management.ReflectionException;

/**
 * Private interface for exposing ThrottledTaskRunner stats
 * **/
public interface ThrottledTaskRunnerStats {
    /**
     * @return the % of CPU being utilized.
     * @throws InstanceNotFoundException
     * @throws ReflectionException
     */
    double getCpuLevel() throws InstanceNotFoundException, ReflectionException;

    /**
     * The % of memory being utilized.
     * @return
     */
    double getMemoryUsage();

    /***
     * @return the OSGi configured max allowed CPU utilization.
     */
    double getMaxCpu();

    /***
     * @return the OSGi configured max allowed Memory (heap) utilization.
     */
    double getMaxHeap();

    /**
     * @return the max number of threads ThrottledTaskRunner will use to execute the work.
     */
    int getMaxThreads();
}