package uk.gov.hmcts.reform.sscs.extractor;

import static org.junit.Assert.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DWP_RESPONSE_RECEIVED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_RECEIVED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DWP_RESPONSE_RECEIVED_NOTIFICATION;

import java.time.ZonedDateTime;
import java.util.Optional;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.SscsCaseDataUtils;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;

public class DwpResponseReceivedDateExtractorTest {

    private final DwpResponseReceivedDateExtractor dwpResponseReceivedDateExtractor =
        new DwpResponseReceivedDateExtractor();

    @Test
    public void extractsDwpResponseReceivedDate() {

        String ccdEventDate = "2018-01-01T14:01:18";
        ZonedDateTime expectedDwpResponseReceivedDate = ZonedDateTime.parse("2018-01-01T14:01:18Z[Europe/London]");

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapperWithEvent(
            DWP_RESPONSE_RECEIVED_NOTIFICATION,
            DWP_RESPONSE_RECEIVED,
            ccdEventDate
        );

        Optional<ZonedDateTime> dwpResponseReceivedDate = dwpResponseReceivedDateExtractor.extract(wrapper.getNewSscsCaseData());

        assertTrue(dwpResponseReceivedDate.isPresent());
        assertEquals(expectedDwpResponseReceivedDate, dwpResponseReceivedDate.get());
    }

    @Test
    public void returnsEmptyOptionalWhenDateNotPresent() {

        CcdNotificationWrapper ccdResponse = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(
            APPEAL_RECEIVED_NOTIFICATION
        );

        Optional<ZonedDateTime> dwpResponseReceivedDate = dwpResponseReceivedDateExtractor.extract(ccdResponse.getNewSscsCaseData());

        assertFalse(dwpResponseReceivedDate.isPresent());
    }

}
