package uk.gov.hmcts.reform.sscs.functional;

import static uk.gov.hmcts.reform.sscs.SscsCaseDataUtils.addHearing;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.config.AppConstants;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientException;

public class ReminderNotificationsFunctionalTest extends AbstractFunctionalTest {

    @Value("${notification.english.oral.evidenceReminder.appellant.emailId}")
    private String evidenceReminderOralAppellantEmailTemplateId;

    @Value("${notification.english.oral.evidenceReminder.appellant.smsId}")
    private String evidenceReminderOralAppellantSmsTemplateId;

    @Value("${notification.english.oral.evidenceReminder.representative.emailId}")
    private String evidenceReminderOralRepresentativeEmailTemplateId;

    @Value("${notification.english.oral.evidenceReminder.representative.smsId}")
    private String evidenceReminderOralRepresentativeSmsTemplateId;

    @Value("${notification.english.oral.responseReceived.appellant.emailId}")
    private String responseReceivedOralAppellantEmailTemplateId;

    @Value("${notification.english.oral.responseReceived.appellant.smsId}")
    private String responseReceivedOralAppellantSmsTemplateId;

    @Value("${notification.english.paper.evidenceReminder.appellant.emailId}")
    private String evidenceReminderPaperAppellantEmailTemplateId;

    @Value("${notification.english.paper.evidenceReminder.appellant.smsId}")
    private String evidenceReminderPaperAppellantSmsTemplateId;

    @Value("${notification.english.paper.evidenceReminder.representative.emailId}")
    private String evidenceReminderPaperRepresentativeEmailTemplateId;

    @Value("${notification.english.paper.evidenceReminder.representative.smsId}")
    private String evidenceReminderPaperRepresentativeSmsTemplateId;

    @Value("${notification.english.paper.responseReceived.appellant.emailId}")
    private String responseReceivedPaperAppellantEmailTemplateId;

    @Value("${notification.english.paper.responseReceived.appellant.smsId}")
    private String responseReceivedPaperAppellantSmsTemplateId;

    @Value("${notification.english.hearingReminder.appellant.emailId}")
    private String hearingReminderAppellantEmailTemplateId;

    @Value("${notification.english.hearingReminder.appellant.smsId}")
    private String hearingReminderAppellantSmsTemplateId;

    @Value("${notification.english.hearingReminder.representative.emailId}")
    private String hearingReminderRepresentativeEmailTemplateId;

    @Value("${notification.english.hearingReminder.representative.smsId}")
    private String hearingReminderRepresentativeSmsTemplateId;

    @Value("${notification.english.hearingReminder.appointee.emailId}")
    private String hearingReminderAppointeeEmailTemplateId;

    @Value("${notification.english.hearingReminder.appointee.smsId}")
    private String hearingReminderAppointeeSmsTemplateId;

    public ReminderNotificationsFunctionalTest() {
        super(120);
    }

    @Test
    public void shouldSendNotificationsWhenDwpResponseReceivedEventIsReceivedForOralWithAnAppellantSubscribed() throws IOException, NotificationClientException {
        triggerEventWithHearingType(DWP_RESPONSE_RECEIVED_NOTIFICATION, "oral");
        simulateCcdCallback(DWP_RESPONSE_RECEIVED_NOTIFICATION,"representative/oral-" + DWP_RESPONSE_RECEIVED_NOTIFICATION.getId() + "Callback.json");

        List<Notification> notifications =
            tryFetchNotificationsForTestCase(
                responseReceivedOralAppellantEmailTemplateId,
                responseReceivedOralAppellantSmsTemplateId,
                evidenceReminderOralAppellantEmailTemplateId,
                evidenceReminderOralAppellantSmsTemplateId,
                evidenceReminderOralRepresentativeEmailTemplateId,
                evidenceReminderOralRepresentativeSmsTemplateId

            );

        assertNotificationSubjectContains(notifications, evidenceReminderOralAppellantEmailTemplateId, "ESA");
        assertNotificationBodyContains(
            notifications,
                evidenceReminderOralAppellantEmailTemplateId,
            caseReference,
            "User Test",
            "ESA",
            "/evidence"
        );

        assertNotificationBodyContains(notifications, evidenceReminderOralAppellantSmsTemplateId, "ESA");

        assertNotificationSubjectContains(notifications, responseReceivedOralAppellantEmailTemplateId, "ESA");
        assertNotificationBodyContains(
            notifications,
            responseReceivedOralAppellantEmailTemplateId,
            caseReference,
            "User Test",
            "ESA",
            "DWP",
            "response",
            "/trackyourappeal",
            "how long"
        );

        assertNotificationBodyContains(
            notifications,
            responseReceivedOralAppellantSmsTemplateId,
            "ESA",
            "DWP",
            "response",
            "how long"
        );
    }


