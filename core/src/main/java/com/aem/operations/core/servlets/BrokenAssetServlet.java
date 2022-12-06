package com.aem.operations.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import com.aem.operations.core.services.AssetReferenceService;
import com.aem.operations.core.services.PrepareDamReportService;
import com.aem.operations.core.services.ThrottledTaskRunnerStats;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.servlets.post.JSONResponse;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.adobe.acs.commons.fam.ThrottledTaskRunner;
import com.adobe.granite.rest.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@Component(service = { Servlet.class }, property = { "sling.servlet.paths=" + BrokenAssetServlet.RESOURCE_PATH,
        "sling.servlet.methods=POST" })
public class BrokenAssetServlet extends SlingAllMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrokenAssetServlet.class);

    private static final long serialVersionUID = 1L;
    public static final String RESOURCE_PATH = "/bin/triggerbrokenasset";
    private static final String MESSAGE = "message";

    @Reference
    PrepareDamReportService prepareDamReportService;

    @Reference
    AssetReferenceService assetReferenceService;

    @Reference
    private Scheduler scheduler;

    @Reference
    private ThrottledTaskRunner throttledTaskRunner;

    @Reference
    private transient ThrottledTaskRunnerStats ttrs;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        // Set response headers.
        response.setContentType(JSONResponse.RESPONSE_CONTENT_TYPE);
        response.setCharacterEncoding(Constants.DEFAULT_CHARSET);

        JsonObject jsonResponse = new JsonObject();

        String selectReport = request.getParameter("selectReport");
        String schedulerExpression = request.getParameter("schedulerExpression");

        if(StringUtils.isNotEmpty(selectReport) && StringUtils.isNotEmpty(schedulerExpression)) {
            @NotNull ResourceResolver resourceResolver = request.getResourceResolver();
            String assetReferenceReportPath = prepareDamReportService.prepare(resourceResolver, selectReport, schedulerExpression);
            ScheduleOptions sOpts = scheduler.EXPR(schedulerExpression.trim());
            sOpts.name(getClass().getSimpleName());
            sOpts.canRunConcurrently(false);
            scheduler.schedule(new Runnable() {
                public void run() {
                    LOGGER.info("BrokenAssetServlet Job started");
                    try {
                        throttledTaskRunner.waitForLowCpuAndLowMemory();
                    } catch (InterruptedException e) {
                        LOGGER.error("Error occured whilechecing taskRunned {}", e.getMessage());
                    }
                    assetReferenceService.checkReferences(assetReferenceReportPath);
                    LOGGER.info("BrokenAssetServlet Job completed");
                }
            }, sOpts);
            jsonResponse.addProperty(MESSAGE, "Scheduled Asset Reference Search Job");
        } else {
            jsonResponse.addProperty(MESSAGE, "Please select report and provide scheduler expresson");
        }
        try (PrintWriter out = response.getWriter()) {
            out.print(new Gson().toJson(jsonResponse));
        }
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        @NotNull ResourceResolver resourceResolver = request.getResourceResolver();
        JsonArray jsonResponse = prepareDamReportService.getStatus(resourceResolver, ttrs);
        try (PrintWriter out = response.getWriter()) {
            out.print(new Gson().toJson(jsonResponse));
        }
    }

    @Override
    protected void doDelete(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        final JSONObject jsonObject = new JSONObject();
        String jobPath = request.getParameter("jobPath");
        try {
            scheduler.unschedule(getClass().getSimpleName());
            if(StringUtils.isNotEmpty(jobPath)) {
                @NotNull ResourceResolver resourceResolver = request.getResourceResolver();
                @NotNull Resource jobRes = resourceResolver.resolve(jobPath);
                if(!ResourceUtil.isNonExistingResource(jobRes)) {
                    ModifiableValueMap map = jobRes.adaptTo(ModifiableValueMap.class);
                    map.put("jobStatus", "unscheduled");
                    resourceResolver.commit();
                }
            }
            jsonObject.put(MESSAGE, "Successfully Unscheduled Asset Reference Search Job");
        } catch (Exception exception) {
            LOGGER.error("Exception",exception);
            try {
                jsonObject.put(MESSAGE, "Unable to unschedule Asset Reference Search Job");
            } catch(JSONException exception2) {
                LOGGER.error("Json exception",exception2);
            }
        }
        response.getWriter().print(jsonObject.toString());
    }
}
