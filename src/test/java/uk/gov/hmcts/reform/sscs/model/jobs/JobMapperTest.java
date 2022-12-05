package uk.gov.hmcts.reform.sscs.model.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobMapperTest {
    @Mock
    private JobMapping<Integer> jobMappingInteger;

    @Mock
    private JobMapping<String> jobMappingString;


    @Test
    void getCorrectJobMappingByPayload() {
        String payload = "some payload";

        given(jobMappingInteger.canHandle(payload)).willReturn(false);
        given(jobMappingString.canHandle(payload)).willReturn(true);

        JobMapper jobMapper = new JobMapper(List.of(jobMappingInteger, jobMappingString));
        JobMapping jobMapping = jobMapper.getJobMapping(payload);

        assertThat(jobMapping).isEqualTo(jobMappingString);
    }

    @Test
    void cannotFindMappingForPayload() {
        String payload = "some payload";
        given(jobMappingInteger.canHandle(payload)).willReturn(false);

        JobMapper jobMapper = new JobMapper(List.of(jobMappingInteger));

        assertThatThrownBy(() -> jobMapper.getJobMapping(payload))
            .isExactlyInstanceOf(IllegalArgumentException.class);
    }
}
