package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.SscsCaseDataUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.OnlinePanel;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public class NotificationValidServiceTest {

    private NotificationValidService notificationValidService;
    private SscsCaseData sscsCaseData;

    @Before
    public void setup() {
        notificationValidService = new NotificationValidService();
        sscsCaseData = SscsCaseData.builder().build();
    }

    @Test
    public void givenHearingIsInFutureAndEventIsHearingBooked_thenNotificationIsStillValidToSend() {
        assertTrue(
            notificationValidService.isNotificationStillValidToSend(SscsCaseDataUtils.addHearing(sscsCaseData, 1), HEARING_BOOKED_NOTIFICATION)
        );
    }

    @Test
    public void givenHearingIsInFutureAndEventIsHearingReminder_thenNotificationIsStillValidToSend() {
        assertTrue(
            notificationValidService.isNotificationStillValidToSend(SscsCaseDataUtils.addHearing(sscsCaseData, 1), HEARING_REMINDER_NOTIFICATION)
        );
    }

    @Test
    public void givenHearingIsInPastAndEventIsHearingBooked_thenNotificationIsNotValidToSend() {
        assertFalse(
            notificationValidService.isNotificationStillValidToSend(SscsCaseDataUtils.addHearing(sscsCaseData, -1), HEARING_BOOKED_NOTIFICATION)
        );
    }

    @Test
    public void givenHearingIsInPastAndEventIsHearingReminder_thenNotificationIsNotValidToSend() {
        assertFalse(
            notificationValidService.isNotificationStillValidToSend(SscsCaseDataUtils.addHearing(sscsCaseData, -1), HEARING_REMINDER_NOTIFICATION)
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
        SscsCaseData caseData = SscsCaseData.builder().appeal(Appeal.builder().hearingOptions(HearingOptions.builder().wantsToAttend("Yes").build()).build()).build();

        assertTrue(
            notificationValidService.isHearingTypeValidToSendNotification(caseData, APPEAL_RECEIVED_NOTIFICATION)
        );
    }

    @Test
    public void givenCaseIsOralCaseAndNotificationTypeIsNotSentForOral_thenReturnFalse() {
        SscsCaseData caseData = SscsCaseData.builder().appeal(Appeal.builder().hearingOptions(HearingOptions.builder().wantsToAttend("Yes").build()).build()).build();

        assertFalse(
            notificationValidService.isHearingTypeValidToSendNotification(caseData, DO_NOT_SEND)
        );
    }

    @Test
    public void givenCaseIsPaperCaseAndNotificationTypeIsSentForPaper_thenReturnTrue() {
        SscsCaseData caseData = SscsCaseData.builder().appeal(Appeal.builder().hearingOptions(HearingOptions.builder().wantsToAttend("No").build()).build()).build();

        assertTrue(
            notificationValidService.isHearingTypeValidToSendNotification(caseData, APPEAL_RECEIVED_NOTIFICATION)
        );
    }

    @Test
    public void givenCaseIsPaperCaseAndNotificationTypeIsNotSentForPaper_thenReturnFalse() {
        SscsCaseData caseData = SscsCaseData.builder().appeal(Appeal.builder().hearingOptions(HearingOptions.builder().wantsToAttend("No").build()).build()).build();

        assertFalse(
            notificationValidService.isHearingTypeValidToSendNotification(caseData, HEARING_BOOKED_NOTIFICATION)
        );
    }

    @Test
    public void givenCaseIsCohCaseAndNotificationTypeIsSentForCoh_thenReturnTrue() {
        SscsCaseData caseData = SscsCaseData.builder().onlinePanel(OnlinePanel.builder().build()).appeal(Appeal.builder().hearingOptions(HearingOptions.builder().wantsToAttend("No").build()).build()).build();

        assertTrue(
                notificationValidService.isHearingTypeValidToSendNotification(caseData, QUESTION_ROUND_ISSUED_NOTIFICATION)
        );
    }

    @Test
    public void givenCaseIsACohCaseAndNotificationTypeIsNotSentForCoh_thenReturnFalse() {
        SscsCaseData caseData = SscsCaseData.builder().appeal(Appeal.builder().hearingOptions(HearingOptions.builder().wantsToAttend("No").build()).build()).build();

        assertFalse(
                notificationValidService.isHearingTypeValidToSendNotification(caseData, DO_NOT_SEND)
        );
    }

}