package com.aem.operations.core.models;

import javax.inject.Named;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.form.GeneratedDialog;
import com.adobe.acs.commons.mcp.form.SelectComponent;

import lombok.Getter;
import lombok.Setter;

@Model(adaptables = { Resource.class,SlingHttpServletRequest.class }, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class PackageCreatorModel extends GeneratedDialog {

    public enum GetChildren {
        NO, YES
    }

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

    @Getter
    @Setter
    @Named(value = "getChildren")
    @FormField(
            name = "Get Children",
            description = "child pages will be pulled",
            required = true,
            component = SelectComponent.EnumerationSelector.class,
            options = {"horizontal", "default=NO"})
    GetChildren getChildren = GetChildren.NO;
}
