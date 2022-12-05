package uk.gov.hmcts.reform.sscs.service.quartz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.model.jobs.JobDataKeys.PAYLOAD;

import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import uk.gov.hmcts.reform.sscs.exception.JobException;
import uk.gov.hmcts.reform.sscs.model.jobs.Job;
import uk.gov.hmcts.reform.sscs.model.jobs.JobClassMapper;
import uk.gov.hmcts.reform.sscs.model.jobs.JobClassMapping;

@ExtendWith(MockitoExtension.class)
class QuartzJobSchedulerTest {

    private final Scheduler scheduler = mock(Scheduler.class);
    private final JobClassMapper jobClassMapper = mock(JobClassMapper.class);
    private final QuartzJobScheduler quartzJobScheduler = new QuartzJobScheduler(
        scheduler, jobClassMapper
    );
    private final JobClassMapping jobClassMapping = mock(JobClassMapping.class);

    @Test
    void job_is_scheduled() throws SchedulerException {
        String jobGroup = "test-job-group";
        String jobName = "test-job-name";
        String jobPayload = "payload";
        ZonedDateTime triggerAt = ZonedDateTime.now();

        Job<String> job = new Job<>(
            jobGroup,
            jobName,
            jobPayload,
            triggerAt
        );

        given(jobClassMapper.getJobMapping(String.class)).willReturn(jobClassMapping);
        given(jobClassMapping.serialize(jobPayload)).willReturn("serialized-payload");

        String actualJobId = quartzJobScheduler.schedule(job);

        ArgumentCaptor<JobDetail> jobDetailCaptor = ArgumentCaptor.forClass(JobDetail.class);
        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);

        verify(scheduler, times(1)).scheduleJob(
            jobDetailCaptor.capture(),
            triggerCaptor.capture()
        );

        JobDetail actualJobDetail = jobDetailCaptor.getValue();
        assertThat(actualJobDetail.getKey().getName()).isEqualTo(actualJobId);
        assertThat(actualJobDetail.getKey().getGroup()).isEqualTo(jobGroup);
        assertThat(actualJobDetail.getDescription()).isEqualTo(jobName);
        assertThat(actualJobDetail.getJobDataMap())
            .containsKey(PAYLOAD.getKey())
            .extractingByKey(PAYLOAD.getKey())
            .isEqualTo("serialized-payload");

        Trigger actualTrigger = triggerCaptor.getValue();
        assertThat(triggerAt.toInstant().toEpochMilli())
            .isEqualTo(actualTrigger.getStartTime().toInstant().toEpochMilli());
    }

    @Test
    void schedule_wraps_exception_from_client_deserializer() {
        String jobGroup = "test-job-group";
        String jobName = "test-job";
        String jobPayload = "payload";
        ZonedDateTime triggerAt = ZonedDateTime.now();

        Job<String> job = new Job<>(
            jobGroup,
            jobName,
            jobPayload,
            triggerAt
        );

        given(jobClassMapper.getJobMapping(String.class)).willReturn(jobClassMapping);
        doThrow(RuntimeException.class)
            .when(jobClassMapping)
            .serialize(jobPayload);

        assertThatThrownBy(() -> quartzJobScheduler.schedule(job))
            .hasMessage("Error while scheduling job")
            .isExactlyInstanceOf(JobException.class);
    }

    @Test
    void schedule_throws_when_quartz_fails() throws SchedulerException {
        String jobGroup = "test-job-group";
        String jobName = "test-job";
        String jobPayload = "payload";
        ZonedDateTime triggerAt = ZonedDateTime.now();

        given(jobClassMapper.getJobMapping(String.class)).willReturn(jobClassMapping);
        given(jobClassMapping.serialize(jobPayload)).willReturn("serialized-payload");

        doThrow(RuntimeException.class)
            .when(scheduler)
            .scheduleJob(any(), any());

        Job<String> job = new Job<>(jobGroup, jobName, jobPayload, triggerAt);

        assertThatThrownBy(() -> quartzJobScheduler.schedule(job))
            .hasMessage("Error while scheduling job")
            .isExactlyInstanceOf(JobException.class);
    }
}
