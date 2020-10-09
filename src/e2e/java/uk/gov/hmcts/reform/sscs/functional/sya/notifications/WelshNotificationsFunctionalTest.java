package uk.gov.hmcts.reform.sscs.functional.sya.notifications;

import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ADJOURNED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_DORMANT_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_WITHDRAWN_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.EVIDENCE_RECEIVED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.EVIDENCE_REMINDER_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.HEARING_BOOKED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.POSTPONEMENT_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SUBSCRIPTION_CREATED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SUBSCRIPTION_UPDATED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SYA_APPEAL_CREATED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.VALID_APPEAL_CREATED;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

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

    @Value("${notification.welsh.hearingPostponed.joint_party.emailId}")
    private String hearingPostponedEmailTemplateIdJointPartyWelsh;

    @Value("${notification.welsh.paper.appealDormant.appellant.emailId}")
    private String appealDormantPaperAppellantEmailTemplateIdWelsh;

    @Value("${notification.welsh.paper.appealDormant.appellant.smsId}")
    private String appealDormantPaperAppellantSmsTemplateIdWelsh;

    @Value("${notification.welsh.paper.appealDormant.joint_party.emailId}")
    private String appealDormantPaperJointPartyEmailTemplateIdWelsh;

    @Value("${notification.welsh.paper.appealDormant.joint_party.smsId}")
    private String appealDormantPaperJointPartySmsTemplateIdWelsh;

    @Value("${notification.welsh.hearingPostponed.appellant.emailId}")
    private String hearingPostponedEmailTemplateIdWelsh;

    @Value("${notification.welsh.hearingAdjourned.appellant.emailId}")
    private String hearingAdjournedEmailTemplateIdWelsh;

    @Value("${notification.welsh.hearingAdjourned.appellant.smsId}")
    private String hearingAdjournedSmsTemplateIdWelsh;

    @Value("${notification.welsh.hearingAdjourned.joint_party.emailId}")
    private String hearingAdjournedJointPartyEmailTemplateIdWelsh;

    @Value("${notification.welsh.hearingAdjourned.joint_party.smsId}")
    private String hearingAdjournedJointPartySmsTemplateIdWelsh;

    @Value("${notification.welsh.subscriptionCreated.appellant.smsId}")
    private String subscriptionCreatedSmsTemplateIdWelsh;

    @Value("${notification.welsh.subscriptionUpdated.emailId}")
    private String subscriptionUpdatedEmailTemplateIdWelsh;

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

    @Value("${notification.welsh.oral.evidenceReminder.joint_party.emailId}")
    private String oralJointPartyEvidenceReminderEmailIdWelsh;

    @Value("${notification.welsh.oral.evidenceReminder.joint_party.smsId}")
    private String oralJointPartyEvidenceReminderSmsIdWelsh;

    @Value("${notification.welsh.paper.evidenceReminder.appointee.emailId}")
    private String paperAppointeeEvidenceReminderEmailIdWelsh;

    @Value("${notification.welsh.paper.evidenceReminder.appointee.smsId}")
    private String paperAppointeeEvidenceReminderSmsIdWelsh;

    @Value("${notification.welsh.paper.evidenceReminder.joint_party.emailId}")
    private String paperJointPartyEvidenceReminderEmailIdWelsh;

    @Value("${notification.welsh.paper.evidenceReminder.joint_party.smsId}")
    private String paperJointPartyEvidenceReminderSmsIdWelsh;

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

    @Value("${notification.welsh.appealWithdrawn.joint_party.emailId}")
    private String jointPartyAppealWithdrawnEmailIdWelsh;

    @Value("${notification.welsh.appealWithdrawn.joint_party.smsId}")
    private String jointPartyAppealWithdrawnSmsIdWelsh;

    @Value("${notification.welsh.hearingBooked.appointee.emailId}")
    private String appointeeHearingBookedEmailIdWelsh;

    @Value("${notification.welsh.hearingBooked.appointee.smsId}")
    private String appointeeHearingBookedSmsIdWelsh;

    @Value("${notification.welsh.hearingBooked.joint_party.emailId}")
    private String jointPartyHearingBookedEmailIdWelsh;

    @Value("${notification.welsh.hearingBooked.joint_party.smsId}")
    private String jointPartyHearingBookedSmsIdWelsh;

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

        tryFetchNotificationsForTestCase(hearingPostponedEmailTemplateIdWelsh, hearingPostponedEmailTemplateIdJointPartyWelsh);
    }

    @Test
    public void shouldSendHearingBookedNotificationWelsh() throws NotificationClientException, IOException {
        simulateCcdCallback(HEARING_BOOKED_NOTIFICATION, "appointee/" + HEARING_BOOKED_NOTIFICATION.getId() + "CallbackWelsh.json");

        tryFetchNotificationsForTestCase(
                appointeeHearingBookedEmailIdWelsh,
                appointeeHearingBookedSmsIdWelsh,
                jointPartyHearingBookedEmailIdWelsh,
                jointPartyHearingBookedSmsIdWelsh);
    }


    @Test
    public void shouldSendHearingAdjournedNotificationWelsh() throws NotificationClientException, IOException {
        simulateWelshCcdCallback(ADJOURNED_NOTIFICATION);

        tryFetchNotificationsForTestCase(
                hearingAdjournedEmailTemplateIdWelsh,
                hearingAdjournedSmsTemplateIdWelsh,
                hearingAdjournedJointPartyEmailTemplateIdWelsh,
                hearingAdjournedJointPartySmsTemplateIdWelsh
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
    public void shouldSendAppointeeEvidenceReminderForPaperCaseNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(EVIDENCE_REMINDER_NOTIFICATION,
                "appointee/paper-" + EVIDENCE_REMINDER_NOTIFICATION.getId() + "CallbackWelsh.json");

        List<Notification> notifications = tryFetchNotificationsForTestCase(
                paperAppointeeEvidenceReminderEmailIdWelsh,
                paperAppointeeEvidenceReminderSmsIdWelsh,
                paperJointPartyEvidenceReminderEmailIdWelsh,
                paperJointPartyEvidenceReminderSmsIdWelsh
        );

        assertNotificationBodyContains(
                notifications,
                paperAppointeeEvidenceReminderEmailIdWelsh,
                DEAR_APPOINTEE_USER,
                AS_APPOINTEE_FOR,
                "/evidence/" + TYA
        );
    }

    @Test
    public void shouldSendPaperAppealDormantNotificationWelsh() throws NotificationClientException, IOException {
        simulateCcdCallback(APPEAL_DORMANT_NOTIFICATION, "paper-" + APPEAL_DORMANT_NOTIFICATION.getId() + "CallbackWelsh.json");
        tryFetchNotificationsForTestCase(
                appealDormantPaperJointPartyEmailTemplateIdWelsh,
                appealDormantPaperAppellantSmsTemplateIdWelsh,
                appealDormantPaperJointPartySmsTemplateIdWelsh,
                appealDormantPaperAppellantEmailTemplateIdWelsh);
    }

    @Test
    public void shouldSendOralAppealDormantNotificationWelsh() throws NotificationClientException, IOException {
        simulateCcdCallback(APPEAL_DORMANT_NOTIFICATION, "oral-" + APPEAL_DORMANT_NOTIFICATION.getId() + "CallbackWelsh.json");
        tryFetchNotificationsForTestCase(appealDormantOralJointPartyEmailTemplateIdWelsh, appealDormantOralAppellantEmailTemplateIdWelsh);
    }

    @Test
    public void shouldSendAppointeeAppealWithdrawnNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(APPEAL_WITHDRAWN_NOTIFICATION,
                "appointee/" + APPEAL_WITHDRAWN_NOTIFICATION.getId() + "CallbackWelsh.json");
        List<Notification> notifications = tryFetchNotificationsForTestCase(
                appointeeAppealWithdrawnEmailIdWelsh,
                appointeeAppealWithdrawnSmsIdWelsh,
                jointPartyAppealWithdrawnEmailIdWelsh,
                jointPartyAppealWithdrawnSmsIdWelsh);
        Notification appointeeEmail = notifications.stream()
                .filter(f -> f.getTemplateId().toString().equals(appointeeAppealWithdrawnEmailIdWelsh))
                .collect(Collectors.toList()).get(0);
        assertTrue(appointeeEmail.getBody().contains("Annwyl Appointee User"));
        assertTrue(appointeeEmail.getBody().contains("You are receiving this update as the appointee for"));
        Notification jointPartyEmail = notifications.stream()
                .filter(f -> f.getTemplateId().toString().equals(jointPartyAppealWithdrawnEmailIdWelsh))
                .collect(Collectors.toList()).get(0);
        assertTrue(jointPartyEmail.getBody().contains("Annwyl Joint Party"));
        assertTrue(jointPartyEmail.getBody().contains("Ysgrifennwyd yr e-bost hwn yn Gymraeg a Saesneg"));
    }

    @Test
    public void shouldSendAppointeeEvidenceReminderForOralCaseNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(EVIDENCE_REMINDER_NOTIFICATION,
                "appointee/oral-" + EVIDENCE_REMINDER_NOTIFICATION.getId() + "CallbackWelsh.json");

        List<Notification> notifications = tryFetchNotificationsForTestCase(
                oralAppointeeEvidenceReminderEmailIdWelsh,
                oralAppointeeEvidenceReminderSmsIdWelsh,
                oralJointPartyEvidenceReminderEmailIdWelsh,
                oralJointPartyEvidenceReminderSmsIdWelsh
                );

        assertNotificationBodyContains(
                notifications,
                oralAppointeeEvidenceReminderEmailIdWelsh,
                DEAR_APPOINTEE_USER,
                AS_APPOINTEE_FOR,
                "/evidence/" + TYA
        );
    }
}