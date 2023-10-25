package com.aem.operations.core.models;

import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.form.TextfieldComponent;
import lombok.Getter;
import lombok.Setter;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import com.adobe.acs.commons.mcp.form.GeneratedDialog;

import javax.inject.Named;

@Model(adaptables = { Resource.class,SlingHttpServletRequest.class }, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ComponentandTemplateAuditorModel extends GeneratedDialog {

    @Getter
    @Setter
    @Named(value = "componentsPath")
    @FormField(name = "Components Path",
            component = TextfieldComponent.class,
            hint = "(enter the /apps/project-path)",
            description = "Enter /apps/project/components folder path to fetch all the components")
    private String componentsPath;
}  