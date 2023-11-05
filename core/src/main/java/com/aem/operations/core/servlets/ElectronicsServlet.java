package com.aem.operations.core.servlets;

import com.adobe.granite.rest.Constants;
import com.day.cq.wcm.api.policies.ContentPolicy;
import com.day.cq.wcm.api.policies.ContentPolicyManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.post.JSONResponse;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@Component(service = { Servlet.class }, property = {
        "sling.servlet.paths=" + ElectronicsServlet.RESOURCE_PATH, "sling.servlet.methods=GET" })
public class ElectronicsServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;

    static final String RESOURCE_PATH = "/bin/electronicsServlet";

    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response)
            throws ServletException, IOException {
        JsonArray jsonResponse = new JsonArray();
        response.setContentType(JSONResponse.RESPONSE_CONTENT_TYPE);
        response.setCharacterEncoding(Constants.DEFAULT_CHARSET);
        String componentPath = request.getParameter("componentPath");
        String dropdownValue = request.getParameter("dropdownValue");
        if(StringUtils.isNotEmpty(componentPath)){
            ContentPolicy policy = getContentPolicy(componentPath, request.getResourceResolver());
            if(null != policy){
                jsonResponse = populateDropdown(policy, request.getResourceResolver(), dropdownValue);
            }
        }
        try (PrintWriter out = response.getWriter()) {
            out.print(new Gson().toJson(jsonResponse));
        }
    }

    private ContentPolicy getContentPolicy(@NotNull String path, @NotNull ResourceResolver resolver) {
        ContentPolicy policy = null;
        ContentPolicyManager policyMgr = resolver.adaptTo(ContentPolicyManager.class);
        Resource contentResource = resolver.getResource(path);
        if (contentResource != null && policyMgr != null) {
            policy = policyMgr.getPolicy(contentResource);
        }
        return policy;
    }

    private JsonArray populateDropdown(ContentPolicy policy, ResourceResolver resolver, String dropdownValue) {
        JsonArray jsonResponse = new JsonArray();
        Resource policyRes = resolver.resolve(policy.getPath());
        Iterator<Resource> children = policyRes.listChildren();
        while (children.hasNext()) {
            final Resource child = children.next();
            if (StringUtils.equalsIgnoreCase(child.getName(), "multifield")) {
                Iterator < Resource > multiChild = child.listChildren();
                while (multiChild.hasNext()) {
                    ValueMap valueMap = multiChild.next().adaptTo(ModifiableValueMap.class);
                    String[] type = valueMap.get("country",String[].class);
                    if(ArrayUtils.contains(type, dropdownValue)){
                        String[] allowedDevices = valueMap.get("devices",String[].class);
                        if (allowedDevices != null && allowedDevices.length > 0) {
                            for (String allowedDevice : allowedDevices) {
                                JsonObject jsonObj = new JsonObject();
                                jsonObj.addProperty("text", allowedDevice.toUpperCase());
                                jsonObj.addProperty("value", allowedDevice);
                                jsonResponse.add(jsonObj);
                            }
                        }
                    }
                }
            }
        }
        return jsonResponse;
    }
}