    @Test
    public void shouldSendNotificationsWhenDwpResponseReceivedEventIsReceivedForPaperWithAnAppellantSubscribed() throws IOException, NotificationClientException {

        triggerEventWithHearingType(DWP_RESPONSE_RECEIVED_NOTIFICATION, "paper");
        simulateCcdCallback(DWP_RESPONSE_RECEIVED_NOTIFICATION,"representative/paper-" + DWP_RESPONSE_RECEIVED_NOTIFICATION.getId() + "Callback.json");

        List<Notification> notifications =
            tryFetchNotificationsForTestCase(
                responseReceivedPaperAppellantEmailTemplateId,
                responseReceivedPaperAppellantSmsTemplateId,
                evidenceReminderPaperAppellantEmailTemplateId,
                evidenceReminderPaperAppellantSmsTemplateId,
                evidenceReminderPaperRepresentativeEmailTemplateId,
                evidenceReminderPaperRepresentativeSmsTemplateId
        );

        assertNotificationSubjectContains(notifications, evidenceReminderPaperAppellantEmailTemplateId, "ESA");
        assertNotificationBodyContains(
                notifications,
                evidenceReminderPaperAppellantEmailTemplateId,
                caseReference,
                "User Test",
                "ESA",
                "/evidence"
        );

        assertNotificationBodyContains(notifications, evidenceReminderPaperAppellantSmsTemplateId, "ESA");

        assertNotificationSubjectContains(notifications, responseReceivedPaperAppellantEmailTemplateId, "ESA");
        assertNotificationBodyContains(
                notifications,
                responseReceivedPaperAppellantEmailTemplateId,
                caseReference,
                "User Test",
                "ESA",
                "DWP",
                "response",
                "/trackyourappeal",
                "how long"
        );

        assertNotificationBodyContains(
                notifications,
                responseReceivedPaperAppellantSmsTemplateId,
                "ESA",
                "DWP",
                "response",
                "how long"
        );
    }

    @Test
    public void shouldSendNotificationsWhenDwpResponseReceivedEventIsReceivedForOralWithAnRepSubscribed() throws IOException, NotificationClientException {
        subscribeRepresentative();
        triggerEventWithHearingType(DWP_RESPONSE_RECEIVED_NOTIFICATION, "oral");
        simulateCcdCallback(DWP_RESPONSE_RECEIVED_NOTIFICATION,"representative/oral-" + DWP_RESPONSE_RECEIVED_NOTIFICATION.getId() + "Callback.json");

        List<Notification> notifications =
                tryFetchNotificationsForTestCase(
                        responseReceivedOralAppellantEmailTemplateId,
                        responseReceivedOralAppellantSmsTemplateId,
                        evidenceReminderOralRepresentativeEmailTemplateId,
                        evidenceReminderOralRepresentativeSmsTemplateId
                );

        assertNotificationSubjectContains(notifications, evidenceReminderOralRepresentativeEmailTemplateId, "ESA");
        assertNotificationBodyContains(
                notifications,
                evidenceReminderOralRepresentativeEmailTemplateId,
                caseReference,
                "Harry Potter",
                "ESA",
                "/evidence"
        );

        assertNotificationBodyContains(notifications, evidenceReminderOralRepresentativeSmsTemplateId, "ESA");

        assertNotificationSubjectContains(notifications, responseReceivedOralAppellantEmailTemplateId, "ESA");
        assertNotificationBodyContains(
                notifications,
                responseReceivedOralAppellantEmailTemplateId,
                caseReference,
                "User Test",
                "ESA",
                "DWP",
                "response",
                "/trackyourappeal",
                "how long"
        );

        assertNotificationBodyContains(
                notifications,
                responseReceivedOralAppellantSmsTemplateId,
                "ESA",
                "DWP",
                "response",
                "how long"
        );
    }


