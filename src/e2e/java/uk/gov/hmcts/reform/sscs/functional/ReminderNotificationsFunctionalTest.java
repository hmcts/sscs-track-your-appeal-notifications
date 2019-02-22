package uk.gov.hmcts.reform.sscs.functional;

import static uk.gov.hmcts.reform.sscs.SscsCaseDataUtils.addHearing;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.config.AppConstants;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientException;

public class ReminderNotificationsFunctionalTest extends AbstractFunctionalTest {

    private static final String AS_APPOINTEE_FOR = "You are receiving this update as the appointee for";
    private static final String APPOINTEE_TYA = "v8eg15XeZk";
    public static final String APPOINTEE_FIRST_NAME = "Appointee";
    public static final String APPOINTEE_LAST_NAME = "User";
    private static final String DEAR_APPOINTEE_USER = "Dear " + APPOINTEE_FIRST_NAME + " " + APPOINTEE_LAST_NAME;

    @Value("${notification.dwpResponseLateReminder.emailId}")
    private String dwpResponseLateReminderEmailTemplateId;

    @Value("${notification.dwpResponseLateReminder.smsId}")
    private String dwpResponseLateReminderSmsTemplateId;

    @Value("${notification.evidenceReminder.emailId}")
    private String evidenceReminderEmailTemplateId;

    @Value("${notification.evidenceReminder.smsId}")
    private String evidenceReminderSmsTemplateId;

    @Value("${notification.oral.responseReceived.emailId}")
    private String responseReceivedEmailTemplateId;

    @Value("${notification.oral.responseReceived.smsId}")
    private String responseReceivedSmsTemplateId;

    @Value("${notification.hearingReminder.emailId}")
    private String hearingReminderEmailTemplateId;

    @Value("${notification.hearingReminder.smsId}")
    private String hearingReminderSmsTemplateId;

    @Value("${notification.hearingHoldingReminder.emailId}")
    private String firstHearingHoldingReminderEmailTemplateId;

    @Value("${notification.hearingHoldingReminder.smsId}")
    private String firstHearingHoldingReminderSmsTemplateId;

    @Value("${notification.secondHearingHoldingReminder.emailId}")
    private String secondHearingHoldingReminderEmailTemplateId;

    @Value("${notification.secondHearingHoldingReminder.smsId}")
    private String secondHearingHoldingReminderSmsTemplateId;

    @Value("${notification.thirdHearingHoldingReminder.emailId}")
    private String thirdHearingHoldingReminderEmailTemplateId;

    @Value("${notification.thirdHearingHoldingReminder.smsId}")
    private String thirdHearingHoldingReminderSmsTemplateId;

    @Value("${notification.finalHearingHoldingReminder.emailId}")
    private String finalHearingHoldingReminderEmailTemplateId;

    @Value("${notification.finalHearingHoldingReminder.smsId}")
    private String finalHearingHoldingReminderSmsTemplateId;

    public ReminderNotificationsFunctionalTest() {
        super(90);
    }

    @Test
    public void shouldSendNotificationsWhenAppealReceivedEventIsReceived() throws IOException, NotificationClientException {

        simulateCcdCallback(APPEAL_RECEIVED_NOTIFICATION,"representative/" + APPEAL_RECEIVED_NOTIFICATION.getId() + "Callback.json");

        List<Notification> notifications =
            tryFetchNotificationsForTestCase(
                dwpResponseLateReminderEmailTemplateId,
                dwpResponseLateReminderSmsTemplateId
            );

        assertNotificationSubjectContains(notifications, dwpResponseLateReminderEmailTemplateId, "ESA");
        assertNotificationBodyContains(
            notifications,
            dwpResponseLateReminderEmailTemplateId,
            caseReference,
            "User Test",
            "DWP",
            "due to respond",
            "28 June 2017",
            "/trackyourappeal"
        );

        assertNotificationBodyContains(
            notifications,
            dwpResponseLateReminderSmsTemplateId,
            "DWP",
            "due to respond",
            "28 June 2017",
            "/trackyourappeal"
        );
    }

