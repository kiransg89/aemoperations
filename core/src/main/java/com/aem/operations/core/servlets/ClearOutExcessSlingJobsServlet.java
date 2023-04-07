package com.aem.operations.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.JobManager.QueryType;
import org.apache.sling.servlets.post.JSONResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.granite.rest.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

@Component(service = { Servlet.class }, property = {
		"sling.servlet.paths=" + ClearOutExcessSlingJobsServlet.RESOURCE_PATH, "sling.servlet.methods=POST" })
public class ClearOutExcessSlingJobsServlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 1L;
	public static final String RESOURCE_PATH = "/bin/triggerclearoutexcessslingjobs";
	private static final String MESSAGE = "message";

	@Reference
	private JobManager jobManager;	

	@Override
	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {
		// Set response headers.
		response.setContentType(JSONResponse.RESPONSE_CONTENT_TYPE);
		response.setCharacterEncoding(Constants.DEFAULT_CHARSET);
		JsonObject jsonResponse = new JsonObject();					
		
		String jobTopic = request.getParameter("jobTopic");
		if(StringUtils.isNotEmpty(jobTopic)) {
			cleanSlingJobs(jobTopic, JobManager.QueryType.ACTIVE);
			cleanSlingJobs(jobTopic, JobManager.QueryType.ALL);
			cleanSlingJobs(jobTopic, JobManager.QueryType.CANCELLED);
			cleanSlingJobs(jobTopic, JobManager.QueryType.DROPPED);
			cleanSlingJobs(jobTopic, JobManager.QueryType.ERROR);
			cleanSlingJobs(jobTopic, JobManager.QueryType.GIVEN_UP);
			cleanSlingJobs(jobTopic, JobManager.QueryType.HISTORY);
			cleanSlingJobs(jobTopic, JobManager.QueryType.QUEUED);
			cleanSlingJobs(jobTopic, JobManager.QueryType.STOPPED);
			cleanSlingJobs(jobTopic, JobManager.QueryType.SUCCEEDED);		
			jsonResponse.addProperty(MESSAGE, "Jobs are cleared");
		} else {
			jsonResponse.addProperty(MESSAGE, "Job Topic is Empty");
		}		
		try (PrintWriter out = response.getWriter()) {
			out.print(new Gson().toJson(jsonResponse));
		}
	}

	private void cleanSlingJobs(String jobTopic, QueryType type) {
		Collection<Job> allJobs = jobManager.findJobs(type, jobTopic, 1000, (Map<String, Object>[]) null);
		if(!allJobs.isEmpty()) {
			allJobs.stream().forEach(childJob -> {
				jobManager.stopJobById(childJob.getId());
				jobManager.removeJobById(childJob.getId());
			});
			cleanSlingJobs(jobTopic, type);
		}		
	}	
}
