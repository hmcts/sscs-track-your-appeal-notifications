package uk.gov.hmcts.sscs.scheduler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.sscs.scheduler.SampleData.validJob;

import java.sql.Date;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import uk.gov.hmcts.sscs.scheduler.exceptions.JobException;
import uk.gov.hmcts.sscs.scheduler.jobs.HttpCallJob;
import uk.gov.hmcts.sscs.scheduler.jobs.JobDataKeys;
import uk.gov.hmcts.sscs.scheduler.model.Job;
import uk.gov.hmcts.sscs.scheduler.serialize.ActionSerializer;

@RunWith(MockitoJUnitRunner.class)
public class CreateJobTest {

    @Mock private Scheduler scheduler;
    @Mock private ActionSerializer actionSerializer;

    private JobsService jobsService;

    @Before
    public void setUp() {
        jobsService = new JobsService(scheduler, actionSerializer);
    }

    @Test
    public void should_use_service_name_as_job_group() throws Exception {
        String serviceName = "hello!";

        jobsService.create(validJob(), serviceName);

        assertThat(jobDetailPassedToScheduler().getKey().getGroup())
            .isEqualTo(serviceName);
    }

    @Test
    public void should_schedule_job_of_correct_type() throws Exception {
        jobsService.create(validJob(), "some service name");

        assertThat(jobDetailPassedToScheduler().getJobClass())
            .isEqualTo(HttpCallJob.class);
    }

    @Test
    public void should_store_serialized_action_details_in_job_data_map() throws Exception {
        String serializedAction = "fake-action-representation";
        given(actionSerializer.serialize(any())).willReturn(serializedAction);

        jobsService.create(validJob(), "some service name");

        assertThat(jobDetailPassedToScheduler().getJobDataMap().getString(JobDataKeys.PARAMS))
            .isEqualTo(serializedAction);
    }

    @Test
    public void should_create_a_trigger_with_datetime_specified_by_caller() throws Exception {
        Job job = validJob();
        jobsService.create(job, "some service name");

        assertThat(triggerPassedToScheduler().getStartTime())
            .isEqualTo(Date.from(job.trigger.startDateTime.toInstant()));
    }

    @Test
    public void should_throw_an_exception_when_scheduler_fails() throws Exception {
        given(scheduler.scheduleJob(any(), any())).willThrow(new SchedulerException());

        JobsService jobsService = new JobsService(scheduler, actionSerializer);

        Throwable exc = catchThrowable(() -> jobsService.create(validJob(), "foo"));

        assertThat(exc)
            .isInstanceOf(JobException.class)
            .hasCauseInstanceOf(SchedulerException.class);
    }

    private Trigger triggerPassedToScheduler() throws Exception {
        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
        verify(scheduler).scheduleJob(any(), triggerCaptor.capture());
        return triggerCaptor.getValue();
    }

    private JobDetail jobDetailPassedToScheduler() throws Exception {
        ArgumentCaptor<JobDetail> jobDetailCaptor = ArgumentCaptor.forClass(JobDetail.class);
        verify(scheduler).scheduleJob(jobDetailCaptor.capture(), any());
        return jobDetailCaptor.getValue();
    }

}
