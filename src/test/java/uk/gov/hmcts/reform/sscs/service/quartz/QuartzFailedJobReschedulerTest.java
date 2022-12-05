package uk.gov.hmcts.reform.sscs.service.quartz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.quartz.JobBuilder.newJob;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.Trigger;

@ExtendWith(MockitoExtension.class)
class QuartzFailedJobReschedulerTest {

    private static final int MAX_NUMBER_OF_ATTEMPTS = 5;
    private static final Duration DELAY_BETWEEN_ATTEMPTS = Duration.ofMillis(1000);
    private static final String ATTEMPT_JOB_DATA_KEY = "attempt";

    @Mock
    private Scheduler scheduler;
    @Mock
    private JobExecutionContext context;
    private final JobExecutionException jobExecutionException = new JobExecutionException("test");

    private final QuartzFailedJobRescheduler rescheduler = new QuartzFailedJobRescheduler(
        MAX_NUMBER_OF_ATTEMPTS,
        DELAY_BETWEEN_ATTEMPTS
    );

    @Test
    void getName_does_not_throw_exception() {
        assertThatNoException().isThrownBy(rescheduler::getName);
    }

    @Test
    void jobToBeExecuted_does_not_throw_exception() {
        assertThatNoException().isThrownBy(() -> rescheduler.jobToBeExecuted(context));
    }

    @Test
    void jobExecutionVetoed_does_not_throw_exception() {
        assertThatNoException().isThrownBy(() -> rescheduler.jobExecutionVetoed(context));
    }

    @Test
    void jobWasExecuted_should_not_reschedule_successful_job() throws Exception {
        rescheduler.jobWasExecuted(context, null);

        verify(scheduler, never()).scheduleJob(any());
    }

    @Test
    void jobWasExecuted_should_reschedule_failed_job() throws Exception {
        // given
        JobDetail jobDetail = newJob(Job.class)
            .withIdentity("id123")
            .build();
        given(context.getJobDetail()).willReturn(jobDetail);
        int lastAttempt = 2;
        Map<String, Object> originalJobDataMap = createSampleMap(lastAttempt);
        given(context.getMergedJobDataMap()).willReturn(convertToJobDataMap(originalJobDataMap));
        given(context.getScheduler()).willReturn(scheduler);

        // when
        rescheduler.jobWasExecuted(context, jobExecutionException);

        //then
        Instant rescheduleTime = Instant.now();

        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
        verify(scheduler).scheduleJob(triggerCaptor.capture());

        Trigger trigger = triggerCaptor.getValue();
        assertTriggerStartTimeIsCorrect(trigger, rescheduleTime);
        assertSameMapWithIncrementedAttempt(trigger.getJobDataMap(), originalJobDataMap);
        assertThat(trigger.getJobKey()).isEqualTo(jobDetail.getKey());
    }

    @Test
    void jobWasExecuted_should_not_reschedule_failed_job_too_many_times() throws Exception {
        // given
        JobDetail jobDetail = newJob(Job.class)
            .withIdentity("id123")
            .build();
        given(context.getJobDetail()).willReturn(jobDetail);
        JobDataMap jobDataMap =
            convertToJobDataMap(ImmutableMap.of(ATTEMPT_JOB_DATA_KEY, MAX_NUMBER_OF_ATTEMPTS));
        given(context.getMergedJobDataMap()).willReturn(jobDataMap);

        // when
        rescheduler.jobWasExecuted(context, jobExecutionException);

        // then
        verify(scheduler, never()).scheduleJob(any());
    }

    private void assertTriggerStartTimeIsCorrect(
        Trigger trigger,
        Instant startOfDelayPeriod
    ) {
        Duration actualDelay =
            Duration.between(startOfDelayPeriod, trigger.getStartTime().toInstant());

        // check if trigger is set to fire after the expected delay, with 100-millisecond tolerance
        assertThat(DELAY_BETWEEN_ATTEMPTS.minus(actualDelay)).isLessThan(Duration.ofMillis(100));
    }

    private void assertSameMapWithIncrementedAttempt(
        Map<String, Object> compared,
        Map<String, Object> original
    ) {
        Map<String, Object> expected = incrementAttempt(original);

        assertThat(compared).containsAllEntriesOf(expected);
        assertThat(compared).hasSameSizeAs(expected);
    }

    private Map<String, Object> incrementAttempt(Map<String, Object> map) {
        Map<String, Object> updatedMap = Maps.newHashMap(map);
        updatedMap.put(ATTEMPT_JOB_DATA_KEY, (int) map.get(ATTEMPT_JOB_DATA_KEY) + 1);
        return updatedMap;
    }

    private JobDataMap convertToJobDataMap(Map<?, ?> innerMap) {
        return new JobDataMap(innerMap);
    }

    private Map<String, Object> createSampleMap(int attempt) {
        return ImmutableMap.of(
            "params", "some params",
            ATTEMPT_JOB_DATA_KEY, attempt,
            "more params", "..."
        );
    }
}
