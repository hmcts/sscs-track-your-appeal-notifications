package uk.gov.hmcts.sscs.functional;

import static org.junit.Assert.*;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.sscs.CcdResponseUtils.addHearing;
import static uk.gov.hmcts.sscs.CcdResponseUtils.buildCcdResponse;
import static uk.gov.hmcts.sscs.config.AppConstants.RESPONSE_DATE_FORMAT;
import static uk.gov.hmcts.sscs.domain.notify.EventType.DWP_RESPONSE_RECEIVED;
import static uk.gov.hmcts.sscs.domain.notify.EventType.HEARING_BOOKED;

import helper.EnvironmentProfileValueSource;
import io.restassured.RestAssured;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
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
import uk.gov.hmcts.sscs.domain.idam.IdamTokens;
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

    private CcdResponse caseData;
    private IdamTokens idamTokens;
    private Long caseId;

    @Value("${notification.evidenceReminder.emailId}")
    private String evidenceReminderEmailTemplateId;

    @Value("${notification.evidenceReminder.smsId}")
    private String evidenceReminderSmsTemplateId;

    @Value("${notification.hearingReminder.emailId}")
    private String hearingReminderEmailTemplateId;

    @Value("${notification.hearingReminder.smsId}")
    private String hearingReminderSmsTemplateId;

    @Value("${notification.hearingHoldingReminder.emailId}")
    private String hearingHoldingReminderEmailTemplateId;

    @Value("${notification.hearingHoldingReminder.smsId}")
    private String hearingHoldingReminderSmsTemplateId;

    @Value("${notification.finalHearingHoldingReminder.emailId}")
    private String finalHearingHoldingReminderEmailTemplateId;

    @Value("${notification.finalHearingHoldingReminder.smsId}")
    private String finalHearingHoldingReminderSmsTemplateId;

    @Autowired
    private NotificationClient client;

    String testCaseReference;

    private static final int MAX_SECONDS_TO_WAIT_FOR_NOTIFICATIONS = 120;

    public void setup(EventType eventType) {

        String epoch = String.valueOf(Instant.now().toEpochMilli());
        testCaseReference =
            "SC"
            + epoch.substring(3, 6)
            + "/"
            + epoch.substring(6, 8)
            + "/"
            + epoch.substring(8, 13);

        caseData = buildCcdResponse(testCaseReference, "Yes", "Yes", eventType);

        String oauth2Token = idamService.getIdamOauth2Token();
        idamTokens = IdamTokens.builder()
            .idamOauth2Token(oauth2Token)
            .serviceAuthorization(idamService.generateServiceAuthorization())
            .userId(idamService.getUserId(oauth2Token))
            .build();

        CaseDetails caseDetails = createCcdService.create(caseData, idamTokens);

        assertNotNull(caseDetails);
        caseId = caseDetails.getId();

        LOG.info("Built case with ID: " + caseId + " and reference: " + testCaseReference);
    }

    @Test
    public void shouldSendEvidenceReminderAndHearingHoldingNotification() throws IOException, NotificationClientException {

        setup(DWP_RESPONSE_RECEIVED);

        CaseDetails updatedCaseDetails = updateCcdService.update(caseData, caseId, DWP_RESPONSE_RECEIVED.getId(), idamTokens);
        if (isPreviewEnv()) {
            simulateCcdCallback(DWP_RESPONSE_RECEIVED);
        } else {
            assertEquals("COMPLETED", updatedCaseDetails.getCallbackResponseStatus());
        }

        List<Notification> notifications =
            tryFetchNotificationsForTestCase(
                evidenceReminderEmailTemplateId,
                evidenceReminderSmsTemplateId,
                hearingHoldingReminderEmailTemplateId,
                hearingHoldingReminderEmailTemplateId,
                hearingHoldingReminderEmailTemplateId,
                hearingHoldingReminderSmsTemplateId,
                hearingHoldingReminderSmsTemplateId,
                hearingHoldingReminderSmsTemplateId,
                finalHearingHoldingReminderEmailTemplateId,
                finalHearingHoldingReminderSmsTemplateId
            );

        final String todayPlus6Weeks = LocalDate.now().plusWeeks(6).format(DateTimeFormatter.ofPattern(RESPONSE_DATE_FORMAT));

        assertNotificationSubjectContains(notifications, evidenceReminderEmailTemplateId, "ESA benefit appeal");
        assertNotificationBodyContains(
            notifications,
            evidenceReminderEmailTemplateId,
            testCaseReference,
            "User Test",
            "ESA benefit",
            "/evidence"
        );

        assertNotificationBodyContains(notifications, evidenceReminderSmsTemplateId, "ESA benefit appeal");

        assertNotificationSubjectContains(notifications, hearingHoldingReminderEmailTemplateId, "ESA benefit appeal");
        assertNotificationBodyContains(
            notifications,
            hearingHoldingReminderEmailTemplateId,
            testCaseReference,
            "User Test",
            "ESA benefit",
            "not been booked",
            "/trackyourappeal",
            todayPlus6Weeks
        );

        assertNotificationBodyContains(
            notifications,
            hearingHoldingReminderSmsTemplateId,
            "ESA benefit",
            "not been booked",
            "/trackyourappeal",
            todayPlus6Weeks
        );

        assertNotificationSubjectContains(notifications, finalHearingHoldingReminderEmailTemplateId, "ESA benefit appeal");
        assertNotificationBodyContains(
            notifications,
            finalHearingHoldingReminderEmailTemplateId,
            testCaseReference,
            "User Test",
            "ESA benefit",
            "not been booked",
            "/trackyourappeal"
        );

        assertNotificationBodyContains(
            notifications,
            finalHearingHoldingReminderSmsTemplateId,
            "ESA benefit",
            "not been booked",
            "/trackyourappeal"
        );
    }

    @Test
    public void shouldSendHearingReminderNotification() throws IOException, NotificationClientException {

        setup(HEARING_BOOKED);
        addHearing(caseData);

        CaseDetails updatedCaseDetails = updateCcdService.update(caseData, caseId, HEARING_BOOKED.getId(), idamTokens);
        if (isPreviewEnv()) {
            simulateCcdCallback(HEARING_BOOKED);
        } else {
            assertEquals("COMPLETED", updatedCaseDetails.getCallbackResponseStatus());
        }

        List<Notification> notifications =
            tryFetchNotificationsForTestCase(
                hearingReminderEmailTemplateId,
                hearingReminderEmailTemplateId,
                hearingReminderSmsTemplateId,
                hearingReminderSmsTemplateId
            );

        assertNotificationSubjectContains(notifications, hearingReminderEmailTemplateId, "ESA benefit appeal");
        assertNotificationBodyContains(
            notifications,
            hearingReminderEmailTemplateId,
            testCaseReference,
            "ESA benefit",
            "reminder",
            "AB12 0HN",
            "/abouthearing"
        );

        assertNotificationBodyContains(
            notifications,
            hearingReminderSmsTemplateId,
            "ESA benefit",
            "reminder"
        );
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

        LOG.info("Is preview environment -- simulating a CCD callback to: " + callbackUrl + " for case " + testCaseReference);

        String path = getClass().getClassLoader().getResource("json/ccdResponse.json").getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        json = json.replace("appealReceived", eventType.getId());
        json = json.replace("12345656789", caseId.toString());
        json = json.replace("SC022/14/12423", testCaseReference);

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

        int maxSecondsToWaitForNotification = MAX_SECONDS_TO_WAIT_FOR_NOTIFICATIONS;

        do {

            if (maxSecondsToWaitForNotification-- == 0) {

                String allTemplateIds =
                    allNotifications
                        .stream()
                        .map(notification -> notification.getTemplateId().toString())
                        .collect(Collectors.joining("\n"));

                throw new RuntimeException(
                    "Timed out fetching notifications after "
                    + MAX_SECONDS_TO_WAIT_FOR_NOTIFICATIONS
                    + " seconds. Template IDs:\n"
                    + allTemplateIds
                );
            }

            LOG.info(
                "Waiting for all test case notifications to be delivered "
                + "[" + matchingNotifications.size() + "/" + expectedTemplateIds.length + "] ..."
            );

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // noop
            }

            allNotifications =
                client
                    .getNotifications("", "", testCaseReference, "")
                    .getNotifications();

            matchingNotifications =
                allNotifications
                    .stream()
                    .filter(notification -> Arrays.asList(expectedTemplateIds).contains(notification.getTemplateId().toString()))
                    .collect(Collectors.toList());

            if (matchingNotifications.size() >= expectedTemplateIds.length) {

                for (Notification notification : matchingNotifications) {
                    assertFalse(notification.getStatus().contains("fail"));
                }

                LOG.info(
                    "Test case notifications have been delivered "
                    + "[" + matchingNotifications.size() + "/" + expectedTemplateIds.length + "]"
                );

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
