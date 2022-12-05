package uk.gov.hmcts.reform.sscs.model.jobs;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.time.ZonedDateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("integration")
@AutoConfigureMockMvc
public class JobsTestIT {

    @Autowired
    @Qualifier("scheduler")
    private Scheduler quartzScheduler;

    @Autowired
    private JobService jobService;

    @Autowired
    private JobScheduler jobScheduler;

    @Autowired
    private JobRemover jobRemover;

    @MockBean
    private JobPayloadSerializer<TestPayload> jobPayloadSerializer;

    @MockBean
    private JobPayloadDeserializer<TestPayload> jobPayloadDeserializer;

    @MockBean
    private JobExecutor<TestPayload> jobExecutor;

    @MockBean
    private JobClassMapper jobClassMapper;

    @MockBean
    private JobMapper jobMapper;

    TestPayload testPayload = new TestPayload();

    @Before
    public void setUp() {

        jobService.start();

        try {
            quartzScheduler.clear();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }

        given(jobPayloadSerializer.serialize(testPayload)).willReturn("serialized-payload");
        given(jobPayloadDeserializer.deserialize("serialized-payload")).willReturn(testPayload);

        given(jobClassMapper.getJobMapping(TestPayload.class)).willReturn(new JobClassMapping<>(TestPayload.class, jobPayloadSerializer));
        given(jobMapper.getJobMapping(any())).willReturn(new JobMapping<>(x -> true, jobPayloadDeserializer, jobExecutor));
    }

    @Test
    public void jobIsScheduledAndExecutesInTheFuture() {

        assertTrue("Job scheduler is empty at start", getScheduledJobCount() == 0);

        String jobGroup = "test-job-group";
        String jobName = "test-job-name";

        Job<TestPayload> job = new Job<>(
            jobGroup,
            jobName,
            testPayload,
            ZonedDateTime.now().plusSeconds(2)
        );

        String jobId = jobScheduler.schedule(job);

        assertNotNull(jobId);

        assertTrue("Job was scheduled into Quartz", getScheduledJobCount() == 1);

        // job is executed
        verify(jobExecutor, timeout(10000)).execute(
            eq(jobId),
            eq(jobGroup),
            eq(jobName),
            eq(testPayload)
        );
    }

    @Test
    public void jobIsScheduledAndThenRemovedByGroup() {

        assertTrue("Job scheduler is empty at start", getScheduledJobCount() == 0);

        String jobGroup = "test-job-group";
        String jobName = "test-job-name";

        Job<TestPayload> job1 = new Job<>(
            jobGroup,
            jobName,
            testPayload,
            ZonedDateTime.now().plusSeconds(2)
        );

        String jobId1 = jobScheduler.schedule(job1);

        assertNotNull(jobId1);

        Job<TestPayload> job2 = new Job<>(
            jobGroup,
            jobName,
            testPayload,
            ZonedDateTime.now().plusSeconds(2)
        );

        String jobId2 = jobScheduler.schedule(job2);

        assertNotNull(jobId2);

        assertTrue("Jobs were scheduled into Quartz", getScheduledJobCount() == 2);

        jobRemover.removeGroup(jobGroup);

        assertTrue("Jobs were removed from Quartz after execution", getScheduledJobCount() == 0);

        // jobs are /never/ executed
        verify(jobExecutor, after(10000).never()).execute(
            eq(jobId1),
            eq(jobGroup),
            eq(jobName),
            eq(testPayload)
        );

        verify(jobExecutor, after(10000).never()).execute(
            eq(jobId2),
            eq(jobGroup),
            eq(jobName),
            eq(testPayload)
        );
    }

    @Test
    public void jobIsScheduledAndThenRemovedById() {

        assertTrue("Job scheduler is empty at start", getScheduledJobCount() == 0);

        String jobGroup = "test-job-group";
        String jobName = "test-job-name";

        Job<TestPayload> job = new Job<>(
            jobGroup,
            jobName,
            testPayload,
            ZonedDateTime.now().plusSeconds(2)
        );

        String jobId = jobScheduler.schedule(job);

        assertNotNull(jobId);

        assertTrue("Job was scheduled into Quartz", getScheduledJobCount() == 1);

        jobRemover.remove(jobId, jobGroup);

        assertTrue("Job was removed from Quartz after execution", getScheduledJobCount() == 0);

        // job is /never/ executed
        verify(jobExecutor, after(10000).never()).execute(
            eq(jobId),
            eq(jobGroup),
            eq(jobName),
            eq(testPayload)
        );
    }

    public int getScheduledJobCount() {

        try {

            return quartzScheduler
                .getJobKeys(GroupMatcher.anyGroup())
                .size();

        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    private class TestPayload {

        public String getFoo() {
            return "bar";
        }
    }

}
