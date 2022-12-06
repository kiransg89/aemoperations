package com.aem.operations.core.models;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.adobe.acs.commons.mcp.form.SelectComponent;

/**
 * This custom select is used to populate the available collection for the current user.
 */
public class DamReportSelectComponent extends SelectComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(DamReportSelectComponent.class);
    private static final String DAM_REPORT_PATH = "/var/dam/reports";

    @Override
    public Map<String, String> getOptions() {
        Map<String, String> options = new LinkedHashMap<>();
        if (null != getHelper()
                && null != getHelper().getRequest()
                && null != getHelper().getRequest().getResourceResolver()) {

            ResourceResolver resourceResolver = getHelper().getRequest().getResourceResolver();
            @NotNull Resource reportResource = resourceResolver.resolve(DAM_REPORT_PATH);

            if(!ResourceUtil.isNonExistingResource(reportResource)) {
                @NotNull Iterator<Resource> childIterator = reportResource.getChildren().iterator();
                options.put(StringUtils.EMPTY, "Select the Asset Report");
                while(childIterator.hasNext()) {
                    Resource child = childIterator.next();
                    if(null != child) {
                        String title = child.getValueMap().get("jobTitle", StringUtils.EMPTY);
                        if(StringUtils.isNotEmpty(title)) {
                            options.put(child.getPath(), child.getValueMap().get("jobTitle", StringUtils.EMPTY));
                        }
                    }
                }
                if(options.isEmpty()) {
                    LOGGER.error("Asset Reports are missing hence returing empty map");
                    return Collections.emptyMap();
                }
            }
        } else {
            LOGGER.error("Resource resolver is null while getting the DAM Report list");
        }
        return options;
    }
}
