package com.aem.operations.core.visitors;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.AbstractResourceVisitor;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.jetbrains.annotations.NotNull;

public class ContentVisitor extends AbstractResourceVisitor {

    private static final String CONTENT_SLASH = "/content/";
    private static final String CONTENT_DAM_SLASH = "/content/dam/";
    private static final String CONTENT_XF_SLASH = "/content/experience-fragments/";

    Pattern htmlPattern = Pattern.compile(".*\\<[^>]+>.*", Pattern.DOTALL);

    Set<String> contentPaths = new HashSet<>();
    Set<String> xfPaths = new HashSet<>();
    Set<String> damPaths = new HashSet<>();

    @Override
    public final void accept(final Resource resource) {
        if (null != resource && !ResourceUtil.isNonExistingResource(resource)) {
            final ValueMap properties = resource.adaptTo(ValueMap.class);
            final String primaryType = properties.get(ResourceResolver.PROPERTY_RESOURCE_TYPE, StringUtils.EMPTY);
            if(StringUtils.isNoneEmpty(primaryType) || StringUtils.startsWith(resource.getPath(), "/content/dam/content-fragments")){
                visit(resource);
            }
            this.traverseChildren(resource.listChildren());
        }
    }

    @Override
    protected void traverseChildren(final @NotNull Iterator<Resource> children) {
        while (children.hasNext()) {
            final Resource child = children.next();
            accept(child);
        }
    }

    @Override
    protected void visit(final @NotNull Resource resource) {
        resource.getValueMap().entrySet().forEach(property -> {
            Object prop = property.getValue();
            if (prop.getClass() == String[].class) {
                List<String> propertyValue = Arrays.asList((String[]) prop);
                if (!propertyValue.isEmpty()) {
                    propertyValue.stream().filter(s -> StringUtils.isNotEmpty(s) && StringUtils.startsWith(s, CONTENT_SLASH) && !htmlPattern.matcher(s).matches()).forEach(this::populateValue);
                }
            } else if (prop.getClass() == String.class) {
                String propertyValue = (String) prop;
                if (StringUtils.isNotEmpty(propertyValue) && StringUtils.startsWith(propertyValue, CONTENT_SLASH) && !htmlPattern.matcher(propertyValue).matches()) {
                    populateValue(propertyValue);
                }
            }
        });
    }

    private void populateValue(String value) {
        if(StringUtils.startsWith(value, CONTENT_DAM_SLASH)) {
            damPaths.add(value);
        } else if(StringUtils.startsWith(value, CONTENT_XF_SLASH)) {
            xfPaths.add(value+"/jcr:content");
        } else {
            contentPaths.add(StringUtils.substringBeforeLast(value, ".html")+"/jcr:content");
        }
    }

    public Set<String> getContentPaths() {
        return contentPaths;
    }

    public Set<String> getXfPaths() {
        return xfPaths;
    }

    public Set<String> getDamPaths() {
        return damPaths;
    }
}
