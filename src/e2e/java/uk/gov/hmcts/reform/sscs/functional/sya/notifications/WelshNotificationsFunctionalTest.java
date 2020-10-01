package uk.gov.hmcts.reform.sscs.functional.sya.notifications;

import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ADJOURNED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_DORMANT_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DWP_RESPONSE_RECEIVED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DWP_UPLOAD_RESPONSE_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.EVIDENCE_RECEIVED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.POSTPONEMENT_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SUBSCRIPTION_CREATED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SUBSCRIPTION_UPDATED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SYA_APPEAL_CREATED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.VALID_APPEAL_CREATED;

import java.io.IOException;
import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.functional.AbstractFunctionalTest;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientException;

public class WelshNotificationsFunctionalTest extends AbstractFunctionalTest {

    private static final String AS_APPOINTEE_FOR = "You are receiving this update as the appointee for";
    private static final String RESPONSE_RECEIVED_PAPER_PATH = "paper/responseReceived/";
    private static final String DEAR_APPOINTEE_USER = "Dear Appointee User";
    private static final String APPEAL_ID = "appeal_id";
    private static final String TYA = "v8eg15XeZk";

    @Value("${track.appeal.link}")
    private String tyaLink;

    @Value("${notification.welsh.oral.appealDormant.appellant.emailId}")
    private String appealDormantOralAppellantEmailTemplateIdWelsh;

    @Value("${notification.welsh.oral.appealDormant.joint_party.emailId}")
    private String appealDormantOralJointPartyEmailTemplateIdWelsh;

    @Value("${notification.welsh.hearingPostponed.appellant.emailId}")
    private String hearingPostponedEmailTemplateIdWelsh;

    @Value("${notification.welsh.hearingAdjourned.appellant.emailId}")
    private String hearingAdjournedEmailTemplateIdWelsh;

    @Value("${notification.welsh.hearingAdjourned.appellant.smsId}")
    private String hearingAdjournedSmsTemplateIdWelsh;

    @Value("${notification.welsh.subscriptionCreated.appellant.smsId}")
    private String subscriptionCreatedSmsTemplateIdWelsh;

    @Value("${notification.welsh.subscriptionUpdated.emailId}")
    private String subscriptionUpdatedEmailTemplateIdWelsh;

    @Value("${notification.welsh.online.responseReceived.emailId}")
    private String onlineResponseReceivedEmailIdWelsh;

    @Value("${notification.welsh.paper.responseReceived.appellant.emailId}")
    private String paperResponseReceivedEmailIdWelsh;

    @Value("${notification.welsh.paper.responseReceived.appellant.smsId}")
    private String paperResponseReceivedSmsIdWelsh;

    @Value("${notification.welsh.subscriptionUpdated.emailId}")
    private String subscriptionUpdateEmailIdWelsh;

    @Value("${notification.welsh.subscriptionUpdated.smsId}")
    private String subscriptionUpdateSmsIdWelsh;

    @Value("${notification.welsh.subscriptionOld.emailId}")
    private String subscriptionUpdateOldEmailIdWelsh;

    @Value("${notification.welsh.subscriptionOld.smsId}")
    private String subscriptionUpdateOldSmsIdWelsh;

    @Value("${notification.welsh.oral.evidenceReceived.appointee.emailId}")
    private String oralEvidenceReceivedEmailTemplateIdWelsh;

    @Value("${notification.welsh.oral.evidenceReceived.appointee.smsId}")
    private String oralEvidenceReceivedSmsTemplateIdWelsh;

    @Value("${notification.welsh.paper.evidenceReceived.appointee.emailId}")
    private String paperAppointeeEvidenceReceivedEmailIdWelsh;

    @Value("${notification.welsh.paper.evidenceReceived.appointee.smsId}")
    private String paperAppointeeEvidenceReceivedSmsIdWelsh;

    @Value("${notification.welsh.paper.evidenceReceived.appointee.emailId}")
    private String appointeeEvidenceReceivedEmailIdWelsh;

    @Value("${notification.welsh.paper.evidenceReceived.appointee.smsId}")
    private String appointeeEvidenceReceivedSmsIdWelsh;

    @Value("${notification.welsh.paper.responseReceived.appointee.emailId}")
    private String paperAppointeeResponseReceivedEmailIdWelsh;

    @Value("${notification.welsh.paper.responseReceived.appointee.smsId}")
    private String paperAppointeeResponseReceivedSmsIdWelsh;

