package uk.gov.hmcts.sscs.functional;

import static helper.EnvironmentProfileValueSource.getEnvOrEmpty;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.sscs.CcdResponseUtils.buildCcdResponse;

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
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
public abstract class AbstractFunctionalTest {

    private static final org.slf4j.Logger LOG = getLogger(AbstractFunctionalTest.class);

    private static final int MAX_SECONDS_TO_WAIT_FOR_NOTIFICATIONS = 200;

    @Autowired
    @Qualifier("testNotificationClient")
    private NotificationClient client;

    @Autowired
    private IdamService idamService;

    private IdamTokens idamTokens;

    protected String caseReference;

    private Long caseId;

    protected CcdResponse caseData;

    @Autowired
    private CreateCcdService createCcdService;

    @Autowired
    private UpdateCcdService updateCcdService;

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

        caseData = buildCcdResponse(caseReference, "Yes", "Yes", EventType.SYA_APPEAL_CREATED);

        CaseDetails caseDetails = createCcdService.create(caseData, idamTokens);

        assertNotNull(caseDetails);
        caseId = caseDetails.getId();
    }

    protected static String generateRandomCaseReference() {
        String epoch = String.valueOf(Instant.now().toEpochMilli());
        return "SC" + epoch.substring(3, 6)
                    + "/"
                    + epoch.substring(6, 8)
                    + "/"
                    + epoch.substring(8, 13);
    }

    protected List<Notification> tryFetchNotificationsForTestCase(String... expectedTemplateIds) throws NotificationClientException {

        List<Notification> allNotifications = new ArrayList<>();
        List<Notification> matchingNotifications = new ArrayList<>();

        int waitForAtLeastNumberOfNotifications = expectedTemplateIds.length;

        int maxSecondsToWaitForNotification = MAX_SECONDS_TO_WAIT_FOR_NOTIFICATIONS;

        do {

            LOG.info("Waiting for all test case notifications to be delivered "
                    + "[" + matchingNotifications.size() + "/" + waitForAtLeastNumberOfNotifications + "] ..."
            );

            if (maxSecondsToWaitForNotification <= 0) {

                String allTemplateIds =
                        allNotifications
                                .stream()
                                .map(notification -> notification.getTemplateId().toString())
                                .collect(Collectors.joining("\n"));

                fail("Timed out fetching notifications after "
                        + MAX_SECONDS_TO_WAIT_FOR_NOTIFICATIONS
                        + " seconds. Template IDs:\n"
                        + allTemplateIds);
            } else {

                maxSecondsToWaitForNotification -= 5;

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    // noop
                }

                allNotifications = client.getNotifications("", "", caseReference, "").getNotifications();

                matchingNotifications =
                        allNotifications
                                .stream()
                                .filter(notification -> Arrays.asList(expectedTemplateIds).contains(notification.getTemplateId().toString()))
                                .collect(Collectors.toList());
            }
            if (matchingNotifications.size() >= waitForAtLeastNumberOfNotifications) {

                for (Notification notification : matchingNotifications) {
                    assertFalse(notification.getStatus().contains("fail"));
                }

                LOG.info("Test case notifications have been delivered "
                        + "[" + matchingNotifications.size() + "/" + waitForAtLeastNumberOfNotifications + "]");

                return matchingNotifications;
            }

        } while (true);
    }

    protected void simulateCcdCallback(EventType eventType) throws IOException {

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

    protected void triggerEvent(EventType eventType) {

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

        updateCcdService.update(caseData, caseId, eventType.getId(), idamTokens);
    }

    protected void assertNotificationSubjectContains(List<Notification> notifications, String templateId, String... matches) {
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

    protected void assertNotificationBodyContains(List<Notification> notifications, String templateId, String... matches) {
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
}
