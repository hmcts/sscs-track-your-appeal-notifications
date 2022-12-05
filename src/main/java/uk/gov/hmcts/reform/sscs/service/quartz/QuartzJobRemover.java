package uk.gov.hmcts.reform.sscs.service.quartz;

import java.util.List;
import java.util.stream.Collectors;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.exception.JobException;
import uk.gov.hmcts.reform.sscs.exception.JobNotFoundException;
import uk.gov.hmcts.reform.sscs.model.jobs.JobRemover;

@Service
public class QuartzJobRemover implements JobRemover {

    private final Scheduler scheduler;

    public QuartzJobRemover(
        Scheduler scheduler
    ) {
        this.scheduler = scheduler;
    }

    public void remove(String jobId, String jobGroup) {
        try {

            boolean jobFound = scheduler.deleteJob(JobKey.jobKey(jobId, jobGroup));
            if (!jobFound) {
                throw new JobNotFoundException("ID: " + jobId + ", Group: " + jobGroup);
            }

        } catch (SchedulerException e) {
            throw new JobException(
                "Error while removing Job. ID: " + jobId + ", Group: " + jobGroup,
                e
            );
        }
    }

    public void removeGroup(String jobGroup) {
        try {

            List<JobKey> jobKeys = scheduler
                .getJobKeys(GroupMatcher.groupEquals(jobGroup))
                .stream()
                .collect(Collectors.toList());

            if (jobKeys.isEmpty()) {
                throw new JobNotFoundException("Group: " + jobGroup);
            }

            boolean jobsFound = scheduler.deleteJobs(jobKeys);
            if (!jobsFound || jobKeys.isEmpty()) {
                throw new JobNotFoundException("Group: " + jobGroup);
            }

        } catch (SchedulerException e) {
            throw new JobException(
                "Error while removing Job by Group. Group: " + jobGroup,
                e
            );
        }
    }

}