    @Value("${notification.welsh.oral.evidenceReminder.appointee.emailId}")
    private String oralAppointeeEvidenceReminderEmailIdWelsh;

    @Value("${notification.welsh.oral.evidenceReminder.appointee.smsId}")
    private String oralAppointeeEvidenceReminderSmsIdWelsh;

    @Value("${notification.welsh.paper.evidenceReminder.appointee.emailId}")
    private String paperAppointeeEvidenceReminderEmailIdWelsh;

    @Value("${notification.welsh.paper.evidenceReminder.appointee.smsId}")
    private String paperAppointeeEvidenceReminderSmsIdWelsh;

    @Value("${notification.welsh.appealCreated.appellant.smsId}")
    private String appealCreatedAppellantSmsIdWelsh;

    @Value("${notification.welsh.appealCreated.appellant.emailId}")
    private String appealCreatedAppellantEmailIdWelsh;

    @Value("${notification.welsh.appealCreated.appointee.smsId}")
    private String appealCreatedAppointeeSmsIdWelsh;

    @Value("${notification.welsh.appealCreated.appointee.emailId}")
    private String appealCreatedAppointeeEmailIdWelsh;

    @Value("${notification.welsh.hearingAdjourned.appointee.emailId}")
    private String hearingAdjournedAppointeeEmailIdWelsh;

    @Value("${notification.welsh.hearingAdjourned.appointee.smsId}")
    private String hearingAdjournedAppointeeSmsIdWelsh;

    @Value("${notification.welsh.appealLapsed.appointee.emailId}")
    private String appealLapsedAppointeeEmailTemplateIdWelsh;

    @Value("${notification.welsh.appealLapsed.appointee.smsId}")
    private String appealLapsedAppointeeSmsTemplateIdWelsh;

    @Value("${notification.welsh.appealLapsed.appointee.letterId}")
    private String appealLapsedAppointeeLetterTemplateIdWelsh;

    @Value("${notification.welsh.appealWithdrawn.appointee.emailId}")
    private String appointeeAppealWithdrawnEmailIdWelsh;

    @Value("${notification.welsh.appealWithdrawn.appointee.smsId}")
    private String appointeeAppealWithdrawnSmsIdWelsh;

    @Value("${notification.welsh.hearingBooked.appointee.emailId}")
    private String appointeeHearingBookedEmailIdWelsh;

    @Value("${notification.welsh.hearingBooked.appointee.smsId}")
    private String appointeeHearingBookedSmsIdWelsh;

    @Value("${notification.welsh.hearingPostponed.appointee.emailId}")
    private String appointeeHearingPostponedEmailIdWelsh;

    public WelshNotificationsFunctionalTest() {
        super(30);
    }

    @Override
    public void setup() {
        idamTokens = idamService.getIdamTokens();
        createCase(true);
    }

    @Test
    public void shouldSendEvidenceReceivedNotificationWelsh() throws NotificationClientException, IOException {
        simulateWelshCcdCallback(EVIDENCE_RECEIVED_NOTIFICATION);
        tryFetchNotificationsForTestCase(
                oralEvidenceReceivedEmailTemplateIdWelsh,
                oralEvidenceReceivedSmsTemplateIdWelsh
        );
    }

    @Test
    public void shouldSendHearingPostponedNotificationWelsh() throws NotificationClientException, IOException {
        simulateWelshCcdCallback(POSTPONEMENT_NOTIFICATION);

        tryFetchNotificationsForTestCase(hearingPostponedEmailTemplateIdWelsh);
    }


    @Test
    public void shouldSendHearingAdjournedNotificationWelsh() throws NotificationClientException, IOException {
        simulateWelshCcdCallback(ADJOURNED_NOTIFICATION);

        tryFetchNotificationsForTestCase(
                hearingAdjournedEmailTemplateIdWelsh,
                hearingAdjournedSmsTemplateIdWelsh
        );
    }

    @Test
    public void shouldSendSubscriptionCreatedNotificationWelsh() throws NotificationClientException, IOException {

        simulateWelshCcdCallback(SUBSCRIPTION_CREATED_NOTIFICATION);

        tryFetchNotificationsForTestCase(subscriptionCreatedSmsTemplateIdWelsh);
    }


