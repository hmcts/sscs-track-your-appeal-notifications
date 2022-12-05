package uk.gov.hmcts.reform.sscs.service.quartz;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import uk.gov.hmcts.reform.sscs.exception.JobException;
import uk.gov.hmcts.reform.sscs.exception.JobNotFoundException;

@ExtendWith(MockitoExtension.class)
class QuartzJobRemoverTest {

    private final Scheduler scheduler = mock(Scheduler.class);
    private final QuartzJobRemover quartzJobRemover = new QuartzJobRemover(scheduler);

    @Test
    void job_is_removed_from_scheduler_by_id() throws SchedulerException {
        String jobId = "job-id";
        String jobGroup = "job-group";

        given(scheduler.deleteJob(JobKey.jobKey(jobId, jobGroup)))
            .willReturn(true);

        assertThatNoException().isThrownBy(() -> quartzJobRemover.remove(jobId, jobGroup));

        verify(scheduler, times(1)).deleteJob(JobKey.jobKey(jobId, jobGroup));
    }

    @Test
    void job_is_removed_from_scheduler_by_group() throws SchedulerException {
        String jobId1 = "job-id-1";
        String jobId2 = "job-id-2";
        String jobGroup = "job-group";

        JobKey jobKey1 = new JobKey(jobId1, jobGroup);
        JobKey jobKey2 = new JobKey(jobId2, jobGroup);

        Set<JobKey> jobKeysFoundAsSet =
            ImmutableSet.of(jobKey1, jobKey2);

        given(scheduler.getJobKeys(GroupMatcher.groupEquals(jobGroup)))
            .willReturn(jobKeysFoundAsSet);

        List<JobKey> jobKeysFoundAsList = new ArrayList<>(jobKeysFoundAsSet);

        given(scheduler.deleteJobs(jobKeysFoundAsList))
            .willReturn(true);

        assertThatNoException().isThrownBy(() -> quartzJobRemover.removeGroup(jobGroup));

        verify(scheduler, times(1)).deleteJobs(jobKeysFoundAsList);
    }

    @Test
    void remove_job_by_id_throws_when_job_not_found_by_id() throws SchedulerException {
        String jobId = "missing-job-id";
        String jobGroup = "job-group";

        given(scheduler.deleteJob(JobKey.jobKey(jobId, jobGroup))).willReturn(false);

        assertThatThrownBy(() -> quartzJobRemover.remove(jobId, jobGroup))
            .hasMessage("ID: missing-job-id, Group: job-group")
            .isExactlyInstanceOf(JobNotFoundException.class);
    }

    @Test
    void remove_job_by_id_throws_when_group_has_no_jobs() throws SchedulerException {
        String jobGroup = "empty-job-group";

        Set<JobKey> jobKeysFoundAsSet =
            Collections.emptySet();

        given(scheduler.getJobKeys(GroupMatcher.groupEquals(jobGroup)))
            .willReturn(jobKeysFoundAsSet);

        assertThatThrownBy(() -> quartzJobRemover.removeGroup(jobGroup))
            .hasMessage("Group: empty-job-group")
            .isExactlyInstanceOf(JobNotFoundException.class);

        verify(scheduler, never()).deleteJobs(any());
    }

    @Test
    void remove_job_by_id_throws_when_group_has_jobs_that_cannot_be_found() throws SchedulerException {
        String jobGroup = "empty-job-group";

        Set<JobKey> jobKeysFoundAsSet =
            Collections.emptySet();

        given(scheduler.getJobKeys(GroupMatcher.groupEquals(jobGroup)))
            .willReturn(jobKeysFoundAsSet);

        assertThatThrownBy(() -> quartzJobRemover.removeGroup(jobGroup))
            .hasMessage("Group: empty-job-group")
            .isExactlyInstanceOf(JobNotFoundException.class);

        verify(scheduler, never()).deleteJobs(any());
    }

    @Test
    void remove_job_by_group_throws_when_quartz_fails() throws SchedulerException {
        String jobId1 = "job-id-1";
        String jobId2 = "job-id-2";
        String jobGroup = "failing-job-group";

        JobKey jobKey1 = new JobKey(jobId1, jobGroup);
        JobKey jobKey2 = new JobKey(jobId2, jobGroup);

        Set<JobKey> jobKeysFoundAsSet =
            ImmutableSet.of(jobKey1, jobKey2);

        given(scheduler.getJobKeys(GroupMatcher.groupEquals(jobGroup)))
            .willReturn(jobKeysFoundAsSet);

        List<JobKey> jobKeysFoundAsList = new ArrayList<>(jobKeysFoundAsSet);

        doThrow(SchedulerException.class)
            .when(scheduler)
            .deleteJobs(jobKeysFoundAsList);

        assertThatThrownBy(() -> quartzJobRemover.removeGroup(jobGroup))
            .hasMessage("Error while removing Job by Group. Group: failing-job-group")
            .isExactlyInstanceOf(JobException.class);
    }

    @Test
    void remove_job_by_id_throws_when_quartz_fails() throws SchedulerException {
        String jobId = "failing-job-id";
        String jobGroup = "job-group";

        doThrow(SchedulerException.class)
            .when(scheduler)
            .deleteJob(JobKey.jobKey(jobId, jobGroup));


        assertThatThrownBy(() -> quartzJobRemover.remove(jobId, jobGroup))
            .hasMessage("Error while removing Job. ID: failing-job-id, Group: job-group")
            .isExactlyInstanceOf(JobException.class);
    }

}