    @Test
    public void shouldSendNotificationsWhenDwpResponseReceivedEventIsReceived() throws IOException, NotificationClientException {

        triggerEvent(DWP_RESPONSE_RECEIVED_NOTIFICATION);
        simulateCcdCallback(DWP_RESPONSE_RECEIVED_NOTIFICATION);

        List<Notification> notifications =
            tryFetchNotificationsForTestCase(
                responseReceivedEmailTemplateId,
                responseReceivedSmsTemplateId,
                evidenceReminderEmailTemplateId,
                evidenceReminderSmsTemplateId,
                firstHearingHoldingReminderEmailTemplateId,
                firstHearingHoldingReminderSmsTemplateId,
                secondHearingHoldingReminderEmailTemplateId,
                secondHearingHoldingReminderSmsTemplateId,
                thirdHearingHoldingReminderEmailTemplateId,
                thirdHearingHoldingReminderSmsTemplateId,
                finalHearingHoldingReminderEmailTemplateId,
                finalHearingHoldingReminderSmsTemplateId
            );

        assertNotificationSubjectContains(notifications, evidenceReminderEmailTemplateId, "ESA");
        assertNotificationBodyContains(
            notifications,
            evidenceReminderEmailTemplateId,
            caseReference,
            "User Test",
            "ESA",
            "/evidence"
        );

        assertNotificationBodyContains(notifications, evidenceReminderSmsTemplateId, "ESA");

        assertNotificationSubjectContains(notifications, responseReceivedEmailTemplateId, "ESA");
        assertNotificationBodyContains(
            notifications,
            responseReceivedEmailTemplateId,
            caseReference,
            "User Test",
            "ESA",
            "DWP",
            "response",
            "/trackyourappeal",
            "12 March 2016"
        );

        assertNotificationBodyContains(
            notifications,
            responseReceivedSmsTemplateId,
            "ESA",
            "DWP",
            "response",
            "/trackyourappeal",
            "12 March 2016"
        );

        assertNotificationSubjectContains(notifications, firstHearingHoldingReminderEmailTemplateId, "ESA");
        assertNotificationBodyContains(
            notifications,
            firstHearingHoldingReminderEmailTemplateId,
            caseReference,
            "User Test",
            "ESA",
            "not been booked",
            "/trackyourappeal",
            "23 April 2016"
        );

        assertNotificationBodyContains(
            notifications,
            firstHearingHoldingReminderSmsTemplateId,
            "ESA",
            "not been booked",
            "/trackyourappeal",
            "23 April 2016"
        );

        assertNotificationSubjectContains(notifications, secondHearingHoldingReminderEmailTemplateId, "Your ESA appeal");
        assertNotificationBodyContains(
            notifications,
            secondHearingHoldingReminderEmailTemplateId,
            caseReference,
            "User Test",
            "ESA benefit",
            "not been booked",
            "/trackyourappeal",
            "4 June 2016"
        );

        assertNotificationBodyContains(
            notifications,
            secondHearingHoldingReminderSmsTemplateId,
            "ESA benefit",
            "not been booked",
            "/trackyourappeal",
            "4 June 2016"
        );

        assertNotificationSubjectContains(notifications, thirdHearingHoldingReminderEmailTemplateId, "Your ESA appeal");
        assertNotificationBodyContains(
            notifications,
            thirdHearingHoldingReminderEmailTemplateId,
            caseReference,
            "User Test",
            "ESA",
            "not been booked",
            "/trackyourappeal",
            "16 July 2016"
        );

        assertNotificationBodyContains(
            notifications,
            thirdHearingHoldingReminderSmsTemplateId,
            "ESA",
            "not been booked",
            "/trackyourappeal",
            "16 July 2016"
        );

        assertNotificationSubjectContains(notifications, finalHearingHoldingReminderEmailTemplateId, "ESA");
        assertNotificationBodyContains(
            notifications,
            finalHearingHoldingReminderEmailTemplateId,
            caseReference,
            "User Test",
            "ESA",
            "not been booked",
            "/trackyourappeal"
        );

        assertNotificationBodyContains(
            notifications,
            finalHearingHoldingReminderSmsTemplateId,
            "ESA",
            "not been booked",
            "/trackyourappeal"
        );
    }

    @Test
    public void shouldSendNotificationsWhenHearingBookedEventIsReceived() throws IOException, NotificationClientException {

        addHearing(caseData, 0);
        triggerEvent(HEARING_BOOKED_NOTIFICATION);
        simulateCcdCallback(HEARING_BOOKED_NOTIFICATION);

        List<Notification> notifications =
            tryFetchNotificationsForTestCase(
                hearingReminderEmailTemplateId,
                hearingReminderEmailTemplateId,
                hearingReminderSmsTemplateId,
                hearingReminderSmsTemplateId
            );

        assertNotificationSubjectContains(notifications, hearingReminderEmailTemplateId, "ESA");

        String formattedString = LocalDate.now().format(DateTimeFormatter.ofPattern(AppConstants.RESPONSE_DATE_FORMAT));

        assertNotificationBodyContains(
            notifications,
            hearingReminderEmailTemplateId,
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
            hearingReminderSmsTemplateId,
            "ESA",
            "reminder",
            formattedString,
            "11:59 PM",
            "AB12 0HN",
            "/abouthearing"
        );
    }

    @Test
    public void shouldSendAppointeeNotificationsWhenHearingBookedEventIsReceived() throws IOException, NotificationClientException {

        addAppointee(caseData, APPOINTEE_TYA);
        addHearing(caseData, 0);
        triggerEvent(HEARING_BOOKED_NOTIFICATION);
        simulateCcdCallback(HEARING_BOOKED_NOTIFICATION,
                "appointee/" + HEARING_BOOKED_NOTIFICATION.getId() + "Callback.json");

        List<Notification> notifications =
                tryFetchNotificationsForTestCase(
                        hearingReminderEmailTemplateId,
                        hearingReminderEmailTemplateId,
                        hearingReminderSmsTemplateId,
                        hearingReminderSmsTemplateId
                );

        assertNotificationSubjectContains(notifications, hearingReminderEmailTemplateId, "ESA");

        String formattedString = LocalDate.now().format(DateTimeFormatter.ofPattern(AppConstants.RESPONSE_DATE_FORMAT));

        assertNotificationBodyContains(
                notifications,
                hearingReminderEmailTemplateId,
                caseReference,
                DEAR_APPOINTEE_USER,
                AS_APPOINTEE_FOR,
                "ESA",
                "reminder",
                formattedString,
                "11:59 PM",
                "AB12 0HN",
                "/abouthearing/" + APPOINTEE_TYA
        );

        assertNotificationBodyContains(
                notifications,
                hearingReminderSmsTemplateId,
                "ESA",
                "reminder",
                formattedString,
                "11:59 PM",
                "AB12 0HN",
                "/abouthearing/" + APPOINTEE_TYA
        );
    }

    private static void addAppointee(SscsCaseData response, String tya) {
        Appointee appointee = Appointee.builder()
                .name(Name.builder()
                        .firstName(APPOINTEE_FIRST_NAME)
                        .lastName(APPOINTEE_LAST_NAME)
                        .build())
                .build();

        response.getAppeal().getAppellant().setAppointee(appointee);
    }

}
