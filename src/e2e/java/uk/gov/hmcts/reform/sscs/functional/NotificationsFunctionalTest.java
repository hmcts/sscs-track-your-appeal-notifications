package uk.gov.hmcts.reform.sscs.functional;

import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ADJOURNED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_LAPSED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_RECEIVED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_WITHDRAWN_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DWP_RESPONSE_RECEIVED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.EVIDENCE_RECEIVED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.HEARING_BOOKED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.POSTPONEMENT_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SUBSCRIPTION_CREATED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SUBSCRIPTION_UPDATED_NOTIFICATION;

import java.io.IOException;
import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientException;

public class NotificationsFunctionalTest extends AbstractFunctionalTest {

    @Value("${notification.appealReceived.emailId}")
    private String appealReceivedEmailTemplateId;

    @Value("${notification.appealReceived.smsId}")
    private String appealReceivedSmsTemplateId;

    @Value("${notification.evidenceReceived.emailId}")
    private String evidenceReceivedEmailTemplateId;

    @Value("${notification.evidenceReceived.smsId}")
    private String evidenceReceivedSmsTemplateId;

    @Value("${notification.hearingAdjourned.emailId}")
    private String hearingAdjournedEmailTemplateId;

    @Value("${notification.hearingAdjourned.smsId}")
    private String hearingAdjournedSmsTemplateId;

    @Value("${notification.hearingPostponed.emailId}")
    private String hearingPostponedEmailTemplateId;

    @Value("${notification.appealLapsed.emailId}")
    private String appealLapsedEmailTemplateId;

    @Value("${notification.appealLapsed.smsId}")
    private String appealLapsedSmsTemplateId;

    @Value("${notification.appealWithdrawn.emailId}")
    private String appealWithdrawnEmailTemplateId;

    @Value("${notification.appealWithdrawn.smsId}")
    private String appealWithdrawnSmsTemplateId;

    @Value("${notification.hearingBooked.emailId}")
    private String hearingBookedEmailTemplateId;

    @Value("${notification.hearingBooked.smsId}")
    private String hearingBookedSmsTemplateId;

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

    @Value("${notification.paper.subscriptionUpdated.emailId}")
    private String paperSubscriptionUpdateEmailId;

    @Value("${notification.paper.subscriptionUpdated.smsId}")
    private String paperSubscriptionUpdateSmsId;

    @Value("${notification.paper.subscriptionUpdatedOld.emailId}")
    private String paperSubscriptionUpdateOldEmailId;

    @Value("${notification.paper.subscriptionUpdatedOld.smsId}")
    private String paperSubscriptionUpdateOldSmsId;


    public NotificationsFunctionalTest() {
        super(30);
    }

    @Test
    public void shouldSendAppealReceivedNotification() throws IOException, NotificationClientException {
        simulateCcdCallback(APPEAL_RECEIVED_NOTIFICATION);

        tryFetchNotificationsForTestCase(
                appealReceivedEmailTemplateId,
                appealReceivedSmsTemplateId
        );
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
    public void shouldSendHearingAdjournedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(ADJOURNED_NOTIFICATION);

        tryFetchNotificationsForTestCase(
                hearingAdjournedEmailTemplateId,
                hearingAdjournedSmsTemplateId
        );
    }

    @Test
    public void shouldSendHearingPostponedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(POSTPONEMENT_NOTIFICATION);

        tryFetchNotificationsForTestCase(hearingPostponedEmailTemplateId);
    }

    @Test
    public void shouldSendAppealLapsedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(APPEAL_LAPSED_NOTIFICATION);

        tryFetchNotificationsForTestCase(
                appealLapsedEmailTemplateId,
                appealLapsedSmsTemplateId
        );
    }

    @Test
    public void shouldSendAppealWithdrawnNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(APPEAL_WITHDRAWN_NOTIFICATION);

        tryFetchNotificationsForTestCase(
                appealWithdrawnEmailTemplateId,
                appealWithdrawnSmsTemplateId
        );
    }

    @Test
    public void shouldSendHearingBookedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(HEARING_BOOKED_NOTIFICATION);

        tryFetchNotificationsForTestCase(
                hearingBookedEmailTemplateId,
                hearingBookedSmsTemplateId
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
    public void shouldSendPaperDwpResponseReceivedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(DWP_RESPONSE_RECEIVED_NOTIFICATION, "paper-"
                + DWP_RESPONSE_RECEIVED_NOTIFICATION.getId() + "Callback.json");
        List<Notification> notifications = tryFetchNotificationsForTestCase(
                paperResponseReceivedEmailId, paperResponseReceivedSmsId);

        assertNotificationBodyContains(notifications, paperResponseReceivedEmailId, caseData.getCaseReference());
    }
}
