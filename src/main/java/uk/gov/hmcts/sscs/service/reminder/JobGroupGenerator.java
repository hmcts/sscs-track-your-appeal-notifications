package uk.gov.hmcts.sscs.service.reminder;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.sscs.domain.notify.EventType;

@Component
public class JobGroupGenerator {

    public String generate(
        String caseId,
        EventType eventType
    ) {
        return caseId + "_" + eventType.getId();
    }
}
