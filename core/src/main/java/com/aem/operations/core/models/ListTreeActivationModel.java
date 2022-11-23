package com.aem.operations.core.models;

import com.adobe.acs.commons.mcp.form.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

import javax.inject.Named;
import java.io.InputStream;

@Model(adaptables = { Resource.class,SlingHttpServletRequest.class }, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ListTreeActivationModel extends GeneratedDialog {

    public enum QueueMethod {
        USE_MCP_QUEUE, USE_PUBLISH_QUEUE, MCP_AFTER_10K
    }

    public enum ReplicationAction {
        PUBLISH, UNPUBLISH, DELETE
    }

    @Getter
    @Setter
    @Named(value = "repPathExcel")
    @FormField(name = "Replication Excel", description = "Upload Replication Excel", component = FileUploadComponent.class)
    private InputStream repPathExcel = null;

    @Getter
    @Setter
    @Named(value = "queueMethod")
    @FormField(
            name = "Queueing Method",
            description = "For small publishing tasks, standard is sufficient.  For large folder trees, MCP is recommended.",
            required = true,
            component = SelectComponent.EnumerationSelector.class,
            options = {"horizontal", "default=USE_MCP_QUEUE"})
    QueueMethod queueMethod = QueueMethod.USE_MCP_QUEUE;

    @Getter
    @Setter
    @Named(value = "agents")
    @FormField(name = "Agents",
            component = TextfieldComponent.class,
            hint = "(leave blank for default agents)",
            description = "Publish agents to use, if blank then all default agents will be used. Multiple agents can be listed using commas or regex.")
    private String agents;

    @Getter
    @Setter
    @Named(value = "reAction")
    @FormField(name = "Action",
            component = SelectComponent.EnumerationSelector.class,
            description = "Publish or Unpublish?",
            options = "default=PUBLISH")
    ReplicationAction reAction = ReplicationAction.PUBLISH;
}