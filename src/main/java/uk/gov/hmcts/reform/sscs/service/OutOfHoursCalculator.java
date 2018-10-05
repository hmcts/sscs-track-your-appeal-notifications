package uk.gov.hmcts.reform.sscs.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OutOfHoursCalculator {
    private static final ZoneId UK_TIME_ZONE = ZoneId.of("Europe/London");
    private final DateTimeProvider dateTimeProvider;
    private final int startTime;
    private final int endTime;

    public OutOfHoursCalculator(
            @Autowired DateTimeProvider dateTimeProvider,
            @Value("${outOfHours.startHour}") int startHour,
            @Value("${outOfHours.endHour}") int endHour) {
        this.dateTimeProvider = dateTimeProvider;
        this.startTime = startHour;
        this.endTime = endHour;
    }

    public boolean isItOutOfHours() {
        ZonedDateTime now = dateTimeProvider.now();
        int currentHour = now.withZoneSameInstant(UK_TIME_ZONE).getHour();

        return currentHour < startTime || currentHour >= endTime;
    }

    public ZonedDateTime getStartOfNextInHoursPeriod() {
        ZonedDateTime now = dateTimeProvider.now();
        ZonedDateTime nowInUk = now.withZoneSameInstant(UK_TIME_ZONE);

        ZonedDateTime startDay = (nowInUk.getHour() >= startTime) ? nowInUk.plusDays(1) : nowInUk;

        return startDay.withHour(startTime).withMinute(0).withSecond(0).withNano(0).withZoneSameInstant(now.getZone());
    }
}
