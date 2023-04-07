package com.aem.operations.core.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.aem.operations.core.beans.JobManagerBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.Statistics;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;

import com.adobe.acs.commons.mcp.form.GeneratedDialog;
import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentManager;

@Model(adaptables = { Resource.class, SlingHttpServletRequest.class }, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ClearOutExcessSlingJobsModel extends GeneratedDialog {

	@OSGiService
	private JobManager jobManager;

	@OSGiService
	private DiscoveryService discoveryService;
	
	@OSGiService
	private AgentManager agentManager;

	public String getNumberOfQueuedJobs() {
		Statistics stats = jobManager.getStatistics();
		return "jobs queued: " + stats.getNumberOfQueuedJobs();
	}

	public String getNumberOfActiveJobs() {
		Statistics stats = jobManager.getStatistics();
		long now = System.currentTimeMillis();
		long activeJobs = stats.getNumberOfActiveJobs();
		long lastFinishedJobTime;
		long diff;
		if (activeJobs > 0L) {
			lastFinishedJobTime = stats.getLastActivatedJobTime();
			diff = now - lastFinishedJobTime;
			if (diff > 3600000L) {
				return "There are active jobs but the last job activated was over 3600sec ago (" + (diff / 1000L)
						+ "sec) and is not yet finished";
			}
		}
		return StringUtils.EMPTY;
	}

	public String getLastOfQueuedJobs() {
		Statistics stats = jobManager.getStatistics();
		long now = System.currentTimeMillis();
		long queuedJobs = stats.getNumberOfQueuedJobs();
		long lastFinishedJobTime;
		long diff;

		if (queuedJobs > 0L) {
			lastFinishedJobTime = stats.getLastFinishedJobTime();
			diff = now - lastFinishedJobTime;
			if (diff > 3600000L) {
				return "There are queued jobs but the last job finished was over 3600sec ago (" + (diff / 1000L)
						+ "sec)";
			}
		}
		return StringUtils.EMPTY;
	}

	public List<JobManagerBean> getSlingJobStatus() {
		
		Map<String, Agent> agents = this.agentManager.getAgents();		
		Set<String> keys = agents.keySet().stream().map(key -> "com/day/cq/replication/job/"+ key).collect(Collectors.toSet());		
		Set<String> enabledTopics = getTopics();
		enabledTopics.addAll(keys);
		List<JobManagerBean> jobManagerBeans = new ArrayList<>();
		enabledTopics.stream().forEach(topic -> {						
			Collection<Job> activeJobs = jobManager.findJobs(JobManager.QueryType.ACTIVE, topic, 100, (Map<String, Object>[]) null);			
			Collection<Job> allJobs = jobManager.findJobs(JobManager.QueryType.ALL, topic, 100, (Map<String, Object>[]) null);		
			Collection<Job> cancelledJobs = jobManager.findJobs(JobManager.QueryType.CANCELLED, topic, 100, (Map<String, Object>[]) null);			
			Collection<Job> droppedJobs = jobManager.findJobs(JobManager.QueryType.DROPPED, topic, 100, (Map<String, Object>[]) null);			
			Collection<Job> errorJobs = jobManager.findJobs(JobManager.QueryType.ERROR, topic, 100, (Map<String, Object>[]) null);			
			Collection<Job> givenUpJobs = jobManager.findJobs(JobManager.QueryType.GIVEN_UP, topic, 100, (Map<String, Object>[]) null);			
			Collection<Job> historyJobs = jobManager.findJobs(JobManager.QueryType.HISTORY, topic, 100, (Map<String, Object>[]) null);			
			Collection<Job> queuedJobs = jobManager.findJobs(JobManager.QueryType.QUEUED, topic, 100, (Map<String, Object>[]) null);			
			Collection<Job> stoppedJobs = jobManager.findJobs(JobManager.QueryType.STOPPED, topic, 100, (Map<String, Object>[]) null);			
			Collection<Job> succeededJobs = jobManager.findJobs(JobManager.QueryType.SUCCEEDED, topic, 100, (Map<String, Object>[]) null);
			if (!activeJobs.isEmpty() || !allJobs.isEmpty() || !cancelledJobs.isEmpty() || !droppedJobs.isEmpty()
					|| !errorJobs.isEmpty() || !givenUpJobs.isEmpty() || !historyJobs.isEmpty() || !queuedJobs.isEmpty()
					|| !stoppedJobs.isEmpty() || !succeededJobs.isEmpty()) {
				JobManagerBean jobManagerBean = new JobManagerBean();
				jobManagerBean.setJobTopic(topic);
				jobManagerBean.setActiveJobs(activeJobs.size());
				jobManagerBean.setAllJobs(allJobs.size());
				jobManagerBean.setCancelledJobs(cancelledJobs.size());
				jobManagerBean.setDroppedJobs(droppedJobs.size());
				jobManagerBean.setErrorJobs(errorJobs.size());
				jobManagerBean.setGivenUpJobs(givenUpJobs.size());
				jobManagerBean.setHistoryJobs(historyJobs.size());
				jobManagerBean.setQueuedJobs(queuedJobs.size());
				jobManagerBean.setStoppedJobs(stoppedJobs.size());
				jobManagerBean.setSucceededJobs(succeededJobs.size());
				jobManagerBeans.add(jobManagerBean);
			}									
		});
		return jobManagerBeans;
	}

	@SuppressWarnings("rawtypes")
	private Set<String> getTopics() {
		TopologyView topology = discoveryService.getTopology();
		Set<InstanceDescription> instances = topology.getInstances();
		Iterator instanceIt = instances.iterator();
		Set<String> enabledTopics = new TreeSet<>();
		while (instanceIt.hasNext()) {
			InstanceDescription instance = (InstanceDescription) instanceIt.next();
			enabledTopics = expandCSV(instance.getProperty("org.apache.sling.event.jobs.consumer.topics"));
		}
		return enabledTopics;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Set<String> expandCSV(String csvString) {
		Set<String> set = new TreeSet();
		if (StringUtils.isNotEmpty(csvString)) {
			set.addAll(Set.of(csvString.split(",")));
		}
		return set;
	}
}