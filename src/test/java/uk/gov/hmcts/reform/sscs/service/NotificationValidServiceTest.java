package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.SscsCaseDataUtils;

public class NotificationValidServiceTest {

    private NotificationValidService notificationValidService;
    private SscsCaseData ccdResponse;

    @Before
    public void setup() {
        notificationValidService = new NotificationValidService();
        ccdResponse = SscsCaseData.builder().build();
    }

    @Test
    public void givenHearingIsInFutureAndEventIsHearingBooked_thenNotificationIsStillValidToSend() {
        assertTrue(
            notificationValidService.isNotificationStillValidToSend(SscsCaseDataUtils.addHearing(ccdResponse, 1), EventType.HEARING_BOOKED)
        );
    }

    @Test
    public void givenHearingIsInFutureAndEventIsHearingReminder_thenNotificationIsStillValidToSend() {
        assertTrue(
            notificationValidService.isNotificationStillValidToSend(SscsCaseDataUtils.addHearing(ccdResponse, 1), EventType.HEARING_REMINDER)
        );
    }

    @Test
    public void givenHearingIsInPastAndEventIsHearingBooked_thenNotificationIsNotValidToSend() {
        assertFalse(
            notificationValidService.isNotificationStillValidToSend(SscsCaseDataUtils.addHearing(ccdResponse, -1), EventType.HEARING_BOOKED)
        );
    }

    @Test
    public void givenHearingIsInPastAndEventIsHearingReminder_thenNotificationIsNotValidToSend() {
        assertFalse(
            notificationValidService.isNotificationStillValidToSend(SscsCaseDataUtils.addHearing(ccdResponse, -1), EventType.HEARING_REMINDER)
        );
    }

    @Test
    public void givenCaseDoesNotContainHearingAndEventIsHearingBooked_thenNotificationIsNotValidToSend() {
        assertFalse(
            notificationValidService.isNotificationStillValidToSend(null, EventType.HEARING_BOOKED)
        );
    }

}