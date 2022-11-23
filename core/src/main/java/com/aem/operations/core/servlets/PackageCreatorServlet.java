package com.aem.operations.core.servlets;

import com.adobe.acs.commons.packaging.PackageHelper;
import com.adobe.granite.rest.Constants;
import com.aem.operations.core.models.PackageCreatorModel;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.packaging.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.post.JSONResponse;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

@Component(
        service = { Servlet.class },
        property = {
                "sling.servlet.paths=" + PackageCreatorServlet.RESOURCE_PATH,
                "sling.servlet.methods=POST"
        }
)
public class PackageCreatorServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(PackageCreatorServlet.class);
    public static final String RESOURCE_PATH = "/bin/triggerPackageCreator";
    private static final String MESSAGE = "message";
    private static final String CONTENT_DAM_SLASH = "/content/dam/";

    private static final String DEFUALT_GROUP_NAME = "my_packages";
    private static final String DEFUALT_VERSION = "1.0";
    private static final String QUERY_PACKAGE_THUMBNAIL_RESOURCE_PATH = "/apps/acs-commons/components/utilities/packager/query-packager/definition/package-thumbnail.png";

    @Reference
    private PackageHelper packageHelper;

    @Reference
    private Packaging packaging;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        // Set response headers.
        response.setContentType(JSONResponse.RESPONSE_CONTENT_TYPE);
        response.setCharacterEncoding(Constants.DEFAULT_CHARSET);

        JsonObject jsonResponse = new JsonObject();
        ResourceResolver resourceResolver = request.getResourceResolver();

        String packageName = request.getParameter("packageName");
        String packageDescription = request.getParameter("packageGroup");
        String pathsList = request.getParameter("pathsList");
        String getChildren = request.getParameter("getChildren");

        String packagePath = StringUtils.EMPTY;
        if(StringUtils.isNotEmpty(pathsList)) {
            packagePath = createPackage(resourceResolver, packageName, packageDescription, pathsList, getChildren);
        }

        if(StringUtils.isNotEmpty(packagePath)) {
            jsonResponse.addProperty(MESSAGE, "Package creation is Successful and path is <a href=\"" + packagePath + "\">" + packagePath + "</a>");
        } else {
            LOGGER.error("Unable to process package creation");
        }
        try (PrintWriter out = response.getWriter()) {
            out.print(new Gson().toJson(jsonResponse));
        }
    }

    private String createPackage(ResourceResolver resourceResolver, String packageName, String packageDescription,
                                 String pathsList, String getChildren) throws IOException {
        Set<String> pages = new HashSet<>(Arrays.asList(StringUtils.split(pathsList, '\n'))).stream().map(String::trim).collect(Collectors.toSet());
        Set<Resource> packageResources = pages.stream().map(path -> getResource(resourceResolver, path, getChildren)).collect(Collectors.toSet());
        if(null != packageResources && !packageResources.isEmpty()) {
            Map<String, String> packageDefinitionProperties = new HashMap<>();
            // ACL Handling
            packageDefinitionProperties.put(JcrPackageDefinition.PN_AC_HANDLING,
                    AccessControlHandling.OVERWRITE.toString());
            // Package Description
            packageDefinitionProperties.put(JcrPackageDefinition.PN_DESCRIPTION, packageDescription);
            try(JcrPackage jcrPackage = packageHelper.createPackage(packageResources,
                    resourceResolver.adaptTo(Session.class), DEFUALT_GROUP_NAME, packageName, DEFUALT_VERSION,
                    PackageHelper.ConflictResolution.Replace, packageDefinitionProperties)){
                // Add thumbnail to the package definition
                packageHelper.addThumbnail(jcrPackage, resourceResolver.getResource(QUERY_PACKAGE_THUMBNAIL_RESOURCE_PATH));
                final JcrPackageManager packageManager = packaging.getPackageManager(resourceResolver.adaptTo(Session.class));
                packageManager.assemble(jcrPackage, null);
                return jcrPackage.getNode().getPath();
            } catch (RepositoryException | PackageException e) {
                LOGGER.error("Exception occurred during package creation {}", e.getMessage());
            }
        }
        return StringUtils.EMPTY;
    }

    private @Nullable Resource getResource(ResourceResolver resourceResolver, String path, String getChildren) {
        if(StringUtils.equalsIgnoreCase(PackageCreatorModel.GetChildren.NO.toString(), getChildren) && !StringUtils.contains(path, CONTENT_DAM_SLASH)) {
            return resourceResolver.resolve(path + "/jcr:content");
        }
        return resourceResolver.resolve(path);
    }
}