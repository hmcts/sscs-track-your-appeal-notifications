package uk.gov.hmcts.sscs.service.scheduler;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobPayloadDeserializer;

@Component
public class ActionDeserializer implements JobPayloadDeserializer<String> {

    @Override
    public String deserialize(String payload) {
        return null;
    }
}
