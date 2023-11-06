package com.aem.operations.core.servlets;

import com.adobe.granite.ui.components.ExpressionHelper;
import com.day.cq.wcm.api.policies.ContentPolicy;
import com.day.cq.wcm.api.policies.ContentPolicyManager;
import com.day.crx.JcrConstants;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.*;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import com.adobe.granite.ui.components.Config;
import com.adobe.granite.ui.components.Value;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.adobe.granite.ui.components.ExpressionResolver;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;

@Component(
        service = {Servlet.class},
        property = {
                "sling.servlet.resourceTypes=" + ElectronicsDSServlet.RESOURCE_TYPE,
                "sling.servlet.methods=GET",
                "sling.servlet.extensions=html"
        }
)
public class ElectronicsDSServlet extends SlingSafeMethodsServlet {
    private static final long serialVersionUID = 1L;

    static final String RESOURCE_TYPE = "aemoperations/components/electronics/alloweddevices";


    @Reference
    protected ExpressionResolver expressionResolver;

    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response)
            throws ServletException, IOException {
        SimpleDataSource allowedDevicesDataSource = new SimpleDataSource(getAllowedDevices(request).iterator());
        request.setAttribute(DataSource.class.getName(), allowedDevicesDataSource);
    }

    protected List<Resource> getAllowedDevices(@NotNull SlingHttpServletRequest request) {
        List<Resource> colors = Collections.emptyList();
        String contentPath = (String) request.getAttribute(Value.CONTENTPATH_ATTRIBUTE);

        if(StringUtils.isEmpty(contentPath)){
            contentPath = request.getParameter("componentPath");
        }

        String dropdownValue = request.getParameter("dropdownValue");
        if(StringUtils.isEmpty(dropdownValue)){
            Config config = getConfig(request);
            ValueMap map = getComponentValueMap(config, request);
            dropdownValue = getParameter(config, "dropdownValue", request);
            if (StringUtils.isEmpty(dropdownValue)) {
                dropdownValue = map != null ? map.get("country", String.class) : StringUtils.EMPTY;
            }
        }
        ResourceResolver resolver = request.getResourceResolver();
        ContentPolicy policy = null;
        if (StringUtils.isNotEmpty(contentPath)) {
            policy = getContentPolicy(contentPath, resolver);
        }
        if (StringUtils.isEmpty(contentPath) || policy == null) {
            contentPath = request.getRequestPathInfo().getSuffix();
            if (StringUtils.isNotEmpty(contentPath)) {
                policy = getContentPolicy(contentPath, resolver);
            }
        }
        if (policy != null) {
            colors = populateDropdown(policy, resolver, dropdownValue);
        }
        return colors;
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

    private List<Resource> populateDropdown(@NotNull ContentPolicy policy, @NotNull ResourceResolver resolver, String dropdownValue) {
        List<Resource> devices = new ArrayList<>();
        ValueMap device = null;
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
                                device = new ValueMapDecorator(new HashMap<>());
                                device.put("text", allowedDevice.toUpperCase());
                                device.put("value", allowedDevice);
                                devices.add(new ValueMapResource(resolver, new ResourceMetadata(), JcrConstants.NT_UNSTRUCTURED,
                                        device));
                            }
                        }
                    }
                }
            }
        }
        return devices;
    }

    Config getConfig(SlingHttpServletRequest request) {
        // get datasource configuration
        Resource datasource = request.getResource().getChild(Config.DATASOURCE);
        if (datasource == null) {
            return null;
        }
        return new Config(datasource);
    }

    protected String getParameter(@NotNull Config config, @NotNull String name,
                                  @NotNull SlingHttpServletRequest request) {
        String value = config.get(name, String.class);
        if (value == null) {
            return null;
        }
        ExpressionHelper expressionHelper = new ExpressionHelper(expressionResolver, request);
        return expressionHelper.getString(value);
    }

    ValueMap getComponentValueMap(Config config, SlingHttpServletRequest request) {
        if (config == null) {
            return null;
        }
        String componentPath = getParameter(config, "componentPath", request);
        if (componentPath == null) {
            return null;
        }

        // get component resource
        Resource component = request.getResourceResolver().getResource(componentPath);
        if (component == null) {
            return null;
        }
        return component.getValueMap();
    }
}

