package com.aem.operations.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import com.aem.operations.core.utils.ReportRetryUtils;
import com.aem.operations.core.visitors.ContentVisitor;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.post.JSONResponse;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.adobe.acs.commons.packaging.PackageHelper;
import com.adobe.granite.rest.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

@Component(
        service = { Servlet.class },
        property = {
                "sling.servlet.paths=" + PerfectPackageServlet.RESOURCE_PATH,
                "sling.servlet.methods=POST"
        }
)
public class PerfectPackageServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(PerfectPackageServlet.class);
    public static final String RESOURCE_PATH = "/bin/triggerPerfectPackage";
    private static final String MESSAGE = "message";

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

        String packageName = request.getParameter("packageName");
        String packageDescription = request.getParameter("packageGroup");
        String pathsList = request.getParameter("pathsList");

        String packagePath = StringUtils.EMPTY;
        if(StringUtils.isNotEmpty(pathsList)) {
            packagePath = collectReferences(request.getResourceResolver(), pathsList, packageName, packageDescription);
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

    private String collectReferences(ResourceResolver resourceResolver, String pagePaths, String packageName, String packageDescription) {
        Set<String> contentPaths = new HashSet<>();
        Set<String> xfPaths = new HashSet<>();
        Set<String> damPaths = new HashSet<>();
        ContentVisitor contentVisitor =  new ContentVisitor();
        Set<String> pages = new HashSet<>(Arrays.asList(StringUtils.split(pagePaths, '\n'))).stream().map(r -> r.trim() + "/jcr:content").collect(Collectors.toSet());
        try {
            iterateContentPaths(resourceResolver, contentPaths, xfPaths, damPaths, contentVisitor, pages);
            damPaths.addAll(iterateContent(resourceResolver, contentVisitor, damPaths));
            ReportRetryUtils.withRetry(2, 300, () -> xfPaths.addAll(iterateContent(resourceResolver, contentVisitor, xfPaths)));
            Set<String> allPaths = Stream.of(pages, contentPaths, xfPaths, damPaths).flatMap(Collection::stream).collect(Collectors.toSet());
            return packagePaths(resourceResolver, allPaths, packageName, packageDescription);
        } catch (Exception e) {
            LOGGER.error("Exception occurred during package creation in Retry {}", e.getMessage());
        }
        return StringUtils.EMPTY;
    }

    private Set<String> iterateContent(ResourceResolver resourceResolver, ContentVisitor contentVisitor, Set<String> pages) {
        Set<String> interimPaths = new HashSet<>();
        for(String page : pages) {
            contentVisitor.accept(resourceResolver.resolve(page));
            interimPaths.addAll(contentVisitor.getContentPaths());
            interimPaths.addAll(contentVisitor.getDamPaths());
            interimPaths.addAll(contentVisitor.getXfPaths());
        }
        return interimPaths;
    }

    private void iterateContentPaths(ResourceResolver resourceResolver, Set<String> contentPaths, Set<String> xfPaths,
                                     Set<String> damPaths, ContentVisitor contentVisitor, Set<String> pages) {
        for(String page : pages) {
            contentVisitor.accept(resourceResolver.resolve(page));
            contentPaths.addAll(contentVisitor.getContentPaths());
            xfPaths.addAll(contentVisitor.getXfPaths());
            damPaths.addAll(contentVisitor.getDamPaths());
        }
    }

    private String packagePaths(ResourceResolver resourceResolver, Set<String> allPaths, String packageName, String packageDescription)
            throws IOException, RepositoryException, PackageException {
        Map<String, String> packageDefinitionProperties = new HashMap<>();
        // ACL Handling
        packageDefinitionProperties.put(JcrPackageDefinition.PN_AC_HANDLING,
                AccessControlHandling.OVERWRITE.toString());

        // Package Description
        packageDefinitionProperties.put(JcrPackageDefinition.PN_DESCRIPTION, packageDescription);
        Set<@NotNull Resource> packageResources = allPaths.stream().map(resourceResolver::resolve).collect(Collectors.toSet());
        try(JcrPackage jcrPackage = packageHelper.createPackage(packageResources,
                resourceResolver.adaptTo(Session.class), DEFUALT_GROUP_NAME, packageName, DEFUALT_VERSION,
                PackageHelper.ConflictResolution.Replace, packageDefinitionProperties)){
            // Add thumbnail to the package definition
            packageHelper.addThumbnail(jcrPackage, resourceResolver.getResource(QUERY_PACKAGE_THUMBNAIL_RESOURCE_PATH));
            final JcrPackageManager packageManager = packaging.getPackageManager(resourceResolver.adaptTo(Session.class));
            packageManager.assemble(jcrPackage, null);
            return jcrPackage.getNode().getPath();
        }
    }
}