package uk.gov.hmcts.sscs.service.reminder;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.sscs.domain.notify.EventType.DWP_RESPONSE_RECEIVED;
import static uk.gov.hmcts.sscs.domain.notify.EventType.HEARING_BOOKED;

import org.junit.Test;

public class JobGroupGeneratorTest {

    private final JobGroupGenerator jobGroupGenerator = new JobGroupGenerator();

    @Test
    public void generatesJobGroup() {
        assertEquals("123_responseReceived", jobGroupGenerator.generate("123", DWP_RESPONSE_RECEIVED));
        assertEquals("123_hearingBooked", jobGroupGenerator.generate("123", HEARING_BOOKED));
    }

}
