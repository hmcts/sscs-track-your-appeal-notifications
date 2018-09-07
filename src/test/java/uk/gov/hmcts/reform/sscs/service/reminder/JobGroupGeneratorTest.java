package uk.gov.hmcts.reform.sscs.service.reminder;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DWP_RESPONSE_RECEIVED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.HEARING_BOOKED_NOTIFICATION;

import org.junit.Test;

public class JobGroupGeneratorTest {

    private final JobGroupGenerator jobGroupGenerator = new JobGroupGenerator();

    @Test
    public void generatesJobGroup() {
        assertEquals("123_responseReceived", jobGroupGenerator.generate("123", DWP_RESPONSE_RECEIVED_NOTIFICATION.getId()));
        assertEquals("123_hearingBooked", jobGroupGenerator.generate("123", HEARING_BOOKED_NOTIFICATION.getId()));
    }

}
