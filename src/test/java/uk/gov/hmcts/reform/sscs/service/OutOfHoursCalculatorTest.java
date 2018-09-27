package uk.gov.hmcts.reform.sscs.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.Test;

public class OutOfHoursCalculatorTest {

    public static final int START_HOUR = 9;
    public static final int END_HOUR = 17;

    @Test
    public void isNotOutOfHours() {
        ZonedDateTime now = nowAtHour(12);
        boolean isOutOfHours = new OutOfHoursCalculator(new FixedDateTimeProvider(now), START_HOUR, END_HOUR).isItOutOfHours();

        assertThat(isOutOfHours, is(false));
    }

    @Test
    public void isNotOutOfHoursAtStartTime() {
        ZonedDateTime now = nowAtHour(9);
        boolean isOutOfHours = new OutOfHoursCalculator(new FixedDateTimeProvider(now), START_HOUR, END_HOUR).isItOutOfHours();

        assertThat(isOutOfHours, is(false));
    }

    @Test
    public void isOutOfHours() {
        ZonedDateTime now = nowAtHour(20);
        boolean isOutOfHours = new OutOfHoursCalculator(new FixedDateTimeProvider(now), START_HOUR, END_HOUR).isItOutOfHours();

        assertThat(isOutOfHours, is(true));
    }

    @Test
    public void isOutOfHoursAtEndTime() {
        ZonedDateTime now = nowAtHour(17);
        boolean isOutOfHours = new OutOfHoursCalculator(new FixedDateTimeProvider(now), START_HOUR, END_HOUR).isItOutOfHours();

        assertThat(isOutOfHours, is(true));
    }

    @Test
    public void getStartOfNextInHoursPeriodWhenItIsTheNextDay() {
        ZonedDateTime now = nowAtHour(END_HOUR);
        ZonedDateTime nextInHoursTime = new OutOfHoursCalculator(new FixedDateTimeProvider(now), START_HOUR, END_HOUR).getStartOfNextInHoursPeriod();

        assertThat(nextInHoursTime, is(ZonedDateTime.of(2018, 9, 19, START_HOUR, 0, 0, 0, ZoneId.systemDefault())));
    }

    @Test
    public void getStartOfNextInHoursPeriodWhenItIsTheSameDay() {
        ZonedDateTime now = nowAtHour(1);
        ZonedDateTime nextInHoursTime = new OutOfHoursCalculator(new FixedDateTimeProvider(now), START_HOUR, END_HOUR).getStartOfNextInHoursPeriod();

        assertThat(nextInHoursTime, is(ZonedDateTime.of(2018, 9, 18, START_HOUR, 0, 0, 0, ZoneId.systemDefault())));
    }

    private ZonedDateTime nowAtHour(int hour) {
        return ZonedDateTime.of(2018, 9, 18, hour, 0, 0, 0, ZoneId.systemDefault());
    }

    public static class FixedDateTimeProvider extends DateTimeProvider {

        private final ZonedDateTime now;

        public FixedDateTimeProvider(ZonedDateTime now) {
            this.now = now;
        }

        @Override
        public ZonedDateTime now() {
            return now;
        }
    }
}