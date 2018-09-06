package uk.gov.hmcts.reform.sscs.extractor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.SscsCaseDataUtils;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;

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

        List<NotificationEventType> eventsUsingInitialDelay = Arrays.asList(
            DWP_RESPONSE_RECEIVED_NOTIFICATION,
            POSTPONEMENT_NOTIFICATION
        );

        for (NotificationEventType eventUsingInitialDelay : eventsUsingInitialDelay) {

            ZonedDateTime expectedHearingContactDate = ZonedDateTime.parse("2018-01-01T14:02:18Z[Europe/London]");

            CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(eventUsingInitialDelay);

            when(dwpResponseReceivedDateExtractor.extract(wrapper.getNewSscsCaseData())).thenReturn(Optional.of(dwpResponseReceivedDate));

            Optional<ZonedDateTime> hearingContactDate = hearingContactDateExtractor.extract(wrapper.getSscsCaseDataWrapper());

            assertTrue(hearingContactDate.isPresent());
            assertEquals(expectedHearingContactDate, hearingContactDate.get());
        }
    }

    @Test
    public void extractsSecondHearingContactDate() {

        ZonedDateTime expectedHearingContactDate = ZonedDateTime.parse("2018-01-01T15:02:18Z[Europe/London]");

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(FIRST_HEARING_HOLDING_REMINDER_NOTIFICATION);

        when(dwpResponseReceivedDateExtractor.extract(wrapper.getNewSscsCaseData())).thenReturn(Optional.of(dwpResponseReceivedDate));

        Optional<ZonedDateTime> hearingContactDate = hearingContactDateExtractor.extract(wrapper.getSscsCaseDataWrapper());

        assertTrue(hearingContactDate.isPresent());
        assertEquals(expectedHearingContactDate, hearingContactDate.get());
    }

    @Test
    public void extractsThirdHearingContactDate() {

        ZonedDateTime expectedHearingContactDate = ZonedDateTime.parse("2018-01-01T16:02:18Z[Europe/London]");

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(SECOND_HEARING_HOLDING_REMINDER_NOTIFICATION);

        when(dwpResponseReceivedDateExtractor.extract(wrapper.getNewSscsCaseData())).thenReturn(Optional.of(dwpResponseReceivedDate));

        Optional<ZonedDateTime> hearingContactDate = hearingContactDateExtractor.extract(wrapper.getSscsCaseDataWrapper());

        assertTrue(hearingContactDate.isPresent());
        assertEquals(expectedHearingContactDate, hearingContactDate.get());
    }

    @Test
    public void extractsFinalHearingContactDate() {

        ZonedDateTime expectedHearingContactDate = ZonedDateTime.parse("2018-01-01T17:02:18Z[Europe/London]");

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(THIRD_HEARING_HOLDING_REMINDER_NOTIFICATION);

        when(dwpResponseReceivedDateExtractor.extract(wrapper.getNewSscsCaseData())).thenReturn(Optional.of(dwpResponseReceivedDate));

        Optional<ZonedDateTime> hearingContactDate = hearingContactDateExtractor.extract(wrapper.getSscsCaseDataWrapper());

        assertTrue(hearingContactDate.isPresent());
        assertEquals(expectedHearingContactDate, hearingContactDate.get());
    }

    @Test
    public void givenDwpResponseReceivedEvent_thenExtractDateForReferenceEvent() {

        ZonedDateTime expectedHearingContactDate = ZonedDateTime.parse("2018-01-01T16:02:18Z[Europe/London]");

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(DWP_RESPONSE_RECEIVED_NOTIFICATION);

        when(dwpResponseReceivedDateExtractor.extract(wrapper.getNewSscsCaseData())).thenReturn(Optional.of(dwpResponseReceivedDate));

        Optional<ZonedDateTime> hearingContactDate =
            hearingContactDateExtractor.extractForReferenceEvent(
                wrapper.getNewSscsCaseData(),
                SECOND_HEARING_HOLDING_REMINDER_NOTIFICATION
            );

        assertTrue(hearingContactDate.isPresent());
        assertEquals(expectedHearingContactDate, hearingContactDate.get());
    }

    @Test
    public void givenAdjournedEvent_thenExtractDateForReferenceEvent() {

        ZonedDateTime expectedHearingContactDate = ZonedDateTime.parse("2018-01-01T14:02:18Z[Europe/London]");

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(ADJOURNED_NOTIFICATION);

        when(dwpResponseReceivedDateExtractor.extract(wrapper.getNewSscsCaseData())).thenReturn(Optional.of(dwpResponseReceivedDate));

        Optional<ZonedDateTime> hearingContactDate =
                hearingContactDateExtractor.extractForReferenceEvent(
                        wrapper.getNewSscsCaseData(),
                        ADJOURNED_NOTIFICATION
                );

        assertTrue(hearingContactDate.isPresent());
        assertEquals(expectedHearingContactDate, hearingContactDate.get());
    }

    @Test
    public void returnsEmptyOptionalWhenDwpResponseReceivedDateIsNotPresent() {

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(DWP_RESPONSE_RECEIVED_NOTIFICATION);

        when(dwpResponseReceivedDateExtractor.extract(wrapper.getNewSscsCaseData())).thenReturn(Optional.empty());

        Optional<ZonedDateTime> dwpResponseReceivedDate = hearingContactDateExtractor.extract(wrapper.getSscsCaseDataWrapper());

        assertFalse(dwpResponseReceivedDate.isPresent());
    }

    @Test
    public void returnsEmptyOptionalWhenNotificationEventTypeNotAcceptable() {

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(APPEAL_RECEIVED_NOTIFICATION);

        Optional<ZonedDateTime> dwpResponseReceivedDate = hearingContactDateExtractor.extract(wrapper.getSscsCaseDataWrapper());

        assertFalse(dwpResponseReceivedDate.isPresent());

        verify(dwpResponseReceivedDateExtractor, never()).extract(wrapper.getNewSscsCaseData());
    }

}
