package com.aem.operations.core.services;

import org.apache.sling.api.resource.ResourceResolver;
import org.jetbrains.annotations.NotNull;
import com.google.gson.JsonArray;

public interface PrepareDamReportService {

    /**
     * @param resourceResolver
     * @param reportPath
     * @param scheduleDate
     * @return resourcePath
     */
    public String prepare(@NotNull ResourceResolver resourceResolver, String reportPath, String scheduleDate);

    public JsonArray getStatus(ResourceResolver resourceResolver, ThrottledTaskRunnerStats ttrs);
}