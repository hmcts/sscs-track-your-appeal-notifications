package uk.gov.hmcts.sscs.functional;

import static org.junit.Assert.*;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.sscs.CcdResponseUtils.addHearing;
import static uk.gov.hmcts.sscs.CcdResponseUtils.buildCcdResponse;
import static uk.gov.hmcts.sscs.domain.notify.EventType.DWP_RESPONSE_RECEIVED;
import static uk.gov.hmcts.sscs.domain.notify.EventType.HEARING_BOOKED;

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
    public void shouldSendEvidenceReminderNotification() throws IOException, NotificationClientException {

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
                evidenceReminderSmsTemplateId
            );

        assertNotificationTemplateSubjectContains(notifications, evidenceReminderEmailTemplateId, "ESA benefit appeal");
        assertNotificationTemplateBodyContains(notifications, evidenceReminderEmailTemplateId, testCaseReference);
        assertNotificationTemplateBodyContains(notifications, evidenceReminderEmailTemplateId, "User Test");
        assertNotificationTemplateBodyContains(notifications, evidenceReminderEmailTemplateId, "ESA benefit appeal");
        assertNotificationTemplateBodyContains(notifications, evidenceReminderEmailTemplateId, "/evidence");

        assertNotificationTemplateBodyContains(notifications, evidenceReminderSmsTemplateId, "ESA benefit appeal");
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

        assertNotificationTemplateSubjectContains(notifications, hearingReminderEmailTemplateId, "ESA benefit appeal");
        assertNotificationTemplateBodyContains(notifications, hearingReminderEmailTemplateId, "reminder");

        assertNotificationTemplateBodyContains(notifications, hearingReminderSmsTemplateId, "reminder");
    }

    private void assertNotificationTemplate(
        List<Notification> notifications,
        String templateId
    ) {
        String observedTemplateIds =
            notifications
                .stream()
                .map(notification -> notification.getTemplateId().toString())
                .collect(Collectors.joining("\n"));

        assertThat(
            "Notification template " + templateId + " was sent",
            observedTemplateIds,
            CoreMatchers.containsString(templateId)
        );
    }

    private void assertNotificationTemplateSubjectContains(
        List<Notification> notifications,
        String templateId,
        String match
    ) {
        assertNotificationTemplate(notifications, templateId);

        notifications
            .stream()
            .filter(notification -> notification.getTemplateId().equals(UUID.fromString(templateId)))
            .forEach(notification ->
                assertThat(
                    "Notification template " + templateId + " [subject] contains '" + match + "'",
                    notification.getSubject().orElse(""),
                    CoreMatchers.containsString(match)
                )
            );
    }

    private void assertNotificationTemplateBodyContains(
        List<Notification> notifications,
        String templateId,
        String match
    ) {
        assertNotificationTemplate(notifications, templateId);

        notifications
            .stream()
            .filter(notification -> notification.getTemplateId().equals(UUID.fromString(templateId)))
            .forEach(notification ->
                assertThat(
                    "Notification template " + templateId + " [body] contains '" + match + "'",
                    notification.getBody(),
                    CoreMatchers.containsString(match)
                )
            );
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
