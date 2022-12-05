package uk.gov.hmcts.reform.sscs.service.quartz;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.exception.JobException;
import uk.gov.hmcts.reform.sscs.model.jobs.JobService;

@Service
public class QuartzJobService implements JobService {

    private final Scheduler scheduler;

    public QuartzJobService(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void start() {

        try {
            scheduler.start();
        } catch (SchedulerException e) {
            throw new JobException("Cannot start Quartz job scheduler", e);
        }
    }

    public void stop(boolean waitForJobsToComplete) {
        try {
            scheduler.shutdown(waitForJobsToComplete);
        } catch (SchedulerException e) {
            throw new JobException("Cannot stop Quartz job scheduler", e);
        }
    }

}
