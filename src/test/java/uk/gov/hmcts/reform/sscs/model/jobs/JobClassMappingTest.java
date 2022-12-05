package uk.gov.hmcts.reform.sscs.model.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobClassMappingTest {

    @Mock
    private JobPayloadSerializer<String> jobPayloadSerializer;
    private JobClassMapping<String> jobMapping;

    @BeforeEach
    void setup() {
        jobMapping = new JobClassMapping<>(String.class, jobPayloadSerializer);
    }

    @Test
    void mappingCanHandlePayloadByClass() {
        boolean canHandle = jobMapping.canHandle(String.class);

        assertThat(canHandle).isTrue();
    }

    @Test
    void mappingCannotHandlePayloadByClass() {
        boolean canHandle = jobMapping.canHandle(Integer.class);

        assertThat(canHandle).isFalse();
    }

    @Test
    void serialize() {
        String payload = "payload";
        jobMapping.serialize(payload);
        verify(jobPayloadSerializer).serialize(payload);
    }
}
