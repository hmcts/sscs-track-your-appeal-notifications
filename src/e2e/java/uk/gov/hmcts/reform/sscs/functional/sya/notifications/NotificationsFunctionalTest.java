package uk.gov.hmcts.reform.sscs.functional.sya.notifications;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import junitparams.Parameters;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.functional.AbstractFunctionalTest;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientException;

public class NotificationsFunctionalTest extends AbstractFunctionalTest {

    private static final String AS_APPOINTEE_FOR = "You are receiving this update as the appointee for";
    private static final String RESPONSE_RECEIVED_PAPER_PATH = "paper/responseReceived/";
    private static final String DEAR_APPOINTEE_USER = "Dear Appointee User";
    private static final String APPEAL_ID = "appeal_id";
    private static final String TYA = "v8eg15XeZk";

    @Value("${track.appeal.link}")
    private String tyaLink;

    @Value("${notification.evidenceReceived.appellant.emailId}")
    private String evidenceReceivedEmailTemplateId;

    @Value("${notification.evidenceReceived.appellant.smsId}")
    private String evidenceReceivedSmsTemplateId;

    @Value("${notification.hearingPostponed.appellant.emailId}")
    private String hearingPostponedEmailTemplateId;

    @Value("${notification.hearingAdjourned.appellant.emailId}")
    private String hearingAdjournedEmailTemplateId;

    @Value("${notification.hearingAdjourned.appellant.smsId}")
    private String hearingAdjournedSmsTemplateId;

    @Value("${notification.subscriptionCreated.appellant.smsId}")
    private String subscriptionCreatedSmsTemplateId;

    @Value("${notification.subscriptionUpdated.emailId}")
    private String subscriptionUpdatedEmailTemplateId;

    @Value("${notification.online.responseReceived.emailId}")
    private String onlineResponseReceivedEmailId;

    @Value("${notification.paper.responseReceived.appellant.emailId}")
    private String paperResponseReceivedEmailId;

    @Value("${notification.paper.responseReceived.appellant.smsId}")
    private String paperResponseReceivedSmsId;

    @Value("${notification.subscriptionUpdated.emailId}")
    private String subscriptionUpdateEmailId;

    @Value("${notification.subscriptionUpdated.smsId}")
    private String subscriptionUpdateSmsId;

    @Value("${notification.subscriptionOld.emailId}")
    private String subscriptionUpdateOldEmailId;

    @Value("${notification.subscriptionOld.smsId}")
    private String subscriptionUpdateOldSmsId;

    @Value("${notification.paper.evidenceReceived.appointee.emailId}")
    private String paperAppointeeEvidenceReceivedEmailId;

    @Value("${notification.paper.evidenceReceived.appointee.smsId}")
    private String paperAppointeeEvidenceReceivedSmsId;

    @Value("${notification.paper.responseReceived.appointee.emailId}")
    private String paperAppointeeResponseReceivedEmailId;

    @Value("${notification.paper.responseReceived.appointee.smsId}")
    private String paperAppointeeResponseReceivedSmsId;

    @Value("${notification.oral.evidenceReminder.appointee.emailId}")
    private String oralAppointeeEvidenceReminderEmailId;

    @Value("${notification.oral.evidenceReminder.appointee.smsId}")
    private String oralAppointeeEvidenceReminderSmsId;

    @Value("${notification.paper.evidenceReminder.appointee.emailId}")
    private String paperAppointeeEvidenceReminderEmailId;

    @Value("${notification.paper.evidenceReminder.appointee.smsId}")
    private String paperAppointeeEvidenceReminderSmsId;

    @Value("${notification.appealCreated.appellant.smsId}")
    private String appealCreatedAppellantSmsId;

    @Value("${notification.appealCreated.appellant.emailId}")
    private String appealCreatedAppellantEmailId;

    @Value("${notification.appealCreated.appointee.smsId}")
    private String appealCreatedAppointeeSmsId;

    @Value("${notification.appealCreated.appointee.emailId}")
    private String appealCreatedAppointeeEmailId;

    @Value("${notification.hearingAdjourned.appointee.emailId}")
    private String hearingAdjournedAppointeeEmailId;

    @Value("${notification.hearingAdjourned.appointee.smsId}")
    private String hearingAdjournedAppointeeSmsId;

