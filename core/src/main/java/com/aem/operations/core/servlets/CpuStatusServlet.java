package com.aem.operations.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;

import javax.management.InstanceNotFoundException;
import javax.management.ReflectionException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import com.aem.operations.core.services.ThrottledTaskRunnerStats;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

@Component(service = { Servlet.class }, property = { "sling.servlet.paths=" + CpuStatusServlet.RESOURCE_PATH,
        "sling.servlet.methods=POST" })
public class CpuStatusServlet extends SlingAllMethodsServlet{

    private static final Logger LOGGER = LoggerFactory.getLogger(CpuStatusServlet.class);

    private static final long serialVersionUID = 1L;
    public static final String RESOURCE_PATH = "/bin/cpustatust";
    private static final String MESSAGE_FORMAT = "{0,number,#%}";

    @Reference
    private transient ThrottledTaskRunnerStats ttrs;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        JsonObject jsonResponse = getSystemStats();
        try (PrintWriter out = response.getWriter()) {
            out.print(new Gson().toJson(jsonResponse));
        }
    }

    private JsonObject getSystemStats() {
        JsonObject json = new JsonObject();
        try {
            json.addProperty("cpu", MessageFormat.format(MESSAGE_FORMAT, ttrs.getCpuLevel()));
        } catch (InstanceNotFoundException | ReflectionException e) {
            LOGGER.error("Could not collect CPU stats {}", e.getMessage());
            json.addProperty("cpu", -1);
        }
        json.addProperty("mem", MessageFormat.format(MESSAGE_FORMAT, ttrs.getMemoryUsage()));
        json.addProperty("maxCpu", MessageFormat.format(MESSAGE_FORMAT, ttrs.getMaxCpu()));
        json.addProperty("maxMem", MessageFormat.format(MESSAGE_FORMAT, ttrs.getMaxHeap()));
        return json;
    }
}
