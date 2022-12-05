package uk.gov.hmcts.reform.sscs.service.scheduler;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.model.jobs.JobPayloadDeserializer;

@Component
public class CcdActionDeserializer implements JobPayloadDeserializer<String> {

    @Override
    public String deserialize(String payload) {
        return payload;
    }
}
