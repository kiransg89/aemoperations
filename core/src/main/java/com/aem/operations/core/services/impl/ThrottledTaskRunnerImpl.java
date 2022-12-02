package com.aem.operations.core.services.impl;

import java.lang.management.ManagementFactory;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;

import com.aem.operations.core.services.ThrottledTaskRunnerStats;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = ThrottledTaskRunnerStats.class, immediate = true, name = "Throttled Task Runner Service Stats")
public class ThrottledTaskRunnerImpl implements ThrottledTaskRunnerStats {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThrottledTaskRunnerImpl.class);
    private final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    private ObjectName osBeanName;
    private ObjectName memBeanName;

    @Activate
    @Modified
    protected void activate() {
        try {
            memBeanName = ObjectName.getInstance("java.lang:type=Memory");
            osBeanName = ObjectName.getInstance("java.lang:type=OperatingSystem");
        } catch (MalformedObjectNameException | NullPointerException ex) {
            LOGGER.error("Error getting OS MBean (shouldn't ever happen) {}", ex.getMessage());
        }
    }

    @Override
    public double getCpuLevel() throws InstanceNotFoundException, ReflectionException {
        // This method will block until CPU usage is low enough
        AttributeList list = mbs.getAttributes(osBeanName, new String[]{"ProcessCpuLoad"});

        if (list.isEmpty()) {
            LOGGER.error("No CPU stats found for ProcessCpuLoad");
            return -1;
        }

        Attribute att = (Attribute) list.get(0);
        return (Double) att.getValue();
    }

    @Override
    public double getMemoryUsage() {
        try {
            Object memoryusage = mbs.getAttribute(memBeanName, "HeapMemoryUsage");
            CompositeData cd = (CompositeData) memoryusage;
            long max = (Long) cd.get("max");
            long used = (Long) cd.get("used");
            return (double) used / (double) max;
        } catch (AttributeNotFoundException | InstanceNotFoundException | MBeanException | ReflectionException e) {
            LOGGER.error("No Memory stats found for HeapMemoryUsage", e);
            return -1;
        }
    }

    @Override
    public double getMaxCpu() {
        return 0.75;
    }

    @Override
    public double getMaxHeap() {
        return 0.85;
    }

    @Override
    public int getMaxThreads() {
        return Math.max(1, Runtime.getRuntime().availableProcessors()/2);
    }

}
