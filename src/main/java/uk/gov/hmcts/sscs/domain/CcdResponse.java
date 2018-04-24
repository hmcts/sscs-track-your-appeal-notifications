package uk.gov.hmcts.sscs.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.sscs.domain.notify.Event;
import uk.gov.hmcts.sscs.domain.notify.EventType;

@Data
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CcdResponse {

    private String caseId;
    private Benefit benefitType;
    private String caseReference;
    private Subscriptions subscriptions;
    private EventType notificationType;
    private List<Event> events;
    private List<Hearing> hearings;
    private List<Evidence> evidences;

}
