package uk.gov.hmcts.sscs.service.scheduler;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobPayloadSerializer;

@Component
public class ActionSerializer implements JobPayloadSerializer<String> {

    @Override
    public String serialize(String payload) {
        return payload;
    }
}
