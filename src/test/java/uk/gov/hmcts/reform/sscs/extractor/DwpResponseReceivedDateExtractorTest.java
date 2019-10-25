package uk.gov.hmcts.reform.sscs.extractor;

import static org.junit.Assert.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DWP_RESPOND;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_RECEIVED_NOTIFICATION;

import java.time.ZonedDateTime;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.SscsCaseDataUtils;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;

@RunWith(JUnitParamsRunner.class)
public class DwpResponseReceivedDateExtractorTest {

    private final DwpResponseReceivedDateExtractor dwpResponseReceivedDateExtractor =
        new DwpResponseReceivedDateExtractor();

    @Test
    @Parameters({"DWP_RESPONSE_RECEIVED_NOTIFICATION", "DWP_UPLOAD_RESPONSE_NOTIFICATION"})
    public void extractsDwpResponseReceivedDate(NotificationEventType eventType) {

        String ccdEventDate = "2018-01-01T14:01:18";
        ZonedDateTime expectedDwpResponseReceivedDate = ZonedDateTime.parse("2018-01-01T14:01:18Z[Europe/London]");

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapperWithEvent(
            eventType,
            DWP_RESPOND,
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

    @Test
    public void returnsDwpResponseDateWhenThereAreNoEvents() {
        ZonedDateTime expectedDwpResponseReceivedDate = ZonedDateTime.parse("2018-01-25T00:00:00Z[Europe/London]");

        CcdNotificationWrapper ccdResponse = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(
                APPEAL_RECEIVED_NOTIFICATION
        );
        ccdResponse.getSscsCaseDataWrapper().getNewSscsCaseData().setDwpResponseDate("2018-01-25");

        Optional<ZonedDateTime> dwpResponseReceivedDate = dwpResponseReceivedDateExtractor.extract(ccdResponse.getNewSscsCaseData());

        assertTrue(dwpResponseReceivedDate.isPresent());
        assertEquals(expectedDwpResponseReceivedDate, dwpResponseReceivedDate.get());
    }

}
