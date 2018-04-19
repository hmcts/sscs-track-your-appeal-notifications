package uk.gov.hmcts.sscs.scheduler.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.quartz.JobBuilder.newJob;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.quartz.JobExecutionContext;
import org.springframework.http.HttpMethod;
import uk.gov.hmcts.sscs.scheduler.model.HttpAction;
import uk.gov.hmcts.sscs.scheduler.serialize.ActionSerializer;

@RunWith(MockitoJUnitRunner.class)
public class ActionExtractorTest {

    @Test
    public void should_extract_action_from_job_context() {
        // given

        ActionSerializer serializer = new ActionSerializer();
        JobExecutionContext context = mock(JobExecutionContext.class);

        HttpAction scheduledAction =
            new HttpAction(
                "https://example.com",
                HttpMethod.POST,
                ImmutableMap.of("a", "b"),
                "Hello!"
            );

        given(context.getJobDetail())
            .willReturn(
                newJob(HttpCallJob.class)
                    .withIdentity("irrelevant job id", "irrelevant job group")
                    .usingJobData(JobDataKeys.PARAMS, serializer.serialize(scheduledAction))
                    .requestRecovery()
                    .build()
            );

        ActionExtractor actionExtractor = new ActionExtractor(serializer);

        // when
        HttpAction extractedAction = actionExtractor.extract(context);

        // then
        assertThat(extractedAction).isEqualToComparingFieldByField(scheduledAction);
    }
}
