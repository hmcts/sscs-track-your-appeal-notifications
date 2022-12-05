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
class JobClassMapperTest {

    @Mock
    private JobClassMapping<Integer> jobMappingInteger;

    @Mock
    private JobClassMapping<String> jobMappingString;

    @Test
    void getsCorrectJobMappingByPayloadClass() {
        given(jobMappingInteger.canHandle(String.class)).willReturn(false);
        given(jobMappingString.canHandle(String.class)).willReturn(true);

        JobClassMapper jobMapper = new JobClassMapper(List.of(jobMappingInteger, jobMappingString));
        JobClassMapping<String> jobMapping = jobMapper.getJobMapping(String.class);

        assertThat(jobMapping).isEqualTo(jobMappingString);
    }

    @Test
    void cannotFindMappingForPayloadClass() {
        given(jobMappingInteger.canHandle(String.class)).willReturn(false);

        JobClassMapper jobMapper = new JobClassMapper(List.of(jobMappingInteger));

        assertThatThrownBy(() -> jobMapper.getJobMapping(String.class))
            .isExactlyInstanceOf(IllegalArgumentException.class);
    }
}
