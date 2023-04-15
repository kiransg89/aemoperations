package com.aem.operations.core.models;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageFilter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.PostConstruct;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Model(adaptables = SlingHttpServletRequest.class, resourceType = ListChildren.RESOURCE_TYPE, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ListChildren {
    public static final String RESOURCE_TYPE = "aemoperations/components/listchildren";

    @ScriptVariable
    protected ResourceResolver resolver;

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    @Getter
    private String linkURL;

    @OSGiService
    private QueryBuilder queryBuilder;

    @Getter
    public long streamExecution;

    @Getter
    public long streamExecutionResults;

    @Getter
    public long iteratorExecution;

    @Getter
    public long iteratorExecutionResults;

    @Getter
    public long queryExecution;

    @Getter
    public long queryExecutionResults;

    @PostConstruct
    private void initModel() {
        if (StringUtils.isNotEmpty(linkURL)) {
            @NotNull Resource resource = resolver.resolve(linkURL);
            if(null != resource){
                List<String> streamList = new ArrayList<>();
                long startOne = System.currentTimeMillis();
                for (Resource descendant : (Iterable<? extends Resource>) traverse(resource)::iterator) {
                    streamList.add(descendant.getPath());
                }
                long endOne = System.currentTimeMillis();
                streamExecution = (endOne-startOne);
                streamExecutionResults = streamList.size();

                List<String> pageList = new ArrayList<>();
                Page page = resource.adaptTo(Page.class);
                long startTow = System.currentTimeMillis();
                Iterator<Page> childIterator = page.listChildren(new PageFilter(), true);
                StreamSupport.stream(((Iterable<Page>) () -> childIterator).spliterator(), false).forEach( r -> {
                    pageList.add(r.getPath());
                    }
                );
                long endTwo = System.currentTimeMillis();
                iteratorExecution = (endTwo-startTow);
                iteratorExecutionResults = pageList.size();

                List<String> queryList = new ArrayList<>();
                Map<String, String> map = new HashMap<>();
                map.put("path", resource.getPath());
                map.put("type", "cq:PageContent");
                map.put("p.limit", "-1");

                long startThree = System.currentTimeMillis();
                Session session = resolver.adaptTo(Session.class);
                Query query = queryBuilder.createQuery(PredicateGroup.create(map), session);
                SearchResult result = query.getResult();
                ResourceResolver leakingResourceResolverReference = null;
                try {
                    for (final Hit hit : result.getHits()) {
                        if (leakingResourceResolverReference == null) {
                            leakingResourceResolverReference = hit.getResource().getResourceResolver();
                        }
                        queryList.add(hit.getPath());
                    }
                } catch (RepositoryException e) {
                    log.error("Error collecting inherited section search results", e);
                } finally {
                    if (leakingResourceResolverReference != null) {
                        leakingResourceResolverReference.close();
                    }
                }
                long endThree = System.currentTimeMillis();
                queryExecution = (endThree-startThree);
                queryExecutionResults = queryList.size();
            }
        }
    }

    private Stream<Resource> traverse(@NotNull Resource resourceRoot) {
        Stream<Resource> children = StreamSupport.stream(resourceRoot.getChildren().spliterator(), false)
                .filter(this::shouldFollow);
        return Stream.concat(
                shouldInclude(resourceRoot) ? Stream.of(resourceRoot) : Stream.empty(),
                children.flatMap(this::traverse)
        );
    }

    protected boolean shouldFollow(@NotNull Resource resource) {
        return !JcrConstants.JCR_CONTENT.equals(resource.getName());
    }

    protected boolean shouldInclude(@NotNull Resource resource) {
        return resource.getChild(JcrConstants.JCR_CONTENT) != null;
    }
}
