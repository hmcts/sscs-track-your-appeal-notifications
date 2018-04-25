package uk.gov.hmcts.sscs.domain.notify;

import static uk.gov.hmcts.sscs.config.AppConstants.ZONE_ID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.*;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Event {

    String date;
    String type;
    String description;

    @JsonIgnore
    public ZonedDateTime getDateTime() {
        return ZonedDateTime.parse(date + "Z").toInstant().atZone(ZoneId.of(ZONE_ID));
    }

    @JsonIgnore
    public EventType getEventType() {
        return EventType.getNotificationById(type);
    }

}
