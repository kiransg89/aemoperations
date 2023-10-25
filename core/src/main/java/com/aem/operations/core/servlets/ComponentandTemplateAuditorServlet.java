package com.aem.operations.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import com.adobe.aemds.guide.utils.JcrResourceConstants;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.NameConstants;
import com.day.crx.JcrConstants;
import com.google.gson.JsonArray;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.post.JSONResponse;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import com.adobe.granite.rest.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component(service = { Servlet.class }, property = { "sling.servlet.paths=" + ComponentandTemplateAuditorServlet.RESOURCE_PATH,
		"sling.servlet.methods=POST" })
public class ComponentandTemplateAuditorServlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 1L;
	public static final String RESOURCE_PATH = "/bin/triggercomponentandtemplateauditor";

	@Override
	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {

		// Set response headers.
		response.setContentType(JSONResponse.RESPONSE_CONTENT_TYPE);
		response.setCharacterEncoding(Constants.DEFAULT_CHARSET);
		JsonArray jsonArray = new JsonArray();		

		String componentsPath = request.getParameter("componentsPath");
		String searchPaths = request.getParameter("searchPaths");

		@NotNull ResourceResolver resourceResolver = request.getResourceResolver();
		Set<String> componentList = getComponentsList(resourceResolver, componentsPath, NameConstants.NT_COMPONENT, JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY);
		Set<String> templateList = getComponentsList(resourceResolver, componentsPath, NameConstants.NT_TEMPLATE, JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY);

		generateRespone(componentList, resourceResolver, searchPaths, jsonArray, JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY);
		generateRespone(templateList, resourceResolver, searchPaths, jsonArray, NameConstants.NN_TEMPLATE);

		try (PrintWriter out = response.getWriter()) {
			out.print(new Gson().toJson(jsonArray));
		}
	}

	private void generateRespone(Set<String> componentList, @NotNull ResourceResolver resourceResolver, String searchPaths, JsonArray jsonArray, String propertyType) {
		componentList.forEach(componentPath -> {
			SearchResult result = executeQuery(resourceResolver, searchPaths, 5, JcrConstants.NT_UNSTRUCTURED, componentPath, propertyType);
			JsonObject jsonResponse = new JsonObject();
			jsonResponse.addProperty("componentPath", componentPath);
			jsonResponse.addProperty("hasMatches", result.getTotalMatches());
			jsonArray.add(jsonResponse);
		});
	}

	private Set<String> getComponentsList(@NotNull ResourceResolver resourceResolver, String componentsPath, String resourceType, String propertyType) {
		Set<String> componentList = new TreeSet<>();
		SearchResult result = executeQuery(resourceResolver, componentsPath, -1, resourceType, StringUtils.EMPTY, propertyType);
		Iterator<Resource> componentResources = result.getResources();
		StreamSupport.stream(((Iterable<Resource>) () -> componentResources).spliterator(), false).forEach(r -> {
			componentList.add(r.getPath());
		});
		return componentList;
	}

	private SearchResult executeQuery(@NotNull ResourceResolver resourceResolver, String root, int limit, String type, String resourceType, String propertyType) {
		QueryBuilder queryBuilder = (QueryBuilder) Objects.requireNonNull(resourceResolver.adaptTo(QueryBuilder.class));
		Map<String, String> map = new HashMap<>();
		Set<String> searchPaths = new HashSet<>(Arrays.asList(StringUtils.split(root, '\n'))).stream().map(String::trim).collect(Collectors.toSet());
		if(searchPaths.size() <= 1){
			map.put("path", root);
		} else {
			AtomicInteger atomicInteger = new AtomicInteger(1);
			searchPaths.forEach(path -> {
				map.put(String.format("group.1_group.%s_path", atomicInteger), root);
			});
			map.put("group.1_group.p.or", "true");
		}
		map.put("type", type);
		map.put("p.guessTotal", "true");
		map.put("p.limit", String.valueOf(limit));
		if(StringUtils.isNotEmpty(resourceType)){
			map.put("group.p.or", "true");
			map.put("group.1_property", propertyType);
			map.put("group.1_property.value", resourceType);
			map.put("group.2_property", propertyType);
			map.put("group.2_property.value", StringUtils.substringAfter(resourceType, "/apps/") );
		}
		Query query = queryBuilder.createQuery(PredicateGroup.create(map), (Session) resourceResolver.adaptTo(Session.class));
		return query.getResult();
	}
}
