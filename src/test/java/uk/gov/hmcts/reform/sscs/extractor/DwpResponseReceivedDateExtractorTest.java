package uk.gov.hmcts.reform.sscs.extractor;

import static org.junit.Assert.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DWP_RESPONSE_RECEIVED;

import java.time.ZonedDateTime;
import java.util.Optional;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.SscsCaseDataUtils;

public class DwpResponseReceivedDateExtractorTest {

    private final DwpResponseReceivedDateExtractor dwpResponseReceivedDateExtractor =
        new DwpResponseReceivedDateExtractor();

    @Test
    public void extractsDwpResponseReceivedDate() {

        String ccdEventDate = "2018-01-01T14:01:18";
        ZonedDateTime expectedDwpResponseReceivedDate = ZonedDateTime.parse("2018-01-01T14:01:18Z[Europe/London]");

        SscsCaseData ccdResponse = SscsCaseDataUtils.buildBasicSscsCaseDataWithEvent(
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

        SscsCaseData ccdResponse = SscsCaseDataUtils.buildBasicSscsCaseData(
            APPEAL_RECEIVED
        );

        Optional<ZonedDateTime> dwpResponseReceivedDate = dwpResponseReceivedDateExtractor.extract(ccdResponse);

        assertFalse(dwpResponseReceivedDate.isPresent());
    }

}
