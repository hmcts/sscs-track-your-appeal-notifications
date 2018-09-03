package uk.gov.hmcts.reform.sscs.extractor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.SscsCaseDataUtils;

@RunWith(MockitoJUnitRunner.class)
public class HearingContactDateExtractorTest {

    @Mock
    private DwpResponseReceivedDateExtractor dwpResponseReceivedDateExtractor;

    private final ZonedDateTime dwpResponseReceivedDate =
        ZonedDateTime.parse("2018-01-01T14:01:18Z[Europe/London]");

    private HearingContactDateExtractor hearingContactDateExtractor;

    @Before
    public void setup() {

        hearingContactDateExtractor = new HearingContactDateExtractor(
            dwpResponseReceivedDateExtractor,
            60, 3600
        );
    }

    @Test
    public void extractsFirstHearingContactDate() {

        List<EventType> eventsUsingInitialDelay = Arrays.asList(
            DWP_RESPONSE_RECEIVED,
            POSTPONEMENT
        );

        for (EventType eventUsingInitialDelay : eventsUsingInitialDelay) {

            ZonedDateTime expectedHearingContactDate = ZonedDateTime.parse("2018-01-01T14:02:18Z[Europe/London]");

            SscsCaseData ccdResponse = SscsCaseDataUtils.buildBasicSscsCaseData(eventUsingInitialDelay);

            when(dwpResponseReceivedDateExtractor.extract(ccdResponse)).thenReturn(Optional.of(dwpResponseReceivedDate));

            Optional<ZonedDateTime> hearingContactDate = hearingContactDateExtractor.extract(ccdResponse);

            assertTrue(hearingContactDate.isPresent());
            assertEquals(expectedHearingContactDate, hearingContactDate.get());
        }
    }

    @Test
    public void extractsSecondHearingContactDate() {

        ZonedDateTime expectedHearingContactDate = ZonedDateTime.parse("2018-01-01T15:02:18Z[Europe/London]");

        SscsCaseData ccdResponse = SscsCaseDataUtils.buildBasicSscsCaseData(FIRST_HEARING_HOLDING_REMINDER);

        when(dwpResponseReceivedDateExtractor.extract(ccdResponse)).thenReturn(Optional.of(dwpResponseReceivedDate));

        Optional<ZonedDateTime> hearingContactDate = hearingContactDateExtractor.extract(ccdResponse);

        assertTrue(hearingContactDate.isPresent());
        assertEquals(expectedHearingContactDate, hearingContactDate.get());
    }

    @Test
    public void extractsThirdHearingContactDate() {

        ZonedDateTime expectedHearingContactDate = ZonedDateTime.parse("2018-01-01T16:02:18Z[Europe/London]");

        SscsCaseData ccdResponse = SscsCaseDataUtils.buildBasicSscsCaseData(SECOND_HEARING_HOLDING_REMINDER);

        when(dwpResponseReceivedDateExtractor.extract(ccdResponse)).thenReturn(Optional.of(dwpResponseReceivedDate));

        Optional<ZonedDateTime> hearingContactDate = hearingContactDateExtractor.extract(ccdResponse);

        assertTrue(hearingContactDate.isPresent());
        assertEquals(expectedHearingContactDate, hearingContactDate.get());
    }

    @Test
    public void extractsFinalHearingContactDate() {

        ZonedDateTime expectedHearingContactDate = ZonedDateTime.parse("2018-01-01T17:02:18Z[Europe/London]");

        SscsCaseData ccdResponse = SscsCaseDataUtils.buildBasicSscsCaseData(THIRD_HEARING_HOLDING_REMINDER);

        when(dwpResponseReceivedDateExtractor.extract(ccdResponse)).thenReturn(Optional.of(dwpResponseReceivedDate));

        Optional<ZonedDateTime> hearingContactDate = hearingContactDateExtractor.extract(ccdResponse);

        assertTrue(hearingContactDate.isPresent());
        assertEquals(expectedHearingContactDate, hearingContactDate.get());
    }

    @Test
    public void givenDwpResponseReceivedEvent_thenExtractDateForReferenceEvent() {

        ZonedDateTime expectedHearingContactDate = ZonedDateTime.parse("2018-01-01T16:02:18Z[Europe/London]");

        SscsCaseData ccdResponse = SscsCaseDataUtils.buildBasicSscsCaseData(DWP_RESPONSE_RECEIVED);

        when(dwpResponseReceivedDateExtractor.extract(ccdResponse)).thenReturn(Optional.of(dwpResponseReceivedDate));

        Optional<ZonedDateTime> hearingContactDate =
            hearingContactDateExtractor.extractForReferenceEvent(
                ccdResponse,
                SECOND_HEARING_HOLDING_REMINDER
            );

        assertTrue(hearingContactDate.isPresent());
        assertEquals(expectedHearingContactDate, hearingContactDate.get());
    }

    @Test
    public void givenAdjournedEvent_thenExtractDateForReferenceEvent() {

        ZonedDateTime expectedHearingContactDate = ZonedDateTime.parse("2018-01-01T14:02:18Z[Europe/London]");

        SscsCaseData ccdResponse = SscsCaseDataUtils.buildBasicSscsCaseData(ADJOURNED);

        when(dwpResponseReceivedDateExtractor.extract(ccdResponse)).thenReturn(Optional.of(dwpResponseReceivedDate));

        Optional<ZonedDateTime> hearingContactDate =
                hearingContactDateExtractor.extractForReferenceEvent(
                        ccdResponse,
                        ADJOURNED
                );

        assertTrue(hearingContactDate.isPresent());
        assertEquals(expectedHearingContactDate, hearingContactDate.get());
    }

    @Test
    public void returnsEmptyOptionalWhenDwpResponseReceivedDateIsNotPresent() {

        SscsCaseData ccdResponse = SscsCaseDataUtils.buildBasicSscsCaseData(DWP_RESPONSE_RECEIVED);

        when(dwpResponseReceivedDateExtractor.extract(ccdResponse)).thenReturn(Optional.empty());

        Optional<ZonedDateTime> dwpResponseReceivedDate = hearingContactDateExtractor.extract(ccdResponse);

        assertFalse(dwpResponseReceivedDate.isPresent());
    }

    @Test
    public void returnsEmptyOptionalWhenEventTypeNotAcceptable() {

        SscsCaseData ccdResponse = SscsCaseDataUtils.buildBasicSscsCaseData(APPEAL_RECEIVED);

        Optional<ZonedDateTime> dwpResponseReceivedDate = hearingContactDateExtractor.extract(ccdResponse);

        assertFalse(dwpResponseReceivedDate.isPresent());

        verify(dwpResponseReceivedDateExtractor, never()).extract(ccdResponse);
    }

}
