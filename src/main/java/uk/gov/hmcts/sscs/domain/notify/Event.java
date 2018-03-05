package uk.gov.hmcts.sscs.domain.notify;

import java.time.ZonedDateTime;

public class Event implements Comparable<Event> {

    private ZonedDateTime dateTime;
    private EventType eventType;

    public Event(ZonedDateTime date, EventType eventType) {
        this.dateTime = date;
        this.eventType = eventType;
    }

    public ZonedDateTime getDateTime() {
        return dateTime;
    }

    public EventType getEventType() {
        return eventType;
    }

    @Override
    public int compareTo(Event o) {
        return getDateTime().compareTo(o.getDateTime());
    }
}
