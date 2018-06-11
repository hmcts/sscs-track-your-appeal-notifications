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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
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

        idamTokens = IdamTokens.builder()
            .authenticationService(idamService.generateServiceAuthorization())
            .idamOauth2Token(idamService.getIdamOauth2Token())
            .build();

        CaseDetails caseDetails = createCcdService.create(caseData, idamTokens);

        assertNotNull(caseDetails);
        caseId = caseDetails.getId();

        LOG.info("Built case with ID: " + caseId + " and reference: " + testCaseReference);
    }

    @Test
    public void shouldSendEvidenceReceivedAndHearingHoldingNotification() throws IOException, NotificationClientException {

        final int expectedEmailNotifications = 6;
        final int expectedSmsNotifications = 5;

        setup(DWP_RESPONSE_RECEIVED);

        CaseDetails updatedCaseDetails = updateCcdService.update(caseData, caseId, DWP_RESPONSE_RECEIVED.getId(), idamTokens);
        if (isPreviewOrAatEnv()) {
            simulateCcdCallback(DWP_RESPONSE_RECEIVED);
        } else {
            assertEquals("COMPLETED", updatedCaseDetails.getCallbackResponseStatus());
        }

        final String todayPlus6Weeks = LocalDate.now().plusWeeks(6).toString();

        Optional<Pair<List<Notification>, List<Notification>>> notifications =
            tryFetchNotificationsForTestCase(expectedEmailNotifications, expectedSmsNotifications);

        assertNotificationCount(notifications, evidenceReminderEmailTemplateId, 1);
        assertNotificationSubjectContains(notifications, evidenceReminderEmailTemplateId, "ESA benefit appeal");
        assertNotificationBodyContains(
            notifications,
            evidenceReminderEmailTemplateId,
            testCaseReference,
            "User Test",
            "ESA benefit appeal",
            "/evidence"
        );

        assertNotificationCount(notifications, evidenceReminderSmsTemplateId, 1);
        assertNotificationBodyContains(notifications, evidenceReminderSmsTemplateId, "ESA benefit appeal");

        assertNotificationCount(notifications, hearingHoldingReminderEmailTemplateId, 3);
        assertNotificationSubjectContains(notifications, hearingHoldingReminderEmailTemplateId, "ESA benefit appeal");
        assertNotificationBodyContains(
            notifications,
            hearingHoldingReminderEmailTemplateId,
            testCaseReference,
            "User Test",
            "ESA benefit appeal",
            "/trackyourappeal",
            todayPlus6Weeks
        );

        assertNotificationCount(notifications, hearingHoldingReminderSmsTemplateId, 3);
        assertNotificationBodyContains(notifications, hearingHoldingReminderSmsTemplateId, "ESA benefit appeal");

        assertNotificationCount(notifications, finalHearingHoldingReminderEmailTemplateId, 1);
        assertNotificationSubjectContains(notifications, finalHearingHoldingReminderEmailTemplateId, "ESA benefit appeal");
        assertNotificationBodyContains(
            notifications,
            finalHearingHoldingReminderEmailTemplateId,
            testCaseReference,
            "User Test",
            "ESA benefit appeal",
            "/trackyourappeal"
        );

        assertNotificationCount(notifications, finalHearingHoldingReminderSmsTemplateId, 1);
        assertNotificationBodyContains(notifications, finalHearingHoldingReminderSmsTemplateId, "ESA benefit appeal");
    }

    @Test
    public void shouldSendHearingReminderNotification() throws IOException, NotificationClientException {

        final int expectedEmailNotifications = 3;
        final int expectedSmsNotifications = 2;

        setup(HEARING_BOOKED);
        addHearing(caseData);

        CaseDetails updatedCaseDetails = updateCcdService.update(caseData, caseId, HEARING_BOOKED.getId(), idamTokens);
        if (isPreviewOrAatEnv()) {
            simulateCcdCallback(HEARING_BOOKED);
        } else {
            assertEquals("COMPLETED", updatedCaseDetails.getCallbackResponseStatus());
        }

        Optional<Pair<List<Notification>, List<Notification>>> notifications =
            tryFetchNotificationsForTestCase(expectedEmailNotifications, expectedSmsNotifications);

        assertNotificationCount(notifications, hearingReminderEmailTemplateId, 2);
        assertNotificationSubjectContains(notifications, hearingReminderEmailTemplateId, "ESA benefit appeal");
        assertNotificationBodyContains(
            notifications,
            hearingReminderEmailTemplateId,
            testCaseReference,
            "AB12 0HN",
            "/abouthearing"
        );

        assertNotificationCount(notifications, hearingReminderSmsTemplateId, 2);
        assertNotificationBodyContains(notifications, hearingReminderSmsTemplateId, "ESA benefit appeal");
    }

    private void assertNotificationCount(
        Optional<Pair<List<Notification>, List<Notification>>> notifications,
        String templateId,
        int expectedCount
    ) {
        assertTrue(notifications.isPresent());
        assertEquals(
            "Notification template was sent",
            expectedCount,
            Stream.concat(
                notifications.get().getLeft().stream(),
                notifications.get().getRight().stream()
            ).filter(notification ->
                notification.getTemplateId().equals(UUID.fromString(templateId))
            ).count()
        );
    }

    private void assertNotificationSubjectContains(
        Optional<Pair<List<Notification>, List<Notification>>> notifications,
        String templateId,
        String... matches
    ) {
        String bodies =
            Stream
                .concat(
                    notifications.get().getLeft().stream(),
                    notifications.get().getRight().stream()
                )
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
        Optional<Pair<List<Notification>, List<Notification>>> notifications,
        String templateId,
        String... matches
    ) {
        String bodies =
            Stream
                .concat(
                    notifications.get().getLeft().stream(),
                    notifications.get().getRight().stream()
                )
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

    private boolean isPreviewOrAatEnv() {
        final String testUrl = getEnvOrEmpty("TEST_URL");
        return testUrl.contains("preview.internal") || testUrl.contains("aat.internal");
    }

    private void simulateCcdCallback(EventType eventType) throws IOException {

        /*
         this method simulates the ccd callback in preview & aat,
         because ccd callbacks cannot be configured in preview env
         */

        final String callbackUrl = getEnvOrEmpty("TEST_URL") + "/send";

        LOG.info("Is preview or AAT environment -- simulating a CCD callback to: " + callbackUrl + " for case " + testCaseReference);

        String path = getClass().getClassLoader().getResource("json/ccdResponse.json").getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        json = json.replace("appealReceived", eventType.getId());
        json = json.replace("12345656789", caseId.toString());
        json = json.replace("SC022/14/12423", testCaseReference);

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

    private Optional<Pair<List<Notification>, List<Notification>>> tryFetchNotificationsForTestCase(
        int expectedEmailNotifications,
        int expectedSmsNotifications
    ) throws NotificationClientException {

        List<Notification> emailNotifications = new ArrayList<>();
        List<Notification> smsNotifications = new ArrayList<>();

        int maxSecondsToWaitForNotification = MAX_SECONDS_TO_WAIT_FOR_NOTIFICATIONS;

        do {

            if (maxSecondsToWaitForNotification-- == 0) {
                throw new RuntimeException(
                    "Timed out fetching notifications after " + MAX_SECONDS_TO_WAIT_FOR_NOTIFICATIONS + " seconds"
                );
            }

            LOG.info(
                "Waiting for all test case notifications to be delivered "
                + "[" + emailNotifications.size() + "/" + expectedEmailNotifications + "] "
                + "[" + smsNotifications.size() + "/" + expectedSmsNotifications + "] ..."
            );

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // noop
            }

            emailNotifications =
                client
                    .getNotifications("", "email", testCaseReference, "")
                    .getNotifications();

            smsNotifications =
                client
                    .getNotifications("", "sms", testCaseReference, "")
                    .getNotifications();

            if (emailNotifications.size() >= expectedEmailNotifications
                && smsNotifications.size() >= expectedSmsNotifications) {

                for (Notification n : emailNotifications) {
                    assertFalse(n.getStatus().contains("fail"));
                }

                for (Notification n : smsNotifications) {
                    assertFalse(n.getStatus().contains("fail"));
                }

                return Optional.of(
                    Pair.of(
                        emailNotifications,
                        smsNotifications
                    )
                );
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
