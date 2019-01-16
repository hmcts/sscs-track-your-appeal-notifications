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

    private static final String RESPONSE_RECEIVED_PAPER_PATH = "paper/responseReceived/";
    @Value("${track.appeal.link}")
    private String tyaLink;

    @Value("${notification.evidenceReceived.emailId}")
    private String evidenceReceivedEmailTemplateId;

    @Value("${notification.evidenceReceived.smsId}")
    private String evidenceReceivedSmsTemplateId;

    @Value("${notification.hearingPostponed.appellant.emailId}")
    private String hearingPostponedEmailTemplateId;

    @Value("${notification.hearingAdjourned.appellant.emailId}")
    private String hearingAdjournedEmailTemplateId;

    @Value("${notification.hearingAdjourned.appellant.smsId}")
    private String hearingAdjournedSmsTemplateId;

    @Value("${notification.subscriptionCreated.smsId}")
    private String subscriptionCreatedSmsTemplateId;

    @Value("${notification.subscriptionUpdated.emailId}")
    private String subscriptionUpdatedEmailTemplateId;

    @Value("${notification.online.responseReceived.emailId}")
    private String onlineResponseReceivedEmailId;

    @Value("${notification.online.responseReceived.smsId}")
    private String onlineResponseReceivedSmsId;

    @Value("${notification.paper.responseReceived.emailId}")
    private String paperResponseReceivedEmailId;

    @Value("${notification.paper.responseReceived.smsId}")
    private String paperResponseReceivedSmsId;

    @Value("${notification.subscriptionUpdated.emailId}")
    private String subscriptionUpdateEmailId;

    @Value("${notification.subscriptionUpdated.smsId}")
    private String subscriptionUpdateSmsId;

    @Value("${notification.subscriptionOld.emailId}")
    private String subscriptionUpdateOldEmailId;

    @Value("${notification.subscriptionOld.smsId}")
    private String subscriptionUpdateOldSmsId;


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
        List<Notification> notifications = tryFetchNotificationsForTestCase(onlineResponseReceivedEmailId, onlineResponseReceivedSmsId);

        assertNotificationBodyContains(notifications, onlineResponseReceivedEmailId, caseData.getCaseReference());
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

        String expectedHearingContactDate = "9 April 2016";
        String expectedTyaLink = tyaLink.replace("appeal_id", "v8eg15XeZk");
        assertNotificationBodyContains(notifications, paperResponseReceivedEmailId, caseData.getCaseReference(),
                expectedPanelComposition, expectedHearingContactDate, expectedTyaLink);
        assertNotificationBodyContains(notifications, paperResponseReceivedSmsId, expectedHearingContactDate,
                expectedTyaLink);
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

}