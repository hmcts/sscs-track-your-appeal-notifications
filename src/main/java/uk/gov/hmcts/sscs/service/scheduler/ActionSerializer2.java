package uk.gov.hmcts.sscs.service.scheduler;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobPayloadSerializer;
import uk.gov.hmcts.sscs.domain.reminder.Action;

@Component
public class ActionSerializer2 implements JobPayloadSerializer<Action> {

    @Override
    public String serialize(Action payload) {
        return null;
    }
}