    @Test
    public void shouldSendNotificationsWhenDwpResponseReceivedEventIsReceivedForPaperWithAnRepSubscribed() throws IOException, NotificationClientException {
        subscribeRepresentative();
        triggerEventWithHearingType(DWP_RESPONSE_RECEIVED_NOTIFICATION, "paper");
        simulateCcdCallback(DWP_RESPONSE_RECEIVED_NOTIFICATION,"representative/paper-" + DWP_RESPONSE_RECEIVED_NOTIFICATION.getId() + "Callback.json");

        List<Notification> notifications =
                tryFetchNotificationsForTestCase(
                        responseReceivedPaperAppellantEmailTemplateId,
                        responseReceivedPaperAppellantSmsTemplateId,
                        evidenceReminderPaperRepresentativeEmailTemplateId,
                        evidenceReminderPaperRepresentativeSmsTemplateId
                );

        assertNotificationSubjectContains(notifications, evidenceReminderPaperRepresentativeEmailTemplateId, "ESA");
        assertNotificationBodyContains(
                notifications,
                evidenceReminderPaperRepresentativeEmailTemplateId,
                caseReference,
                "Harry Potter",
                "ESA",
                "/evidence"
        );

        assertNotificationBodyContains(notifications, evidenceReminderPaperRepresentativeSmsTemplateId, "ESA");

        assertNotificationSubjectContains(notifications, responseReceivedPaperAppellantEmailTemplateId, "ESA");
        assertNotificationBodyContains(
                notifications,
                responseReceivedPaperAppellantEmailTemplateId,
                caseReference,
                "User Test",
                "ESA",
                "DWP",
                "response",
                "/trackyourappeal",
                "how long"
        );

        assertNotificationBodyContains(
                notifications,
                responseReceivedPaperAppellantSmsTemplateId,
                "ESA",
                "DWP",
                "response",
                "how long"
        );
    }

    @Test
    // Put back when covid19 feature turned off
    @Ignore
    public void shouldSendNotificationsWhenHearingBookedEventIsReceivedWhenAnAppellantIsSubscribed() throws IOException, NotificationClientException {

        addHearing(caseData, 0);
        triggerEvent(HEARING_BOOKED_NOTIFICATION);
        simulateCcdCallback(HEARING_BOOKED_NOTIFICATION, "appointee/" + HEARING_BOOKED_NOTIFICATION.getId() + "Callback.json");

        String formattedString = LocalDate.now().format(DateTimeFormatter.ofPattern(AppConstants.RESPONSE_DATE_FORMAT));

        List<Notification> notifications =
            tryFetchNotificationsForTestCaseWithExpectedText(formattedString,
                hearingReminderAppellantEmailTemplateId,
                hearingReminderAppointeeEmailTemplateId,
                hearingReminderAppellantSmsTemplateId,
                hearingReminderAppointeeSmsTemplateId
            );

        assertNotificationSubjectContains(notifications, hearingReminderAppellantEmailTemplateId, "ESA");

        assertNotificationBodyContains(
            notifications,
                hearingReminderAppellantEmailTemplateId,
            caseReference,
            "ESA",
            "reminder",
            formattedString,
            "11:59 PM",
            "AB12 0HN",
            "/abouthearing"
        );

        assertNotificationBodyContains(
            notifications,
                hearingReminderAppellantSmsTemplateId,
            "ESA",
            "reminder",
            formattedString,
            "11:59 PM",
            "AB12 0HN",
            "/abouthearing"
        );
    }

    @Test
    // Put back when covid19 feature turned off
    @Ignore
    public void shouldSendNotificationsWhenHearingBookedEventIsReceivedWhenARepresentativeIsSubscribed() throws IOException, NotificationClientException {
        subscribeRepresentative();
        addHearing(caseData, 0);
        triggerEvent(HEARING_BOOKED_NOTIFICATION);
        simulateCcdCallback(HEARING_BOOKED_NOTIFICATION,"representative/" + HEARING_BOOKED_NOTIFICATION.getId() + "Callback.json");

        List<Notification> notifications =
                tryFetchNotificationsForTestCase(
                        hearingReminderRepresentativeEmailTemplateId,
                        hearingReminderRepresentativeEmailTemplateId,
                        hearingReminderRepresentativeSmsTemplateId,
                        hearingReminderRepresentativeSmsTemplateId
                );

        assertNotificationSubjectContains(notifications, hearingReminderRepresentativeEmailTemplateId, "ESA");

        String formattedString = LocalDate.now().format(DateTimeFormatter.ofPattern(AppConstants.RESPONSE_DATE_FORMAT));

        assertNotificationBodyContains(
                notifications,
                hearingReminderRepresentativeEmailTemplateId,
                caseReference,
                "ESA",
                "reminder",
                formattedString,
                "11:59 PM",
                "AB12 0HN",
                "/abouthearing"
        );

        assertNotificationBodyContains(
                notifications,
                hearingReminderRepresentativeSmsTemplateId,
                "ESA",
                "reminder",
                formattedString,
                "11:59 PM",
                "AB12 0HN",
                "/abouthearing"
        );
    }
}
