package uk.gov.hmcts.sscs.functional;

import static helper.EnvironmentProfileValueSource.getEnvOrEmpty;
import static helper.FunctionalTestHelper.generateRandomCaseReference;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.sscs.CcdResponseUtils.buildCcdResponse;

import helper.EnvironmentProfileValueSource;
import io.restassured.RestAssured;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import uk.gov.hmcts.sscs.service.idam.IdamService;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
@ProfileValueSourceConfiguration(EnvironmentProfileValueSource.class)
public class NotificationsFunctionalTest {

    private static final org.slf4j.Logger LOG = getLogger(NotificationsFunctionalTest.class);

    private String caseReference;

    private Long caseId;

    @Autowired
    private CreateCcdService createCcdService;

    @Autowired
    private IdamService idamService;

    private IdamTokens idamTokens;

    @Value("${notification.appealReceived.emailId}")
    private String appealReceivedEmailTemplateId;

    @Value("${notification.appealReceived.smsId}")
    private String appealReceivedSmsTemplateId;

    private static final int MAX_SECONDS_TO_WAIT_FOR_NOTIFICATIONS = 120;

    @Autowired
    @Qualifier("testNotificationClient")
    private NotificationClient client;

    @Before
    public void setup() {

        String oauth2Token = idamService.getIdamOauth2Token();
        idamTokens = IdamTokens.builder()
                .idamOauth2Token(oauth2Token)
                .serviceAuthorization(idamService.generateServiceAuthorization())
                .userId(idamService.getUserId(oauth2Token))
                .build();

        createCase();
    }

    private void createCase() {

        caseReference = generateRandomCaseReference();

        CcdResponse caseData = buildCcdResponse(caseReference, "Yes", "Yes", EventType.SYA_APPEAL_CREATED);

        CaseDetails caseDetails = createCcdService.create(caseData, idamTokens);

        assertNotNull(caseDetails);
        caseId = caseDetails.getId();
    }

    @Test
    public void appealReceivedNotification() throws IOException, NotificationClientException {
        simulateCcdCallback(EventType.APPEAL_RECEIVED);

        tryFetchNotificationsForTestCase(
                appealReceivedEmailTemplateId,
                appealReceivedSmsTemplateId
        );
    }

    private void simulateCcdCallback(EventType eventType) throws IOException {

        final String callbackUrl = getEnvOrEmpty("TEST_URL") + "/send";

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

        boolean isTimeoutExceeded = false;
        int maxSecondsToWaitForNotification = MAX_SECONDS_TO_WAIT_FOR_NOTIFICATIONS;

        do {

            LOG.info("Waiting for all test case notifications to be delivered "
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

            maxSecondsToWaitForNotification -= 5;

            if (!isTimeoutExceeded) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    // noop
                }
            }

            allNotifications = client.getNotifications("", "", caseReference, "").getNotifications();

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

                return matchingNotifications;
            }

        } while (true);
    }


}