    @Value("${notification.appealLapsed.appointee.emailId}")
    private String appealLapsedAppointeeEmailTemplateId;

    @Value("${notification.appealLapsed.appointee.smsId}")
    private String appealLapsedAppointeeSmsTemplateId;

    @Value("${notification.appealLapsed.appointee.letterId}")
    private String appealLapsedAppointeeLetterTemplateId;

    @Value("${notification.appealWithdrawn.appointee.emailId}")
    private String appointeeAppealWithdrawnEmailId;

    @Value("${notification.appealWithdrawn.appointee.smsId}")
    private String appointeeAppealWithdrawnSmsId;

    @Value("${notification.hearingBooked.appointee.emailId}")
    private String appointeeHearingBookedEmailId;

    @Value("${notification.hearingBooked.appointee.smsId}")
    private String appointeeHearingBookedSmsId;

    @Value("${notification.evidenceReceived.appellant.emailId}")
    private String appointeeEvidenceReceivedEmailId;

    @Value("${notification.evidenceReceived.appellant.smsId}")
    private String appointeeEvidenceReceivedSmsId;

    @Value("${notification.hearingPostponed.appointee.emailId}")
    private String appointeeHearingPostponedEmailId;

    public NotificationsFunctionalTest() {
        super(30);
    }

    @Test
    public void shouldSendEvidenceReceivedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(EVIDENCE_RECEIVED_NOTIFICATION);

