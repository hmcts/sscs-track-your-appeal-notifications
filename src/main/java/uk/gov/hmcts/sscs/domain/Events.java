package uk.gov.hmcts.sscs.domain;

import lombok.Builder;
import lombok.Value;
import uk.gov.hmcts.sscs.domain.notify.Event;

@Value
@Builder
public class Events implements Comparable<Events> {
    Event value;

    @Override
    public int compareTo(Events o) {
        return value.getDate().compareTo(o.getValue().getDate());
    }

}
