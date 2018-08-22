package uk.gov.hmcts.sscs.extractor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.sscs.domain.notify.EventType.*;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.sscs.CcdResponseUtils;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.notify.EventType;

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

            CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponse(eventUsingInitialDelay);

            when(dwpResponseReceivedDateExtractor.extract(ccdResponse)).thenReturn(Optional.of(dwpResponseReceivedDate));

            Optional<ZonedDateTime> hearingContactDate = hearingContactDateExtractor.extract(ccdResponse);

            assertTrue(hearingContactDate.isPresent());
            assertEquals(expectedHearingContactDate, hearingContactDate.get());
        }
    }

    @Test
    public void extractsSecondHearingContactDate() {

        ZonedDateTime expectedHearingContactDate = ZonedDateTime.parse("2018-01-01T15:02:18Z[Europe/London]");

        CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponse(FIRST_HEARING_HOLDING_REMINDER);

        when(dwpResponseReceivedDateExtractor.extract(ccdResponse)).thenReturn(Optional.of(dwpResponseReceivedDate));

        Optional<ZonedDateTime> hearingContactDate = hearingContactDateExtractor.extract(ccdResponse);

        assertTrue(hearingContactDate.isPresent());
        assertEquals(expectedHearingContactDate, hearingContactDate.get());
    }

    @Test
    public void extractsThirdHearingContactDate() {

        ZonedDateTime expectedHearingContactDate = ZonedDateTime.parse("2018-01-01T16:02:18Z[Europe/London]");

        CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponse(SECOND_HEARING_HOLDING_REMINDER);

        when(dwpResponseReceivedDateExtractor.extract(ccdResponse)).thenReturn(Optional.of(dwpResponseReceivedDate));

        Optional<ZonedDateTime> hearingContactDate = hearingContactDateExtractor.extract(ccdResponse);

        assertTrue(hearingContactDate.isPresent());
        assertEquals(expectedHearingContactDate, hearingContactDate.get());
    }

    @Test
    public void extractsFinalHearingContactDate() {

        ZonedDateTime expectedHearingContactDate = ZonedDateTime.parse("2018-01-01T17:02:18Z[Europe/London]");

        CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponse(THIRD_HEARING_HOLDING_REMINDER);

        when(dwpResponseReceivedDateExtractor.extract(ccdResponse)).thenReturn(Optional.of(dwpResponseReceivedDate));

        Optional<ZonedDateTime> hearingContactDate = hearingContactDateExtractor.extract(ccdResponse);

        assertTrue(hearingContactDate.isPresent());
        assertEquals(expectedHearingContactDate, hearingContactDate.get());
    }

    @Test
    public void givenDwpResponseReceivedEvent_thenExtractDateForReferenceEvent() {

        ZonedDateTime expectedHearingContactDate = ZonedDateTime.parse("2018-01-01T16:02:18Z[Europe/London]");

        CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponse(DWP_RESPONSE_RECEIVED);

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

        CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponse(ADJOURNED);

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

        CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponse(DWP_RESPONSE_RECEIVED);

        when(dwpResponseReceivedDateExtractor.extract(ccdResponse)).thenReturn(Optional.empty());

        Optional<ZonedDateTime> dwpResponseReceivedDate = hearingContactDateExtractor.extract(ccdResponse);

        assertFalse(dwpResponseReceivedDate.isPresent());
    }

    @Test
    public void returnsEmptyOptionalWhenEventTypeNotAcceptable() {

        CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponse(APPEAL_RECEIVED);

        Optional<ZonedDateTime> dwpResponseReceivedDate = hearingContactDateExtractor.extract(ccdResponse);

        assertFalse(dwpResponseReceivedDate.isPresent());

        verify(dwpResponseReceivedDateExtractor, never()).extract(ccdResponse);
    }

}
