package com.aem.operations.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import com.aem.operations.core.models.ListTreeActivationModel;
import com.aem.operations.core.utils.RetryUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.post.JSONResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.adobe.acs.commons.data.CompositeVariant;
import com.adobe.acs.commons.data.Spreadsheet;
import com.adobe.granite.rest.Constants;
import com.day.cq.replication.AgentFilter;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

@Component(service = { Servlet.class }, property = { "sling.servlet.paths=" + ListActiavationServlet.RESOURCE_PATH,
        "sling.servlet.methods=POST" })
public class ListActiavationServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ListActiavationServlet.class);
    public static final String RESOURCE_PATH = "/bin/triggerListActivation";
    private static final String MESSAGE = "message";
    private static final String DESTINATION_PATH = "destination";
    private static int ASYNC_LIMIT = 10000;


    private List<String> agentList = new ArrayList<>();
    private Spreadsheet spreadsheet;
    AtomicInteger replicationCount = new AtomicInteger();

    @Reference
    Replicator replicator;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        // Set response headers.
        response.setContentType(JSONResponse.RESPONSE_CONTENT_TYPE);
        response.setCharacterEncoding(Constants.DEFAULT_CHARSET);

        JsonObject jsonResponse = new JsonObject();

        String queueMethod = request.getParameter("queueMethod");
        String agents = request.getParameter("agents");
        String reAction = request.getParameter("reAction");
        String pathsList = request.getParameter("pathsList");

        @Nullable
        RequestParameter inputPackStream = request.getRequestParameter("file");
        if(inputPackStream != null) {
            spreadsheet = new Spreadsheet(inputPackStream, DESTINATION_PATH).buildSpreadsheet();
        }
        if (null != spreadsheet) {
            AgentFilter replicationAgentFilter = prepareAgents(agents);
            if (StringUtils.equalsIgnoreCase(ListTreeActivationModel.ReplicationAction.PUBLISH.toString(), reAction)) {
                activateTreeStructure(request.getResourceResolver(), queueMethod, reAction, replicationAgentFilter);
                jsonResponse.addProperty(MESSAGE, "Succcessfully Activated all the paths provided in Excel sheet");
            } else if (StringUtils.equalsIgnoreCase(ListTreeActivationModel.ReplicationAction.UNPUBLISH.toString(), reAction)) {
                deactivateTreeStructure(request.getResourceResolver(), reAction, replicationAgentFilter);
                jsonResponse.addProperty(MESSAGE, "Succcessfully Deactivated all the paths provided in Excel sheet");
            } else {
                deleteTree(request.getResourceResolver());
                jsonResponse.addProperty(MESSAGE, "Succcessfully Deleted all the paths provided in Excel sheet");
            }
        } else if(StringUtils.isNotEmpty(pathsList)){
            AgentFilter replicationAgentFilter = prepareAgents(agents);
            Set<String> pages = new HashSet<>(Arrays.asList(StringUtils.split(pathsList, '\n'))).stream().map(String::trim).collect(Collectors.toSet());
            if (StringUtils.equalsIgnoreCase(ListTreeActivationModel.ReplicationAction.PUBLISH.toString(), reAction)) {
                activateTreeStructure(request.getResourceResolver(), pages, queueMethod, reAction, replicationAgentFilter);
                jsonResponse.addProperty(MESSAGE, "Succcessfully Activated all the paths provided in Excel sheet");
            } else if (StringUtils.equalsIgnoreCase(ListTreeActivationModel.ReplicationAction.UNPUBLISH.toString(), reAction)) {
                deactivateTreeStructure(request.getResourceResolver(), pages, reAction, replicationAgentFilter);
                jsonResponse.addProperty(MESSAGE, "Succcessfully Deactivated all the paths provided in Line Feed");
            } else {
                deleteTree(request.getResourceResolver(), pages);
                jsonResponse.addProperty(MESSAGE, "Succcessfully Deleted all the paths provided in Line Feed");
            }
        } else {
            jsonResponse.addProperty(MESSAGE, "Unable to process Activation request because of invalid Inputs");
        }
        try (PrintWriter out = response.getWriter()) {
            out.print(new Gson().toJson(jsonResponse));
        }
    }

    private AgentFilter prepareAgents(String agents) {
        AgentFilter replicationAgentFilter;
        if (StringUtils.isEmpty(agents)) {
            replicationAgentFilter = AgentFilter.DEFAULT;
        } else {
            agentList = Arrays.asList(agents.toLowerCase(Locale.ENGLISH).split(","));
            replicationAgentFilter = agent -> agentList.stream()
                    .anyMatch(p -> p.matches(agent.getId().toLowerCase(Locale.ENGLISH)));
        }
        return replicationAgentFilter;
    }

    private void activateTreeStructure(@NotNull ResourceResolver resourceResolver, String queueMethod, String reAction, AgentFilter replicationAgentFilter) {
        spreadsheet.getDataRowsAsCompositeVariants()
                .forEach(row -> performReplication(resourceResolver, getString(row, DESTINATION_PATH), queueMethod, reAction, replicationAgentFilter));
    }

    private void deactivateTreeStructure(@NotNull ResourceResolver resourceResolver, String reAction, AgentFilter replicationAgentFilter) {
        spreadsheet.getDataRowsAsCompositeVariants()
                .forEach(row -> performAsynchronousReplication(resourceResolver, getString(row, DESTINATION_PATH), reAction, replicationAgentFilter));
    }

    private void activateTreeStructure(@NotNull ResourceResolver resourceResolver, Set<String> pages, String queueMethod, String reAction, AgentFilter replicationAgentFilter) {
        pages.stream().forEach(path -> performReplication(resourceResolver, path, queueMethod, reAction, replicationAgentFilter));
    }

    private void deactivateTreeStructure(@NotNull ResourceResolver resourceResolver, Set<String> pages, String reAction, AgentFilter replicationAgentFilter) {
        pages.stream().forEach(path -> performAsynchronousReplication(resourceResolver, path, reAction, replicationAgentFilter));
    }

    private void performReplication(@NotNull ResourceResolver resourceResolver, String path, String queueMethod, String reAction, AgentFilter replicationAgentFilter) {
        int counter = replicationCount.incrementAndGet();
        if (StringUtils.equalsIgnoreCase(ListTreeActivationModel.QueueMethod.USE_MCP_QUEUE.toString(), queueMethod)
                || (StringUtils.equalsIgnoreCase(ListTreeActivationModel.QueueMethod.MCP_AFTER_10K.toString(),
                queueMethod) && counter >= ASYNC_LIMIT)) {
            performSynchronousReplication(resourceResolver, path, reAction, replicationAgentFilter);
        } else {
            performAsynchronousReplication(resourceResolver, path, reAction, replicationAgentFilter);
        }
    }

    private void performSynchronousReplication(@NotNull ResourceResolver resourceResolver, String path, String reAction, AgentFilter replicationAgentFilter) {
        ReplicationOptions options = buildOptions(replicationAgentFilter);
        options.setSynchronous(true);
        scheduleReplication(resourceResolver, options, path, reAction);
    }

    private void performAsynchronousReplication(@NotNull ResourceResolver resourceResolver, String path, String reAction, AgentFilter replicationAgentFilter) {
        ReplicationOptions options = buildOptions(replicationAgentFilter);
        options.setSynchronous(false);
        scheduleReplication(resourceResolver, options, path, reAction);
    }

    private void scheduleReplication(@NotNull ResourceResolver resourceResolver, ReplicationOptions options,
                                     String path, String reAction) {
        Session session = resourceResolver.adaptTo(Session.class);
        boolean isPublish = StringUtils
                .equalsIgnoreCase(ListTreeActivationModel.ReplicationAction.PUBLISH.toString(), reAction);
        try {
            replicator.replicate(session,
                    isPublish ? ReplicationActionType.ACTIVATE : ReplicationActionType.DEACTIVATE, path, options);
        } catch (ReplicationException e) {
            LOGGER.error("Error occured during replication {}", e.getMessage());
        }
    }

    @SuppressWarnings("rawtypes")
    private void deleteTree(ResourceResolver resourceResolver) {
        try {
            RetryUtils.withRetry(30, 500, () -> {
                Iterator<Map<String, CompositeVariant>> spredIterator = spreadsheet.getDataRowsAsCompositeVariants()
                        .iterator();
                while (spredIterator.hasNext()) {
                    Map<String, CompositeVariant> row = spredIterator.next();
                    deleteContent(resourceResolver, getString(row, DESTINATION_PATH));
                }
                commitChanges(resourceResolver);
            });
        } catch (Exception e) {
            LOGGER.error("Error occured during Retrying {}", e.getMessage());
        }
    }

    private void deleteTree(ResourceResolver resourceResolver, Set<String> pages) {
        try {
            RetryUtils.withRetry(30, 500, () -> {
                pages.forEach(path -> deleteContent(resourceResolver, path));
                commitChanges(resourceResolver);
            });
        } catch (Exception e) {
            LOGGER.error("Error occured during Retrying {}", e.getMessage());
        }
    }

    private void deleteContent(ResourceResolver resourceResolver, String destinationPath) {
        try {
            @Nullable
            Resource destinationResource = resourceResolver.getResource(destinationPath);
            if (null != destinationResource) {
                resourceResolver.delete(destinationResource);
            }
        } catch (PersistenceException e) {
            LOGGER.error("unable to delete content {}", e.getMessage());
        }
    }

    private void commitChanges(ResourceResolver resourceResolver) throws PersistenceException {
        if (resourceResolver.hasChanges()) {
            resourceResolver.commit();
            resourceResolver.refresh();
        }
    }

    private ReplicationOptions buildOptions(AgentFilter replicationAgentFilter) {
        ReplicationOptions options = new ReplicationOptions();
        options.setFilter(replicationAgentFilter);
        return options;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private String getString(Map<String, CompositeVariant> row, String path) {
        CompositeVariant v = row.get(path.toLowerCase(Locale.ENGLISH));
        if (v != null) {
            return (String) v.getValueAs(String.class);
        } else {
            return null;
        }
    }
}
