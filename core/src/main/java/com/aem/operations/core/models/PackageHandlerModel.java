package com.aem.operations.core.models;

import java.io.InputStream;

import javax.inject.Named;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

import com.adobe.acs.commons.mcp.form.FileUploadComponent;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.form.GeneratedDialog;
import com.adobe.acs.commons.mcp.form.RadioComponent;

import lombok.Getter;
import lombok.Setter;

@Model(adaptables = { Resource.class,SlingHttpServletRequest.class }, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class PackageHandlerModel extends GeneratedDialog {

    @Getter
    @Setter
    @Named(value = "inputPackage")
    @FormField(name = "Package", description = "Upload JCR Package", component = FileUploadComponent.class)
    private InputStream inputPackage = null;

    @Getter
    @Setter
    @Named(value = "packageOperation")
    @FormField(
            name = "Package Operation",
            description = "Select the operation to be performed",
            required = true,
            component = RadioComponent.EnumerationSelector.class,
            options = {"vertical", "default=UPLOAD"})
    private Mode packageOperation;

    public enum Mode {
        UPLOAD, UPLOAD_INSTALL
    }
}
