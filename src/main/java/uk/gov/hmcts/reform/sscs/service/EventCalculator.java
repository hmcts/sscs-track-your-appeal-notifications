package uk.gov.hmcts.reform.sscs.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;

@Service
public class EventCalculator {
    private static final ZoneId UK_TIME_ZONE = ZoneId.of("Europe/London");
    private final DateTimeProvider dateTimeProvider;

    public EventCalculator(@Autowired DateTimeProvider dateTimeProvider) {
        this.dateTimeProvider = dateTimeProvider;
    }

    public ZonedDateTime getEventStart(NotificationEventType eventType) {

        return dateTimeProvider.now().withZoneSameInstant(UK_TIME_ZONE);
    }
}
