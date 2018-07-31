package uk.gov.hmcts.sscs.functional;

import java.io.IOException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.sscs.domain.notify.EventType;
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

    @Value("${notification.appealWithdrawn.emailId}")
    private String appealWithdrawnEmailTemplateId;

    @Value("${notification.hearingBooked.emailId}")
    private String hearingBookedEmailTemplateId;

    @Value("${notification.hearingBooked.smsId}")
    private String hearingBookedSmsTemplateId;

    @Value("${notification.subscriptionCreated.smsId}")
    private String subscriptionCreatedSmsTemplateId;

    @Value("${notification.subscriptionUpdated.emailId}")
    private String subscriptionUpdatedEmailTemplateId;

    @Test
    public void shouldSendAppealReceivedNotification() throws IOException, NotificationClientException {
        simulateCcdCallback(EventType.APPEAL_RECEIVED);

        tryFetchNotificationsForTestCase(
            appealReceivedEmailTemplateId,
            appealReceivedSmsTemplateId
        );
    }

    @Test
    public void shouldSendEvidenceReceivedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(EventType.EVIDENCE_RECEIVED);

        tryFetchNotificationsForTestCase(
            evidenceReceivedEmailTemplateId,
            evidenceReceivedSmsTemplateId
        );
    }

    @Test
    public void shouldSendHearingAdjournedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(EventType.ADJOURNED);

        tryFetchNotificationsForTestCase(
            hearingAdjournedEmailTemplateId,
            hearingAdjournedSmsTemplateId
        );
    }

    @Test
    public void shouldSendHearingPostponedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(EventType.POSTPONEMENT);

        tryFetchNotificationsForTestCase(hearingPostponedEmailTemplateId);
    }

    @Test
    public void shouldSendAppealLapsedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(EventType.APPEAL_LAPSED);

        tryFetchNotificationsForTestCase(appealLapsedEmailTemplateId);
    }

    @Test
    public void shouldSendAppealWithdrawnNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(EventType.APPEAL_WITHDRAWN);

        tryFetchNotificationsForTestCase(appealWithdrawnEmailTemplateId);
    }

    @Test
    public void shouldSendHearingBookedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(EventType.HEARING_BOOKED);

        tryFetchNotificationsForTestCase(
            hearingBookedEmailTemplateId,
            hearingBookedSmsTemplateId
        );
    }

    @Test
    public void shouldSendSubscriptionCreatedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(EventType.SUBSCRIPTION_CREATED);

        tryFetchNotificationsForTestCase(subscriptionCreatedSmsTemplateId);
    }

    @Test
    public void shouldSendSubscriptionUpdatedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(EventType.SUBSCRIPTION_UPDATED);

        tryFetchNotificationsForTestCase(subscriptionUpdatedEmailTemplateId);
    }
}
