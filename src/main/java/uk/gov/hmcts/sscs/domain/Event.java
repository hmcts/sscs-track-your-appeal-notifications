package uk.gov.hmcts.sscs.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Event implements Comparable<Event> {
    uk.gov.hmcts.sscs.domain.notify.Event value;

    @Override
    public int compareTo(Event o) {
        return value.getDate().compareTo(o.getValue().getDate());
    }

}
