package uk.gov.hmcts.reform.sscs.functional;

import static uk.gov.hmcts.reform.sscs.SscsCaseDataUtils.addHearing;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.config.AppConstants;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientException;

public class ReminderNotificationsFunctionalTest extends AbstractFunctionalTest {

    @Value("${notification.dwpResponseLateReminder.emailId}")
    private String dwpResponseLateReminderEmailTemplateId;

    @Value("${notification.dwpResponseLateReminder.smsId}")
    private String dwpResponseLateReminderSmsTemplateId;

    @Value("${notification.oral.evidenceReminder.appellant.emailId}")
    private String evidenceReminderOralAppellantEmailTemplateId;

    @Value("${notification.oral.evidenceReminder.appellant.smsId}")
    private String evidenceReminderOralAppellantSmsTemplateId;

    @Value("${notification.oral.evidenceReminder.representative.emailId}")
    private String evidenceReminderOralRepresentativeEmailTemplateId;

    @Value("${notification.oral.evidenceReminder.representative.smsId}")
    private String evidenceReminderOralRepresentativeSmsTemplateId;

    @Value("${notification.oral.responseReceived.emailId}")
    private String responseReceivedOralEmailTemplateId;

    @Value("${notification.oral.responseReceived.smsId}")
    private String responseReceivedOralSmsTemplateId;

    @Value("${notification.paper.evidenceReminder.appellant.emailId}")
    private String evidenceReminderPaperAppellantEmailTemplateId;

    @Value("${notification.paper.evidenceReminder.appellant.smsId}")
    private String evidenceReminderPaperAppellantSmsTemplateId;

    @Value("${notification.paper.evidenceReminder.representative.emailId}")
    private String evidenceReminderPaperRepresentativeEmailTemplateId;

    @Value("${notification.paper.evidenceReminder.representative.smsId}")
    private String evidenceReminderPaperRepresentativeSmsTemplateId;

    @Value("${notification.paper.responseReceived.emailId}")
    private String responseReceivedPaperEmailTemplateId;

    @Value("${notification.paper.responseReceived.smsId}")
    private String responseReceivedPaperSmsTemplateId;

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
    public void shouldSendNotificationsWhenDwpResponseReceivedEventIsReceivedForOral() throws IOException, NotificationClientException {

        triggerEventWithHearingType(DWP_RESPONSE_RECEIVED_NOTIFICATION, "oral");
        simulateCcdCallback(DWP_RESPONSE_RECEIVED_NOTIFICATION,"representative/oral-" + DWP_RESPONSE_RECEIVED_NOTIFICATION.getId() + "Callback.json");

        List<Notification> notifications =
            tryFetchNotificationsForTestCase(
                responseReceivedOralEmailTemplateId,
                responseReceivedOralSmsTemplateId,
                evidenceReminderOralAppellantEmailTemplateId,
                evidenceReminderOralAppellantSmsTemplateId,
                evidenceReminderOralRepresentativeEmailTemplateId,
                evidenceReminderOralRepresentativeSmsTemplateId,
                firstHearingHoldingReminderEmailTemplateId,
                firstHearingHoldingReminderSmsTemplateId,
                secondHearingHoldingReminderEmailTemplateId,
                secondHearingHoldingReminderSmsTemplateId,
                thirdHearingHoldingReminderEmailTemplateId,
                thirdHearingHoldingReminderSmsTemplateId,
                finalHearingHoldingReminderEmailTemplateId,
                finalHearingHoldingReminderSmsTemplateId
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

        assertNotificationSubjectContains(notifications, responseReceivedOralEmailTemplateId, "ESA");
        assertNotificationBodyContains(
            notifications,
            responseReceivedOralEmailTemplateId,
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
            responseReceivedOralSmsTemplateId,
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
    public void shouldSendNotificationsWhenDwpResponseReceivedEventIsReceivedForPaper() throws IOException, NotificationClientException {

        triggerEventWithHearingType(DWP_RESPONSE_RECEIVED_NOTIFICATION, "paper");
        simulateCcdCallback(DWP_RESPONSE_RECEIVED_NOTIFICATION,"representative/paper-" + DWP_RESPONSE_RECEIVED_NOTIFICATION.getId() + "Callback.json");

        List<Notification> notifications =
            tryFetchNotificationsForTestCase(
                responseReceivedPaperEmailTemplateId,
                responseReceivedPaperSmsTemplateId,
                evidenceReminderPaperAppellantEmailTemplateId,
                evidenceReminderPaperAppellantSmsTemplateId,
                evidenceReminderPaperRepresentativeEmailTemplateId,
                evidenceReminderPaperRepresentativeSmsTemplateId,
                firstHearingHoldingReminderEmailTemplateId,
                firstHearingHoldingReminderSmsTemplateId,
                secondHearingHoldingReminderEmailTemplateId,
                secondHearingHoldingReminderSmsTemplateId,
                thirdHearingHoldingReminderEmailTemplateId,
                thirdHearingHoldingReminderSmsTemplateId,
                finalHearingHoldingReminderEmailTemplateId,
                finalHearingHoldingReminderSmsTemplateId
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

        assertNotificationSubjectContains(notifications, responseReceivedPaperEmailTemplateId, "ESA");
        assertNotificationBodyContains(
                notifications,
                responseReceivedPaperEmailTemplateId,
                caseReference,
                "User Test",
                "ESA",
                "DWP",
                "response",
                "/trackyourappeal",
                "9 April 2016"
        );

        assertNotificationBodyContains(
                notifications,
                responseReceivedPaperSmsTemplateId,
                "ESA",
                "DWP",
                "response",
                "/trackyourappeal",
                "9 April 2016"
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
}
