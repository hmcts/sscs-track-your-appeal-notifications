package uk.gov.hmcts.sscs.domain;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.sscs.domain.notify.Event;
import uk.gov.hmcts.sscs.domain.notify.EventType;

@Data
@Builder(toBuilder = true)
public class CcdResponse {

    private String caseId;
    private Benefit benefitType;
    private String caseReference;
    private Subscription appellantSubscription;
    private Subscription supporterSubscription;
    private EventType notificationType;
    private List<Event> events;
    private List<Hearing> hearings;
    private List<Evidence> evidences;

}
