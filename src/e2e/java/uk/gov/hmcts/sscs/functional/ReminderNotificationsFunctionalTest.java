package uk.gov.hmcts.sscs.functional;

import static org.junit.Assert.*;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.sscs.CcdResponseUtils.addHearing;
import static uk.gov.hmcts.sscs.CcdResponseUtils.buildCcdResponse;
import static uk.gov.hmcts.sscs.domain.notify.EventType.*;

import helper.EnvironmentProfileValueSource;
import io.restassured.RestAssured;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.ProfileValueSourceConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.Events;
import uk.gov.hmcts.sscs.domain.idam.IdamTokens;
import uk.gov.hmcts.sscs.domain.notify.Event;
import uk.gov.hmcts.sscs.domain.notify.EventType;
import uk.gov.hmcts.sscs.service.ccd.CreateCcdService;
import uk.gov.hmcts.sscs.service.ccd.UpdateCcdService;
import uk.gov.hmcts.sscs.service.idam.IdamService;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
@ProfileValueSourceConfiguration(EnvironmentProfileValueSource.class)
public class ReminderNotificationsFunctionalTest {

    private static final org.slf4j.Logger LOG = getLogger(ReminderNotificationsFunctionalTest.class);

    @Autowired
    private CreateCcdService createCcdService;
    @Autowired
    private UpdateCcdService updateCcdService;

    @Autowired
    private IdamService idamService;

    @Autowired
    private NotificationClient client;

    private CcdResponse caseData;
    private IdamTokens idamTokens;
    private Long caseId;
    private String caseReference;

    @Value("${notification.dwpResponseLateReminder.emailId}")
    private String dwpResponseLateReminderEmailTemplateId;

    @Value("${notification.dwpResponseLateReminder.smsId}")
    private String dwpResponseLateReminderSmsTemplateId;

    @Value("${notification.evidenceReminder.emailId}")
    private String evidenceReminderEmailTemplateId;

    @Value("${notification.evidenceReminder.smsId}")
    private String evidenceReminderSmsTemplateId;

    @Value("${notification.responseReceived.emailId}")
    private String responseReceivedEmailTemplateId;

    @Value("${notification.responseReceived.smsId}")
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

    private static final int MAX_SECONDS_TO_WAIT_FOR_NOTIFICATIONS = 120;

    @Before
    public void setup() {

        String oauth2Token = idamService.getIdamOauth2Token();
        idamTokens = IdamTokens.builder()
            .idamOauth2Token(oauth2Token)
            .serviceAuthorization(idamService.generateServiceAuthorization())
            .userId(idamService.getUserId(oauth2Token))
            .build();
    }

    private void createCase(EventType eventType) {

        String epoch = String.valueOf(Instant.now().toEpochMilli());
        caseReference =
            "SC"
            + epoch.substring(3, 6)
            + "/"
            + epoch.substring(6, 8)
            + "/"
            + epoch.substring(8, 13);

        caseData = buildCcdResponse(caseReference, "Yes", "Yes", eventType);
        CaseDetails caseDetails = createCcdService.create(caseData, idamTokens);

        assertNotNull(caseDetails);
        caseId = caseDetails.getId();

        LOG.info("Built case with id: " + caseId + " and reference: " + caseReference);
    }

    @Test
    public void shouldSendNotificationsWhenAppealReceivedEventIsReceived() throws IOException, NotificationClientException {

        createCase(SYA_APPEAL_CREATED);
        triggerEvent(APPEAL_RECEIVED);

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

        createCase(SYA_APPEAL_CREATED);
        triggerEvent(DWP_RESPONSE_RECEIVED);

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
            "ESA benefit",
            "DWP",
            "response",
            "/trackyourappeal",
            "12 March 2016"
        );

