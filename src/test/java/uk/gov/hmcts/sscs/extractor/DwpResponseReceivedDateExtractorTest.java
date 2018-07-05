package uk.gov.hmcts.sscs.extractor;

import static org.junit.Assert.*;
import static uk.gov.hmcts.sscs.domain.notify.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.sscs.domain.notify.EventType.DWP_RESPONSE_RECEIVED;

import java.time.ZonedDateTime;
import java.util.Optional;
import org.junit.Test;
import uk.gov.hmcts.sscs.CcdResponseUtils;
import uk.gov.hmcts.sscs.domain.CcdResponse;

public class DwpResponseReceivedDateExtractorTest {

    private final DwpResponseReceivedDateExtractor dwpResponseReceivedDateExtractor =
        new DwpResponseReceivedDateExtractor();

    @Test
    public void extractsDwpResponseReceivedDate() {

        String ccdEventDate = "2018-01-01T14:01:18";
        ZonedDateTime expectedDwpResponseReceivedDate = ZonedDateTime.parse("2018-01-01T14:01:18Z[Europe/London]");

        CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponseWithEvent(
            DWP_RESPONSE_RECEIVED,
            DWP_RESPONSE_RECEIVED,
            ccdEventDate
        );

        Optional<ZonedDateTime> dwpResponseReceivedDate = dwpResponseReceivedDateExtractor.extract(ccdResponse);

        assertTrue(dwpResponseReceivedDate.isPresent());
        assertEquals(expectedDwpResponseReceivedDate, dwpResponseReceivedDate.get());
    }

    @Test
    public void returnsEmptyOptionalWhenDateNotPresent() {

        CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponse(
            APPEAL_RECEIVED
        );

        Optional<ZonedDateTime> dwpResponseReceivedDate = dwpResponseReceivedDateExtractor.extract(ccdResponse);

        assertFalse(dwpResponseReceivedDate.isPresent());
    }

}
