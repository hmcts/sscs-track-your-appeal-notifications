package uk.gov.hmcts.sscs.domain.reminder;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.sscs.config.AppConstants.ZONE_ID;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.Test;

public class TriggerTest {

    @Test
    public void convertZonedDateTimeToFormattedString() {
        ZonedDateTime dateTime = ZonedDateTime.of(LocalDate.of(2018, 4, 1), LocalTime.of(0, 0), ZoneId.of(ZONE_ID));

        Trigger trigger = new Trigger(dateTime);

        assertEquals("2018-04-01T00:00:00+01:00", trigger.getStart_date_time());
    }
}
