package uk.gov.hmcts.sscs.functional;

import static org.junit.Assert.*;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.sscs.CcdResponseUtils.addHearing;
import static uk.gov.hmcts.sscs.CcdResponseUtils.buildCcdResponse;
import static uk.gov.hmcts.sscs.domain.notify.EventType.DWP_RESPONSE_RECEIVED;
import static uk.gov.hmcts.sscs.domain.notify.EventType.HEARING_BOOKED;

import io.restassured.RestAssured;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
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

    @Autowired
    private NotificationClient client;

    String testCaseReference;

    private static int EXPECTED_EMAIL_NOTIFICATIONS;
    private static int EXPECTED_SMS_NOTIFICATIONS;
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

        idamTokens = IdamTokens.builder()
            .authenticationService(idamService.generateServiceAuthorization())
            .idamOauth2Token(idamService.getIdamOauth2Token())
            .build();

        CaseDetails caseDetails = createCcdService.create(caseData, idamTokens);

        assertNotNull(caseDetails);
        caseId = caseDetails.getId();
    }

    @Test
    public void shouldSendResponseReceivedNotification() throws IOException, NotificationClientException {
        EXPECTED_EMAIL_NOTIFICATIONS = 2;
        EXPECTED_SMS_NOTIFICATIONS = 1;

        setup(DWP_RESPONSE_RECEIVED);

        CaseDetails updatedCaseDetails = updateCcdService.update(caseData, caseId, DWP_RESPONSE_RECEIVED.getId(), idamTokens);

        assertEquals("COMPLETED", updatedCaseDetails.getCallbackResponseStatus());

        ifPreviewEnvSimulateCcdCallback(DWP_RESPONSE_RECEIVED);

        assertNotificationsSent(evidenceReminderEmailTemplateId, evidenceReminderSmsTemplateId);
    }

    @Test
    public void shouldSendHearingReminderNotification() throws IOException, NotificationClientException {
        EXPECTED_EMAIL_NOTIFICATIONS = 3;
        EXPECTED_SMS_NOTIFICATIONS = 0;

        setup(HEARING_BOOKED);

        addHearing(caseData);

        CaseDetails updatedCaseDetails = updateCcdService.update(caseData, caseId, HEARING_BOOKED.getId(), idamTokens);

        assertEquals("COMPLETED", updatedCaseDetails.getCallbackResponseStatus());

        ifPreviewEnvSimulateCcdCallback(HEARING_BOOKED);

        assertNotificationsSent(hearingReminderEmailTemplateId, null);
    }

    /*
     this method simulates the ccd callback in preview,
     because ccd callbacks cannot be configured in preview env
     */
    private void ifPreviewEnvSimulateCcdCallback(EventType eventType) throws IOException {

        if (!getEnvOrEmpty("INFRASTRUCTURE_ENV").equals("preview")
            || getEnvOrEmpty("HTTP_HOST").isEmpty()) {
            LOG.info("Is *not* preview environment -- expecting CCD to callback");
            return;
        }

        final String previewHttpHost = getEnvOrEmpty("HTTP_HOST");
        final boolean isSecureConnection = getEnvOrEmpty("SERVER_PORT_SECURE").equals("1");
        final String callbackUrl = "http" + (isSecureConnection ? "s" : "") + "://" + previewHttpHost + "/send";

        LOG.info("Is preview environment -- simulating a CCD callback to: " + callbackUrl);

        String path = getClass().getClassLoader().getResource("ccdResponse.json").getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        json = json.replace("appealReceived", eventType.getId());
        json = json.replace("1527603347855358", caseId.toString());
        json = json.replace("SC760/33/47564", testCaseReference);
        json = json.replace("\r\n", "\n");

        RestAssured.useRelaxedHTTPSValidation();
        RestAssured
            .given()
            .header("ServiceAuthorization", "" + idamTokens.getAuthenticationService())
            .contentType("application/json")
            .body(json)
            .when()
            .post(callbackUrl)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    private void assertNotificationsSent(String emailTemplateId, String smsTemplateId) throws NotificationClientException {
        Optional<Pair<List<Notification>, List<Notification>>> notifications;

        int maxSecondsToWaitForNotification = MAX_SECONDS_TO_WAIT_FOR_NOTIFICATIONS;

        do {

            if (maxSecondsToWaitForNotification-- == 0) {
                throw new RuntimeException(
                        "Timed out fetching notifications after " + MAX_SECONDS_TO_WAIT_FOR_NOTIFICATIONS + " seconds"
                );
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // noop
            }

            notifications = tryFetchNotificationsForTestCase();

        } while (!notifications.isPresent());

        List<Notification> sentEmailNotifications = notifications.get().getLeft();

        assertTrue(sentEmailNotifications.size() >= EXPECTED_EMAIL_NOTIFICATIONS);

        assertTrue(
                sentEmailNotifications.stream()
                        .anyMatch(sentEmailNotification ->
                                sentEmailNotification.getTemplateId().equals(UUID.fromString(emailTemplateId))
                        )
        );

        assertTrue(
                sentEmailNotifications.stream()
                        .anyMatch(sentEmailNotification ->
                                sentEmailNotification.getBody().contains(testCaseReference)
                        )
        );

        if (smsTemplateId != null) {
            List<Notification> sentSmsNotifications = notifications.get().getRight();

            assertTrue(sentSmsNotifications.size() >= EXPECTED_SMS_NOTIFICATIONS);

            assertTrue(
                    sentSmsNotifications.stream()
                            .anyMatch(sentSmsNotification ->
                                    sentSmsNotification.getTemplateId().equals(UUID.fromString(smsTemplateId))
                            )
            );
        }
    }

    private Optional<Pair<List<Notification>, List<Notification>>> tryFetchNotificationsForTestCase() throws NotificationClientException {

        List<Notification> emailNotifications =
            client
                .getNotifications("delivered", "email", testCaseReference, "")
                .getNotifications();

        List<Notification> smsNotifications =
            client
                .getNotifications("delivered", "sms", testCaseReference, "")
                .getNotifications();

        if (emailNotifications.size() >= EXPECTED_EMAIL_NOTIFICATIONS
            && smsNotifications.size() >= EXPECTED_SMS_NOTIFICATIONS) {

            return Optional.of(
                Pair.of(
                    emailNotifications,
                    smsNotifications
                )
            );
        }

        LOG.info("Waiting for all test case notifications to be delivered...");

        return Optional.empty();
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
