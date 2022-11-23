package com.aem.operations.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import com.aem.operations.core.models.PackageHandlerModel;
import com.aem.operations.core.services.PackageHandlerService;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.post.JSONResponse;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.rest.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

@Component(
        service = { Servlet.class },
        property = {
                "sling.servlet.paths=" + PackageHandlerServlet.RESOURCE_PATH,
                "sling.servlet.methods=POST"
        }
)
public class PackageHandlerServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(PackageHandlerServlet.class);
    public static final String RESOURCE_PATH = "/bin/triggerPackageHandler";
    private static final String MESSAGE = "message";

    @Reference
    private PackageHandlerService packageHandlerService;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        // Set response headers.
        response.setContentType(JSONResponse.RESPONSE_CONTENT_TYPE);
        response.setCharacterEncoding(Constants.DEFAULT_CHARSET);

        JsonObject jsonResponse = new JsonObject();
        ResourceResolver resourceResolver = request.getResourceResolver();

        String packageName = request.getParameter("packageName");
        String packageGroup = request.getParameter("packageGroup");
        String packageVersion = request.getParameter("packageVersion");
        String packageOperation = request.getParameter("packageOperation");
        @Nullable RequestParameter inputPackStream = request.getRequestParameter("file");

        String packagePath = StringUtils.EMPTY;
        if (StringUtils.equalsIgnoreCase(PackageHandlerModel.Mode.UPLOAD.toString(), packageOperation)) {
            packagePath = packageHandlerService.uploadPack(resourceResolver, inputPackStream);
        } else  if(StringUtils.equalsIgnoreCase(PackageHandlerModel.Mode.BUILD.toString(), packageOperation)) {
            packagePath = packageHandlerService.buildPackage(resourceResolver, packageGroup, packageName, packageVersion);
        } else if(StringUtils.equalsIgnoreCase(PackageHandlerModel.Mode.UPLOAD_INSTALL.toString(), packageOperation)) {
            packagePath = packageHandlerService.uploadAndInstallPack(resourceResolver, inputPackStream);
        } else if(StringUtils.equalsIgnoreCase(PackageHandlerModel.Mode.INSTALL.toString(), packageOperation)) {
            packagePath = packageHandlerService.installPackage(resourceResolver, packageGroup, packageName, packageVersion, ImportMode.REPLACE, AccessControlHandling.IGNORE);
        } else if(StringUtils.equalsIgnoreCase(PackageHandlerModel.Mode.DELETE.toString(), packageOperation)) {
            packagePath = packageHandlerService.deletePackage(resourceResolver, packageGroup, packageName, packageVersion);
        }
        if(StringUtils.isNotEmpty(packagePath)) {
            jsonResponse.addProperty(MESSAGE, "Package Operation is Successful and path is <a href=\"" + packagePath + "\">" + packagePath + "</a>");
        } else {
            LOGGER.error("Unable to process package operation");
        }
        try (PrintWriter out = response.getWriter()) {
            out.print(new Gson().toJson(jsonResponse));
        }
    }
}