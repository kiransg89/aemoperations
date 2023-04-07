package com.aem.operations.core.beans;

import lombok.Data;

@Data
public class JobManagerBean {
	
	long allJobs;
	long activeJobs;
	long cancelledJobs;
	long droppedJobs;
	long errorJobs;
	long givenUpJobs;
	long historyJobs;
	long queuedJobs;
	long stoppedJobs;	
	long succeededJobs;
	String jobTopic;
}
