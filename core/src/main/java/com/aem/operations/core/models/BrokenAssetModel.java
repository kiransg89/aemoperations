package com.aem.operations.core.models;

import javax.inject.Named;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.form.GeneratedDialog;
import lombok.Getter;
import lombok.Setter;

@Model(adaptables = { Resource.class, SlingHttpServletRequest.class }, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class BrokenAssetModel extends GeneratedDialog {

    @Getter
    @Setter
    @Named(value = "selectReport")
    @FormField(name = "Select DAM Report",
            category = "General",
            required = true,
            description = "Select the DAM Report form the dropdown",
            component = DamReportSelectComponent.class)
    private String selectReport;

    @Getter
    @Setter
    @Named(value = "schedulerExpression")
    @FormField(
            name = "Scheduler Expression",
            category = "General",
            required = true,
            description = "Enter the Scheduler Expression",
            options = {"horizontal", "default=0/30 0/1 * 1/1 * ? *"},
            hint = "0/30 0/1 * 1/1 * ? *")
    private String schedulerExpression;

    @Getter
    @Setter
    @Named(value = "timeout")
    @FormField(
            name = "Refresh Interval",
            category = "General",
            required = true,
            description = "Referesh the results after interva",
            options = {"horizontal", "default=10"},
            hint = "10")
    private String timeout;
}