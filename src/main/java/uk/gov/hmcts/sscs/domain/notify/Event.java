package uk.gov.hmcts.sscs.domain.notify;

import java.time.ZonedDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Event implements Comparable<Event> {

    private ZonedDateTime dateTime;
    private EventType eventType;

    @Override
    public int compareTo(Event o) {
        return getDateTime().compareTo(o.getDateTime());
    }
}
