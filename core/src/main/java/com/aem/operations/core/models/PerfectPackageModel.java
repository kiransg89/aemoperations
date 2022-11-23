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

@Model(adaptables = { Resource.class,SlingHttpServletRequest.class }, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class PerfectPackageModel extends GeneratedDialog {

    @Getter
    @Setter
    @Named(value = "packageName")
    @FormField(
            name = "Package Name",
            category = "General",
            required = true,
            description = "Enter the package name",
            hint = "Name")
    private String packageName;

    @Getter
    @Setter
    @Named(value = "packageDescription")
    @FormField(
            name = "Package Description",
            category = "General",
            required = true,
            description = "Enter the package Description",
            hint = "Description")
    private String packageDescription;
}
