package uk.gov.hmcts.reform.sscs.service;

import java.time.ZonedDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OutOfHoursCalculator {
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
        int currentHour = now.getHour();

        return currentHour < startTime || currentHour >= endTime;
    }

    public ZonedDateTime getStartOfNextInHoursPeriod() {
        ZonedDateTime now = dateTimeProvider.now();
        ZonedDateTime startDay = (now.getHour() >= startTime) ? now.plusDays(1) : now;

        return startDay.withHour(startTime).withMinute(0).withSecond(0).withNano(0);
    }
}
