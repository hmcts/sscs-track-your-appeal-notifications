package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.SscsCaseDataUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

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
            notificationValidService.isNotificationStillValidToSend(SscsCaseDataUtils.addHearing(ccdResponse, 1), HEARING_BOOKED_NOTIFICATION)
        );
    }

    @Test
    public void givenHearingIsInFutureAndEventIsHearingReminder_thenNotificationIsStillValidToSend() {
        assertTrue(
            notificationValidService.isNotificationStillValidToSend(SscsCaseDataUtils.addHearing(ccdResponse, 1), HEARING_REMINDER_NOTIFICATION)
        );
    }

    @Test
    public void givenHearingIsInPastAndEventIsHearingBooked_thenNotificationIsNotValidToSend() {
        assertFalse(
            notificationValidService.isNotificationStillValidToSend(SscsCaseDataUtils.addHearing(ccdResponse, -1), HEARING_BOOKED_NOTIFICATION)
        );
    }

    @Test
    public void givenHearingIsInPastAndEventIsHearingReminder_thenNotificationIsNotValidToSend() {
        assertFalse(
            notificationValidService.isNotificationStillValidToSend(SscsCaseDataUtils.addHearing(ccdResponse, -1), HEARING_REMINDER_NOTIFICATION)
        );
    }

    @Test
    public void givenCaseDoesNotContainHearingAndEventIsHearingBooked_thenNotificationIsNotValidToSend() {
        assertFalse(
            notificationValidService.isNotificationStillValidToSend(null, HEARING_BOOKED_NOTIFICATION)
        );
    }

    @Test
    public void givenCaseIsOralCaseAndNotificationTypeIsSentForOral_thenReturnTrue() {
        assertTrue(
            notificationValidService.isHearingTypeValidToSendNotification(true, APPEAL_RECEIVED_NOTIFICATION)
        );
    }

    @Test
    public void givenCaseIsOralCaseAndNotificationTypeIsNotSentForOral_thenReturnFalse() {
        assertFalse(
            notificationValidService.isHearingTypeValidToSendNotification(true, DO_NOT_SEND)
        );
    }

    @Test
    public void givenCaseIsPaperCaseAndNotificationTypeIsSentForPaper_thenReturnTrue() {
        assertTrue(
            notificationValidService.isHearingTypeValidToSendNotification(false, APPEAL_RECEIVED_NOTIFICATION)
        );
    }

    @Test
    public void givenCaseIsPaperCaseAndNotificationTypeIsNotSentForPaper_thenReturnFalse() {
        assertFalse(
            notificationValidService.isHearingTypeValidToSendNotification(false, APPEAL_LAPSED_NOTIFICATION)
        );
    }

}