        tryFetchNotificationsForTestCase(
                evidenceReceivedEmailTemplateId,
                evidenceReceivedSmsTemplateId
        );
    }

    @Test
    public void shouldSendHearingPostponedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(POSTPONEMENT_NOTIFICATION);

        tryFetchNotificationsForTestCase(hearingPostponedEmailTemplateId);
    }

    @Test
    public void shouldSendHearingAdjournedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(ADJOURNED_NOTIFICATION);

        tryFetchNotificationsForTestCase(
                hearingAdjournedEmailTemplateId,
                hearingAdjournedSmsTemplateId
        );
    }

    @Test
    public void shouldSendSubscriptionCreatedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(SUBSCRIPTION_CREATED_NOTIFICATION);

        tryFetchNotificationsForTestCase(subscriptionCreatedSmsTemplateId);
    }

    @Test
    public void shouldSendSubscriptionUpdatedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(SUBSCRIPTION_UPDATED_NOTIFICATION);

        tryFetchNotificationsForTestCase(subscriptionUpdatedEmailTemplateId);
    }

    @Test
    public void shouldSendOnlineDwpResponseReceivedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(DWP_RESPONSE_RECEIVED_NOTIFICATION, "online-" + DWP_RESPONSE_RECEIVED_NOTIFICATION.getId() + "Callback.json");
        List<Notification> notifications = tryFetchNotificationsForTestCase(onlineResponseReceivedEmailId);

        assertNotificationBodyContains(notifications, onlineResponseReceivedEmailId, getCaseData().getCaseReference());
    }

    @Test
    public void shouldSendOnlineDwpUploadResponseReceivedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(DWP_UPLOAD_RESPONSE_NOTIFICATION, "online-" + DWP_UPLOAD_RESPONSE_NOTIFICATION.getId() + "Callback.json");
        List<Notification> notifications = tryFetchNotificationsForTestCase(onlineResponseReceivedEmailId);

        assertNotificationBodyContains(notifications, onlineResponseReceivedEmailId, getCaseId().toString());
    }

    @Test
    public void shouldSendAppealCreatedAppellantNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(SYA_APPEAL_CREATED_NOTIFICATION, SYA_APPEAL_CREATED_NOTIFICATION.getId() + "Callback.json");
        List<Notification> notifications = tryFetchNotificationsForTestCase(appealCreatedAppellantEmailId, appealCreatedAppellantSmsId);

        assertNotificationBodyContains(notifications, appealCreatedAppellantEmailId, "appeal has been received");
    }

    @Test
    public void shouldSendValidAppealCreatedAppellantNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(VALID_APPEAL_CREATED, SYA_APPEAL_CREATED_NOTIFICATION.getId() + "Callback.json");
        List<Notification> notifications = tryFetchNotificationsForTestCase(appealCreatedAppellantEmailId, appealCreatedAppellantSmsId);

        assertNotificationBodyContains(notifications, appealCreatedAppellantEmailId, "appeal has been received");
    }

    @Test
    public void shouldSendAppealCreatedAppointeeNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(SYA_APPEAL_CREATED_NOTIFICATION, SYA_APPEAL_CREATED_NOTIFICATION.getId() + "AppointeeCallback.json");
        List<Notification> notifications = tryFetchNotificationsForTestCase(appealCreatedAppointeeEmailId, appealCreatedAppointeeSmsId);

        assertNotificationBodyContains(notifications, appealCreatedAppointeeEmailId, "appointee");
    }

    @Test
    public void shouldSendValidAppealCreatedAppointeeNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(VALID_APPEAL_CREATED, SYA_APPEAL_CREATED_NOTIFICATION.getId() + "AppointeeCallback.json");
        List<Notification> notifications = tryFetchNotificationsForTestCase(appealCreatedAppointeeEmailId, appealCreatedAppointeeSmsId);

        assertNotificationBodyContains(notifications, appealCreatedAppointeeEmailId, "appointee");
    }

    @Test
    @Parameters({
            "pip,judge\\, doctor and disability expert",
            "esa,judge and a doctor"
    })
    public void shouldSendPaperDwpResponseReceivedNotification(final String benefit, String expectedPanelComposition)
            throws Exception {
        simulateCcdCallback(DWP_RESPONSE_RECEIVED_NOTIFICATION, RESPONSE_RECEIVED_PAPER_PATH + benefit + "-paper-"
                + DWP_RESPONSE_RECEIVED_NOTIFICATION.getId() + "Callback.json");
        List<Notification> notifications = tryFetchNotificationsForTestCase(
                paperResponseReceivedEmailId, paperResponseReceivedSmsId);

        String expectedHearingContactDate = "how long";
        String expectedTyaLink = tyaLink.replace(APPEAL_ID, TYA);
        assertNotificationBodyContains(notifications, paperResponseReceivedEmailId, getCaseData().getCaseReference(),
                expectedPanelComposition, expectedHearingContactDate, expectedTyaLink);
        assertNotificationBodyContains(notifications, paperResponseReceivedSmsId, expectedHearingContactDate);
    }

    @Test
    public void shouldNotSendPaperDwpResponseReceivedNotificationIfNotSubscribed() throws NotificationClientException, IOException {
        simulateCcdCallback(DWP_RESPONSE_RECEIVED_NOTIFICATION, RESPONSE_RECEIVED_PAPER_PATH + "paper-no-subscriptions-"
                + DWP_RESPONSE_RECEIVED_NOTIFICATION.getId() + "Callback.json");

        List<Notification> notifications = tryFetchNotificationsForTestCaseWithFlag(true,
                paperResponseReceivedEmailId, paperResponseReceivedSmsId);

        assertTrue(notifications.isEmpty());
    }

    @Test
    public void shouldSendAppointeeEvidenceReminderForOralCaseNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(EVIDENCE_REMINDER_NOTIFICATION,
                "appointee/oral-" + EVIDENCE_REMINDER_NOTIFICATION.getId() + "Callback.json");

        List<Notification> notifications = tryFetchNotificationsForTestCase(
                oralAppointeeEvidenceReminderEmailId,
                oralAppointeeEvidenceReminderSmsId
        );

        assertNotificationBodyContains(
                notifications,
                oralAppointeeEvidenceReminderEmailId,
                DEAR_APPOINTEE_USER,
                AS_APPOINTEE_FOR,
                "/evidence/" + TYA
        );
    }

    @Test
    public void shouldSendAppointeeEvidenceReminderForPaperCaseNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(EVIDENCE_REMINDER_NOTIFICATION,
                "appointee/paper-" + EVIDENCE_REMINDER_NOTIFICATION.getId() + "Callback.json");

        List<Notification> notifications = tryFetchNotificationsForTestCase(
                paperAppointeeEvidenceReminderEmailId,
                paperAppointeeEvidenceReminderSmsId
        );

        assertNotificationBodyContains(
                notifications,
                paperAppointeeEvidenceReminderEmailId,
                DEAR_APPOINTEE_USER,
                AS_APPOINTEE_FOR,
                "/evidence/" + TYA
        );
    }

    @Test
    public void shouldSendAppellantSubscriptionUpdateNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(SUBSCRIPTION_UPDATED_NOTIFICATION,
                "appellant-" + SUBSCRIPTION_UPDATED_NOTIFICATION.getId() + "Callback.json");

        List<Notification> notifications = tryFetchNotificationsForTestCase(
                subscriptionUpdateEmailId,
                subscriptionUpdateSmsId,
                subscriptionUpdateOldEmailId,
                subscriptionUpdateOldSmsId
        );

        Notification updateEmailNotification = notifications.stream().filter(f -> f.getTemplateId().toString().equals(subscriptionUpdatedEmailTemplateId)).collect(Collectors.toList()).get(0);
        assertTrue(updateEmailNotification.getBody().contains("Dear Appellant User\r\n\r\nEmails about your ESA"));
        assertFalse(updateEmailNotification.getBody().contains("You are receiving this update as the appointee for"));
    }

    @Test
    public void shouldSendAppointeeSubscriptionUpdateNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(SUBSCRIPTION_UPDATED_NOTIFICATION,
                "appointee-" + SUBSCRIPTION_UPDATED_NOTIFICATION.getId() + "Callback.json");

        List<Notification> notifications = tryFetchNotificationsForTestCase(
                subscriptionUpdateEmailId,
                subscriptionUpdateSmsId,
                subscriptionUpdateOldEmailId,
                subscriptionUpdateOldSmsId
        );
        Notification updateEmailNotification = notifications.stream().filter(f -> f.getTemplateId().toString().equals(subscriptionUpdatedEmailTemplateId)).collect(Collectors.toList()).get(0);
        assertTrue(updateEmailNotification.getBody().contains("Dear Appointee User\r\n\r\nYou are receiving this update as the appointee for Appellant User.\r\n\r\nEmails about your ESA"));
    }

    @Test
    public void shouldSendAppointeeEvidenceReceivedPaperNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(EVIDENCE_RECEIVED_NOTIFICATION,
                "appointee/paper-" + EVIDENCE_RECEIVED_NOTIFICATION.getId() + "Callback.json");

        List<Notification> notifications = tryFetchNotificationsForTestCase(
                paperAppointeeEvidenceReceivedEmailId,
                paperAppointeeEvidenceReceivedSmsId
        );
        Notification emailNotification = notifications.stream().filter(f -> f.getTemplateId().toString().equals(paperAppointeeEvidenceReceivedEmailId)).collect(Collectors.toList()).get(0);
        assertTrue(emailNotification.getBody().contains("Dear Appointee User"));
        assertTrue(emailNotification.getBody().contains("You are receiving this update as the appointee for Appellant User."));
    }

    @Test
    public void shouldSendAppointeeHearingAdjournedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(ADJOURNED_NOTIFICATION,
                "appointee/" + ADJOURNED_NOTIFICATION.getId() + "Callback.json");

        List<Notification> notifications = tryFetchNotificationsForTestCase(
                hearingAdjournedAppointeeEmailId,
                hearingAdjournedAppointeeSmsId
        );
        Notification emailNotification = notifications.stream().filter(f -> f.getTemplateId().toString().equals(hearingAdjournedAppointeeEmailId)).collect(Collectors.toList()).get(0);
        assertTrue(emailNotification.getBody().contains("Dear Appointee User"));
    }

    @Test
    public void shouldSendAppointeeAppealLapsedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(APPEAL_LAPSED_NOTIFICATION,
                "appointee/" + APPEAL_LAPSED_NOTIFICATION.getId() + "Callback.json");
        List<Notification> notifications = tryFetchNotificationsForTestCase(
                appealLapsedAppointeeEmailTemplateId,
                appealLapsedAppointeeSmsTemplateId
        );
        Notification emailNotification = notifications.stream().filter(f -> f.getTemplateId().toString().equals(appealLapsedAppointeeEmailTemplateId)).collect(Collectors.toList()).get(0);

        assertTrue(emailNotification.getBody().contains("Dear Appointee User"));
        assertTrue(emailNotification.getBody().contains("You are receiving this update as the appointee for"));
    }

    @Test
    public void shouldSendAppointeeDwpAppealLapsedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(APPEAL_LAPSED_NOTIFICATION,
                "appointee/dwpAppealLapsedCallback.json");
        List<Notification> notifications = tryFetchNotificationsForTestCase(
                appealLapsedAppointeeEmailTemplateId,
                appealLapsedAppointeeSmsTemplateId
        );
        Notification emailNotification = notifications.stream().filter(f -> f.getTemplateId().toString().equals(appealLapsedAppointeeEmailTemplateId)).collect(Collectors.toList()).get(0);

        assertTrue(emailNotification.getBody().contains("Dear Appointee User"));
        assertTrue(emailNotification.getBody().contains("You are receiving this update as the appointee for"));
    }

    @Test
    public void shouldSendAppointeeResponseReceivedForPaperCaseNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(DWP_RESPONSE_RECEIVED_NOTIFICATION,
                "appointee/" + DWP_RESPONSE_RECEIVED_NOTIFICATION.getId() + "Callback.json");

        List<Notification> notifications = tryFetchNotificationsForTestCase(
                paperAppointeeResponseReceivedEmailId,
                paperAppointeeResponseReceivedSmsId
        );
        Notification emailNotification = notifications.stream().filter(f -> f.getTemplateId().toString().equals(paperAppointeeResponseReceivedEmailId)).collect(Collectors.toList()).get(0);
        assertTrue(emailNotification.getBody().contains("Dear Appointee User"));
        assertTrue(emailNotification.getBody().contains("You should have received a copy"));
    }

    public void shouldSendAppointeeAppealWithdrawnNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(APPEAL_WITHDRAWN_NOTIFICATION,
                "appointee/" + APPEAL_WITHDRAWN_NOTIFICATION.getId() + "Callback.json");
        List<Notification> notifications = tryFetchNotificationsForTestCase(
                appointeeAppealWithdrawnEmailId, appointeeAppealWithdrawnSmsId);
        Notification emailNotification = notifications.stream()
                .filter(f -> f.getTemplateId().toString().equals(appointeeAppealWithdrawnEmailId))
                .collect(Collectors.toList()).get(0);
        assertTrue(emailNotification.getBody().contains("Dear Appointee User"));
        assertTrue(emailNotification.getBody().contains("You are receiving this update as the appointee for"));
    }

    @Test
    public void shouldSendAppointeeHearingBookedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(HEARING_BOOKED_NOTIFICATION,
                "appointee/" + HEARING_BOOKED_NOTIFICATION.getId() + "Callback.json");

        List<Notification> notifications = tryFetchNotificationsForTestCase(
                appointeeHearingBookedEmailId,
                appointeeHearingBookedSmsId
        );
        Notification emailNotification = notifications.stream().filter(f -> f.getTemplateId().toString().equals(appointeeHearingBookedEmailId)).collect(Collectors.toList()).get(0);
        assertTrue(emailNotification.getBody().contains("Dear Appointee User\r\n\r\nYou are receiving this update as the appointee for Appellant User.\r\n\r\n"));
    }

    @Test
    public void shouldSendAppointeeEvidenceReceivedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(EVIDENCE_RECEIVED_NOTIFICATION,
                "appointee/" + EVIDENCE_RECEIVED_NOTIFICATION.getId() + "Callback.json");
        List<Notification> notifications = tryFetchNotificationsForTestCase(appointeeEvidenceReceivedEmailId, appointeeEvidenceReceivedSmsId);
        Notification emailNotification = notifications.stream().filter(f -> f.getTemplateId().toString().equals(appointeeEvidenceReceivedEmailId)).collect(Collectors.toList()).get(0);
        assertTrue(emailNotification.getBody().contains("Dear Appointee User"));
        assertTrue(emailNotification.getBody().contains("You are receiving this update as the appointee for"));
    }

    @Test
    public void shouldSendAppointeeHearingPostponedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(POSTPONEMENT_NOTIFICATION,
                "appointee/" + POSTPONEMENT_NOTIFICATION.getId() + "Callback.json");
        List<Notification> notifications = tryFetchNotificationsForTestCase(appointeeHearingPostponedEmailId);
        Notification emailNotification = notifications.get(0);

        assertTrue(emailNotification.getBody().contains("Dear Appointee User"));
        assertTrue(emailNotification.getBody().contains("You will receive another email"));
    }

}