        assertNotificationBodyContains(
            notifications,
            responseReceivedSmsTemplateId,
            "ESA benefit",
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

        assertNotificationSubjectContains(notifications, secondHearingHoldingReminderEmailTemplateId, "ESA benefit appeal");
        assertNotificationBodyContains(
            notifications,
            secondHearingHoldingReminderEmailTemplateId,
            caseReference,
            "User Test",
            "ESA benefit",
            "not been booked",
            "/trackyourappeal",
            "04 June 2016"
        );

        assertNotificationBodyContains(
            notifications,
            secondHearingHoldingReminderSmsTemplateId,
            "ESA benefit",
            "not been booked",
            "/trackyourappeal",
            "04 June 2016"
        );

        assertNotificationSubjectContains(notifications, thirdHearingHoldingReminderEmailTemplateId, "ESA benefit appeal");
        assertNotificationBodyContains(
            notifications,
            thirdHearingHoldingReminderEmailTemplateId,
            caseReference,
            "User Test",
            "ESA benefit",
            "not been booked",
            "/trackyourappeal",
            "16 July 2016"
        );

        assertNotificationBodyContains(
            notifications,
            thirdHearingHoldingReminderSmsTemplateId,
            "ESA benefit",
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

        createCase(SYA_APPEAL_CREATED);
        addHearing(caseData);
        triggerEvent(HEARING_BOOKED);

        List<Notification> notifications =
            tryFetchNotificationsForTestCase(
                hearingReminderEmailTemplateId,
                hearingReminderEmailTemplateId,
                hearingReminderSmsTemplateId,
                hearingReminderSmsTemplateId
            );

        assertNotificationSubjectContains(notifications, hearingReminderEmailTemplateId, "ESA");
        assertNotificationBodyContains(
            notifications,
            hearingReminderEmailTemplateId,
            caseReference,
            "ESA",
            "reminder",
            "01 January 2016",
            "12:00 PM",
            "AB12 0HN",
            "/abouthearing"
        );

        assertNotificationBodyContains(
            notifications,
            hearingReminderSmsTemplateId,
            "ESA",
            "reminder",
            "01 January 2016",
            "12:00 PM",
            "AB12 0HN",
            "/abouthearing"
        );
    }

    private void triggerEvent(EventType eventType) throws IOException {

        Events events = Events.builder()
            .value(Event.builder()
                .type(eventType.getId())
                .description(eventType.getId())
                .date("2016-01-16T12:34:56.789")
                .build())
            .build();

        List<Events> allEvents = new ArrayList<>(caseData.getEvents());
        allEvents.add(events);
        caseData.setEvents(allEvents);

        CaseDetails updatedCaseDetails = updateCcdService.update(caseData, caseId, eventType.getId(), idamTokens);
        if (isPreviewEnv()) {
            simulateCcdCallback(eventType);
        } else {
            assertEquals("COMPLETED", updatedCaseDetails.getCallbackResponseStatus());
        }
    }

    private void assertNotificationSubjectContains(
        List<Notification> notifications,
        String templateId,
        String... matches
    ) {
        String bodies =
            notifications
                .stream()
                .filter(notification -> notification.getTemplateId().equals(UUID.fromString(templateId)))
                .filter(notification -> notification.getSubject().isPresent())
                .map(notification -> notification.getSubject().get())
                .collect(Collectors.joining("\n--\n"));

        for (String match : matches) {

            Assert.assertThat(
                "Notification template " + templateId + " [subject] contains '" + match + "'",
                bodies,
                CoreMatchers.containsString(match)
            );
        }
    }

    private void assertNotificationBodyContains(
        List<Notification> notifications,
        String templateId,
        String... matches
    ) {
        String bodies =
            notifications
                .stream()
                .filter(notification -> notification.getTemplateId().equals(UUID.fromString(templateId)))
                .map(notification -> notification.getBody())
                .collect(Collectors.joining("\n--\n"));

        for (String match : matches) {

            Assert.assertThat(
                "Notification template " + templateId + " [body] contains '" + match + "'",
                bodies,
                CoreMatchers.containsString(match)
            );
        }
    }

    private boolean isPreviewEnv() {
        final String testUrl = getEnvOrEmpty("TEST_URL");
        return testUrl.contains("preview.internal");
    }

    private void simulateCcdCallback(EventType eventType) throws IOException {

        /*
         this method simulates the ccd callback in preview,
         because ccd callbacks cannot be configured in this environment
         */

        final String callbackUrl = getEnvOrEmpty("TEST_URL") + "/send";

        LOG.info("Is preview environment -- simulating a CCD callback to: " + callbackUrl + " for case " + caseReference);

        String resource = eventType.getId() + "Callback.json";
        String path = getClass().getClassLoader().getResource(resource).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        json = json.replace("12345656789", caseId.toString());
        json = json.replace("SC022/14/12423", caseReference);

        RestAssured.useRelaxedHTTPSValidation();
        RestAssured
            .given()
            .header("ServiceAuthorization", "" + idamTokens.getServiceAuthorization())
            .contentType("application/json")
            .body(json)
            .when()
            .post(callbackUrl)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    private List<Notification> tryFetchNotificationsForTestCase(
        String... expectedTemplateIds
    ) throws NotificationClientException {

        List<Notification> allNotifications = new ArrayList<>();
        List<Notification> matchingNotifications = new ArrayList<>();

        int waitForAtLeastNumberOfNotifications = expectedTemplateIds.length;

        if (isPreviewEnv()) {
            // aat staging slot will also send a set of notifications
            // because aat ccd callbacks go there. the notifications from
            // aat staging slot will be from an older version.
            waitForAtLeastNumberOfNotifications *= 2;
        }

        boolean isTimeoutExceeded = false;
        int maxSecondsToWaitForNotification = 180;

        do {

            LOG.info(
                "Waiting for all test case notifications to be delivered "
                + "[" + matchingNotifications.size() + "/" + waitForAtLeastNumberOfNotifications + "] ..."
            );

            if (maxSecondsToWaitForNotification <= 0) {

                isTimeoutExceeded = true;

                String allTemplateIds =
                    allNotifications
                        .stream()
                        .map(notification -> notification.getTemplateId().toString())
                        .collect(Collectors.joining("\n"));

                LOG.info(
                    "Timed out fetching notifications after "
                    + MAX_SECONDS_TO_WAIT_FOR_NOTIFICATIONS
                    + " seconds. Template IDs:\n"
                    + allTemplateIds
                );
            }

            maxSecondsToWaitForNotification -= 10;

            if (!isTimeoutExceeded) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    // noop
                }
            }

            allNotifications =
                client
                    .getNotifications("", "", caseReference, "")
                    .getNotifications();

            matchingNotifications =
                allNotifications
                    .stream()
                    .filter(notification -> Arrays.asList(expectedTemplateIds).contains(notification.getTemplateId().toString()))
                    .collect(Collectors.toList());

            if (matchingNotifications.size() >= waitForAtLeastNumberOfNotifications
                || isTimeoutExceeded) {

                for (Notification notification : matchingNotifications) {
                    assertFalse(notification.getStatus().contains("fail"));
                }

                LOG.info(
                    "Test case notifications have been delivered "
                    + "[" + matchingNotifications.size() + "/" + waitForAtLeastNumberOfNotifications + "]"
                );

                if (!getEnvOrEmpty("DISPLAY_NOTIFICATIONS").isEmpty()) {

                    String bodies =
                        matchingNotifications
                            .stream()
                            .map(notification -> notification.getId().toString() + "\n" + notification.getBody())
                            .collect(Collectors.joining("\n--\n"));

                    LOG.info(matchingNotifications.size() + " bodies:\n" + bodies);
                }

                return matchingNotifications;
            }

        } while (true);
    }

    private String getEnvOrEmpty(
        String name
    ) {
        String value = System.getenv(name);
        if (value == null) {
            return "";
        }

        return value;
    }

}
