package uk.gov.hmcts.reform.sscs.service.quartz;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.model.jobs.JobDataKeys.PAYLOAD;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import uk.gov.hmcts.reform.sscs.model.jobs.JobMapper;
import uk.gov.hmcts.reform.sscs.model.jobs.JobMapping;

@ExtendWith(MockitoExtension.class)
class QuartzExecutionHandlerTest {
    @Mock
    private JobMapper jobMapper;
    @InjectMocks
    private QuartzExecutionHandler quartzExecutionHandler;
    @Mock
    private JobMapping jobMapping;
    @Mock
    private JobExecutionContext context;
    @Mock
    private JobDetail jobDetail;
    @Mock
    private JobDataMap jobDataMap;

    @Test
    void execute_deserializes_payload_and_delegates_execution() {
        given(context.getJobDetail()).willReturn(jobDetail);
        given(jobDetail.getKey()).willReturn(new JobKey("job-id", "job-group"));
        given(jobDetail.getDescription()).willReturn("job-name");
        given(jobDetail.getJobDataMap()).willReturn(jobDataMap);

        given(jobDataMap.containsKey(PAYLOAD.getKey())).willReturn(true);
        given(jobDataMap.getString(PAYLOAD.getKey())).willReturn("payload-stuff");
        given(jobMapper.getJobMapping("payload-stuff")).willReturn(jobMapping);

        assertThatNoException().isThrownBy(() -> quartzExecutionHandler.execute(context));

        verify(jobMapping, times(1)).execute(
            "job-id", "job-group", "job-name", "payload-stuff");
    }

    @Test
    void execute_uses_empty_payload_when_not_set() {
        given(context.getJobDetail()).willReturn(jobDetail);
        given(jobDetail.getKey()).willReturn(new JobKey("job-id", "job-group"));
        given(jobDetail.getDescription()).willReturn("job-name");
        given(jobDetail.getJobDataMap()).willReturn(jobDataMap);

        given(jobDataMap.containsKey(PAYLOAD.getKey())).willReturn(false);
        given(jobMapper.getJobMapping("")).willReturn(jobMapping);

        assertThatNoException().isThrownBy(() -> quartzExecutionHandler.execute(context));

        verify(jobMapping, times(1)).execute(
            "job-id", "job-group", "job-name", "");

    }

    @Test
    void execute_wraps_exception_from_client_deserializer() {
        given(context.getJobDetail()).willReturn(jobDetail);
        given(jobDetail.getKey()).willReturn(new JobKey("job-id", "job-group"));
        given(jobDetail.getDescription()).willReturn("job-name");
        given(jobDetail.getJobDataMap()).willReturn(jobDataMap);

        given(jobDataMap.containsKey(PAYLOAD.getKey())).willReturn(false);

        assertThatThrownBy(() -> quartzExecutionHandler.execute(context))
            .hasMessage("Job failed. Job ID: job-id");
    }
}
