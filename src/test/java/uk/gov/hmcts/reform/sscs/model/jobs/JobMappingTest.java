package uk.gov.hmcts.reform.sscs.model.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobMappingTest {

    @Mock
    private JobPayloadDeserializer<String> jobPayloadDeserializer;
    @Mock
    private JobExecutor<String> jobExecutor;
    private final String payloadSource = "payloadSource";

    @Test
    void mappingCanHandlePayloadByPayload() {
        JobMapping<String> jobMapping = new JobMapping<>(job -> true, jobPayloadDeserializer, jobExecutor);

        boolean canHandle = jobMapping.canHandle(payloadSource);

        assertThat(canHandle).isTrue();
    }

    @Test
    void mappingCannotHandlePayloadByPayload() {
        JobMapping<String> jobMapping = new JobMapping<>(job -> false, jobPayloadDeserializer, jobExecutor);

        boolean canHandle = jobMapping.canHandle(payloadSource);

        assertThat(canHandle).isFalse();
    }

    @Test
    void deserializesAndExecutesJob() {
        JobMapping<String> jobMapping = new JobMapping<>(job -> true, jobPayloadDeserializer, jobExecutor);

        String deserializedPayload = "deserialized payload";
        when(jobPayloadDeserializer.deserialize(payloadSource)).thenReturn(deserializedPayload);

        String jobId = "jobId";
        String jobGroup = "jobGroup";
        String jobName = "jobName";
        jobMapping.execute(jobId, jobGroup, jobName, payloadSource);

        verify(jobExecutor).execute(jobId, jobGroup, jobName, deserializedPayload);
    }
}