    @Test
    public void shouldSendSubscriptionUpdatedNotificationWelsh() throws NotificationClientException, IOException {
        simulateWelshCcdCallback(SUBSCRIPTION_UPDATED_NOTIFICATION);

        tryFetchNotificationsForTestCase(subscriptionUpdatedEmailTemplateIdWelsh);
    }


    @Test
    public void shouldSendOnlineDwpResponseReceivedNotificationWelsh() throws NotificationClientException, IOException {
        simulateCcdCallback(DWP_RESPONSE_RECEIVED_NOTIFICATION, "online-" + DWP_RESPONSE_RECEIVED_NOTIFICATION.getId() + "CallbackWelsh.json");
        List<Notification> notifications = tryFetchNotificationsForTestCase(onlineResponseReceivedEmailIdWelsh);

        assertNotificationBodyContains(notifications, onlineResponseReceivedEmailIdWelsh, caseData.getCaseReference());
    }

    @Test
    public void shouldSendOnlineDwpUploadResponseReceivedNotificationWelsh() throws NotificationClientException, IOException {
        simulateCcdCallback(DWP_UPLOAD_RESPONSE_NOTIFICATION, "online-" + DWP_UPLOAD_RESPONSE_NOTIFICATION.getId() + "CallbackWelsh.json");
        List<Notification> notifications = tryFetchNotificationsForTestCase(onlineResponseReceivedEmailIdWelsh);

        assertNotificationBodyContains(notifications, onlineResponseReceivedEmailIdWelsh, caseId.toString());
    }


    @Test
    public void shouldSendAppealCreatedAppellantNotificationWelsh() throws NotificationClientException, IOException {
        simulateCcdCallback(SYA_APPEAL_CREATED_NOTIFICATION, SYA_APPEAL_CREATED_NOTIFICATION.getId() + "CallbackWelsh.json");
        List<Notification> notifications = tryFetchNotificationsForTestCase(appealCreatedAppellantEmailIdWelsh, appealCreatedAppellantSmsIdWelsh);

        assertNotificationBodyContains(notifications, appealCreatedAppellantEmailIdWelsh, "appeal has been received");
    }


    @Test
    public void shouldSendValidAppealCreatedAppellantNotificationWelsh() throws NotificationClientException, IOException {
        simulateCcdCallback(VALID_APPEAL_CREATED, SYA_APPEAL_CREATED_NOTIFICATION.getId() + "CallbackWelsh.json");
        List<Notification> notifications = tryFetchNotificationsForTestCase(appealCreatedAppellantEmailIdWelsh, appealCreatedAppellantSmsIdWelsh);

        assertNotificationBodyContains(notifications, appealCreatedAppellantEmailIdWelsh, "appeal has been received");
    }

    @Test
    public void shouldSendAppealCreatedAppointeeNotificationWelsh() throws NotificationClientException, IOException {
        simulateCcdCallback(SYA_APPEAL_CREATED_NOTIFICATION, SYA_APPEAL_CREATED_NOTIFICATION.getId() + "AppointeeCallbackWelsh.json");
        List<Notification> notifications = tryFetchNotificationsForTestCase(appealCreatedAppointeeEmailIdWelsh, appealCreatedAppointeeSmsIdWelsh);

        assertNotificationBodyContains(notifications, appealCreatedAppointeeEmailIdWelsh, "appointee");
    }


    @Test
    public void shouldSendValidAppealCreatedAppointeeNotificationWelsh() throws NotificationClientException, IOException {
        simulateCcdCallback(VALID_APPEAL_CREATED, SYA_APPEAL_CREATED_NOTIFICATION.getId() + "AppointeeCallbackWelsh.json");
        List<Notification> notifications = tryFetchNotificationsForTestCase(appealCreatedAppointeeEmailIdWelsh, appealCreatedAppointeeSmsIdWelsh);

        assertNotificationBodyContains(notifications, appealCreatedAppointeeEmailIdWelsh, "appointee");
    }

    @Test
    public void shouldSendOralAppealDormantNotificationWelsh() throws NotificationClientException, IOException {
        simulateCcdCallback(APPEAL_DORMANT_NOTIFICATION, "oral-" + APPEAL_DORMANT_NOTIFICATION.getId() + "CallbackWelsh.json");
        tryFetchNotificationsForTestCase(appealDormantOralJointPartyEmailTemplateIdWelsh, appealDormantOralAppellantEmailTemplateIdWelsh);
    }


}