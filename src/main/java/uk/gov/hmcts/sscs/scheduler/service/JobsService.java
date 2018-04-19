package uk.gov.hmcts.sscs.scheduler.service;

import static org.quartz.JobBuilder.newJob;
import static uk.gov.hmcts.sscs.scheduler.service.SchedulerExceptionHandlingHelper.call;

import java.util.UUID;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.sscs.scheduler.exceptions.JobException;
import uk.gov.hmcts.sscs.scheduler.jobs.HttpCallJob;
import uk.gov.hmcts.sscs.scheduler.jobs.JobDataKeys;
import uk.gov.hmcts.sscs.scheduler.model.Job;
import uk.gov.hmcts.sscs.scheduler.model.Trigger;
import uk.gov.hmcts.sscs.scheduler.serialize.ActionSerializer;

@Service
public class JobsService {

    private final Scheduler scheduler;
    private final ActionSerializer serializer;

    public JobsService(Scheduler scheduler, ActionSerializer serializer) {
        this.scheduler = scheduler;
        this.serializer = serializer;
    }

    public String create(Job job, String serviceName) {
        try {
            String id = UUID.randomUUID().toString();

            scheduler.scheduleJob(
                newJob(HttpCallJob.class)
                    .withIdentity(id, serviceName)
                    .withDescription(job.name)
                    .usingJobData(JobDataKeys.PARAMS, serializer.serialize(job.action))
                    .requestRecovery()
                    .build(),
                TriggerConverter.toQuartzTrigger(job.trigger)
            );

            return id;

        } catch (SchedulerException exc) {
            throw new JobException("Error while scheduling a job", exc);
        }
    }

    private Job getJobFromDetail(JobDetail jobDetail) {
        Trigger trigger = call(() -> scheduler.getTriggersOfJob(jobDetail.getKey()))
            .stream()
            .filter(quartzTrigger -> quartzTrigger.getJobDataMap().getIntValue(JobDataKeys.ATTEMPT) == 1)
            .findFirst()
            .map(TriggerConverter::toPlatformTrigger)
            .orElse(null);

        return new Job(
            jobDetail.getDescription(),
            serializer.deserialize(jobDetail.getJobDataMap().getString(JobDataKeys.PARAMS)),
            trigger
        );
    }
}
