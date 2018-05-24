package uk.gov.hmcts.sscs.service.scheduler;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobPayloadDeserializer;
import uk.gov.hmcts.sscs.domain.reminder.Action;

@Component
public class ActionDeserializer implements JobPayloadDeserializer<Action> {

    @Override
    public Action deserialize(String payload) {
        return null;
    }
}
