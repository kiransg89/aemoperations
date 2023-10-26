package com.aem.operations.core.models;

import javax.inject.Named;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.form.GeneratedDialog;
import com.adobe.acs.commons.mcp.form.SelectComponent;
import com.adobe.acs.commons.mcp.form.TextfieldComponent;

import lombok.Getter;
import lombok.Setter;

@Model(adaptables = { Resource.class,SlingHttpServletRequest.class }, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ComponentandTemplateAuditorModel extends GeneratedDialog {
	
    public enum CountPrecision {
        APPROXIMATE_TO_5, PRECISE_COUNT
    }

    @Getter
    @Setter
    @Named(value = "countPrecision")
    @FormField(name = "Select Count Precision",
            component = SelectComponent.EnumerationSelector.class,
            description = "Select dropdown to get max 5 reference for optimised query results or use precise exact count",
            options = "default=APPROXIMATE_TO_5")
    CountPrecision countPrecision = CountPrecision.APPROXIMATE_TO_5;

    @Getter
    @Setter
    @Named(value = "componentsPath")
    @FormField(name = "Components Path",
            component = TextfieldComponent.class,
            hint = "(enter the /apps/project-path)",
            description = "Enter /apps/project/components folder path to fetch all the components")
    private String componentsPath;
}  