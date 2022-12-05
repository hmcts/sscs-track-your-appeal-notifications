package uk.gov.hmcts.reform.sscs.service.scheduler;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.model.jobs.JobPayloadSerializer;

@Component
public class CcdActionSerializer implements JobPayloadSerializer<String> {

    @Override
    public String serialize(String payload) {
        return payload;
    }
}
