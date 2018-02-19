package uk.gov.hmcts.sscs.domain.notify;

import java.util.Date;

public class Event implements Comparable<Event> {

    private Date date;
    private EventType eventType;

    public Event(Date date, EventType eventType) {
        this.date = date;
        this.eventType = eventType;
    }

    public Date getDate() {
        return date;
    }

    public EventType getEventType() {
        return eventType;
    }

    @Override
    public int compareTo(Event o) {
        return getDate().compareTo(o.getDate());
    }
}
