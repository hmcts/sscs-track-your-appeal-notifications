package uk.gov.hmcts.sscs.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.sscs.CcdResponseUtils;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.notify.EventType;

public class NotificationValidServiceTest {

    private NotificationValidService notificationValidService;
    private CcdResponse ccdResponse;

    @Before
    public void setup() {
        notificationValidService = new NotificationValidService();
        ccdResponse = CcdResponse.builder().build();
    }

    @Test
    public void givenHearingIsInFutureAndEventIsHearingBooked_thenNotificationIsStillValidToSend() {
        assertTrue(
            notificationValidService.isNotificationStillValidToSend(CcdResponseUtils.addHearing(ccdResponse, 1), EventType.HEARING_BOOKED)
        );
    }

    @Test
    public void givenHearingIsInFutureAndEventIsHearingReminder_thenNotificationIsStillValidToSend() {
        assertTrue(
            notificationValidService.isNotificationStillValidToSend(CcdResponseUtils.addHearing(ccdResponse, 1), EventType.HEARING_REMINDER)
        );
    }

    @Test
    public void givenHearingIsInPastAndEventIsHearingBooked_thenNotificationIsNotValidToSend() {
        assertFalse(
            notificationValidService.isNotificationStillValidToSend(CcdResponseUtils.addHearing(ccdResponse, -1), EventType.HEARING_BOOKED)
        );
    }

    @Test
    public void givenHearingIsInPastAndEventIsHearingReminder_thenNotificationIsNotValidToSend() {
        assertFalse(
            notificationValidService.isNotificationStillValidToSend(CcdResponseUtils.addHearing(ccdResponse, -1), EventType.HEARING_REMINDER)
        );
    }

    @Test
    public void givenCaseDoesNotContainHearingAndEventIsHearingBooked_thenNotificationIsNotValidToSend() {
        assertFalse(
            notificationValidService.isNotificationStillValidToSend(null, EventType.HEARING_BOOKED)
        );
    }

}