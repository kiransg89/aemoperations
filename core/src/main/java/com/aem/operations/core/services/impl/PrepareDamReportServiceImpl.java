package com.aem.operations.core.services.impl;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.management.InstanceNotFoundException;
import javax.management.ReflectionException;

import com.aem.operations.core.services.PrepareDamReportService;
import com.aem.operations.core.services.ThrottledTaskRunnerStats;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.aemds.guide.utils.JcrResourceConstants;
import com.day.crx.JcrConstants;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@Component(service = PrepareDamReportService.class, immediate = true, name = "Prepare Dam Report Service")
public class PrepareDamReportServiceImpl implements PrepareDamReportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareDamReportServiceImpl.class);
    private static final String ASSET_REFERENCE_REPORT_PATH = "/var/dam/assetreferencereports";
    private static final String CSV_FILENAME = "result.csv";
    private static final String MESSAGE_FORMAT = "{0,number,#%}";

    @Override
    public String prepare(@NotNull ResourceResolver resourceResolver, String reportPath, String scheduleDate) {
        @NotNull Resource reportResource = resourceResolver.resolve(reportPath);
        if(!ResourceUtil.isNonExistingResource(reportResource)) {
            String reportId = StringUtils.substringAfterLast(reportPath, FileSystem.SEPARATOR);
            Resource referenceRes = resourceResolver.resolve(ASSET_REFERENCE_REPORT_PATH+FileSystem.SEPARATOR+reportId);
            if(!ResourceUtil.isNonExistingResource(referenceRes)) {
                try {
                    ModifiableValueMap map = referenceRes.adaptTo(ModifiableValueMap.class);
                    map.put("jobStatus", "running");
                    map.put("scheduledExpression", scheduleDate);
                    resourceResolver.commit();
                } catch (PersistenceException e) {
                    LOGGER.error("Error occurred while setting the status {}", e.getMessage());
                }
            } else {
                referenceRes = createReportFolder(resourceResolver, reportPath, scheduleDate);
            }
            if(null != referenceRes) {
                return referenceRes.getPath();
            }
        }
        return StringUtils.EMPTY;
    }

    private Resource createReportFolder(ResourceResolver resourceResolver, String reportPath, String scheduleDate) {

        String reportId = StringUtils.substringAfterLast(reportPath, FileSystem.SEPARATOR);
        Map<String, Object> nodeProperties = new HashMap<>();
        nodeProperties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
        nodeProperties.put("reportPath", reportPath);
        nodeProperties.put("scheduledExpression", scheduleDate);
        nodeProperties.put("jobStatus", "starting");
        try {
            return ResourceUtil.getOrCreateResource(resourceResolver, ASSET_REFERENCE_REPORT_PATH+FileSystem.SEPARATOR+reportId, nodeProperties, JcrResourceConstants.NT_SLING_FOLDER, true);
        } catch (PersistenceException e) {
            LOGGER.error("Error occurred while preparing report folder {}", e.getMessage());
        }
        return null;
    }

    @Override
    public JsonArray getStatus(ResourceResolver resourceResolver, ThrottledTaskRunnerStats ttrs) {
        JsonArray jsonResponse = new JsonArray();
        @NotNull Resource assetRefRes = resourceResolver.resolve(ASSET_REFERENCE_REPORT_PATH);
        if(!ResourceUtil.isNonExistingResource(assetRefRes)) {
            Iterator<Resource> iterator =  assetRefRes.getChildren().iterator();
            while(iterator.hasNext()) {
                JsonObject childJson = new JsonObject();
                Resource child = iterator.next();
                String damReport = child.getValueMap().get("reportPath", StringUtils.EMPTY);
                @NotNull Resource damReportRes = resourceResolver.resolve(damReport);
                childJson.addProperty("reportName", damReportRes.getValueMap().get("jobTitle", StringUtils.EMPTY));
                childJson.addProperty("jobStatus", child.getValueMap().get("jobStatus", StringUtils.EMPTY));
                childJson.addProperty("current", child.getValueMap().get("count", StringUtils.EMPTY));
                childJson.addProperty("scheduledExpression", child.getValueMap().get("scheduledExpression", StringUtils.EMPTY));
                childJson.addProperty("reportPath", child.getPath());
                @Nullable Resource csvReport = child.getChild(CSV_FILENAME);
                if(null != csvReport) {
                    childJson.addProperty("csvPath", child.getChild(CSV_FILENAME).getPath());
                }
                getSystemStats(childJson, ttrs);
                jsonResponse.add(childJson);
            }
        }
        return jsonResponse;
    }

    private JsonObject getSystemStats(JsonObject json, ThrottledTaskRunnerStats ttrs) {
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
