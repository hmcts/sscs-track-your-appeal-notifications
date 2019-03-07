package uk.gov.hmcts.reform.sscs.functional;

import static helper.EnvironmentProfileValueSource.getEnvOrEmpty;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.sscs.SscsCaseDataUtils.buildSscsCaseData;
import static uk.gov.hmcts.reform.sscs.SscsCaseDataUtils.subscribeRep;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SYA_APPEAL_CREATED;

import helper.EnvironmentProfileValueSource;
import io.restassured.RestAssured;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import junitparams.JUnitParamsRunner;
import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.ProfileValueSourceConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

@RunWith(JUnitParamsRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
@ProfileValueSourceConfiguration(EnvironmentProfileValueSource.class)
public abstract class AbstractFunctionalTest {

    private static final Logger log = getLogger(AuthorisationService.class);

    // Below rules are needed to use the junitParamsRunner together with SpringRunner
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();
    //end of rules needed for junitParamsRunner

    @Rule
    public Retry retry = new Retry(3);


    private final int maxSecondsToWaitForNotification;

    @Autowired
    @Qualifier("testNotificationClient")
    private NotificationClient client;

    @Autowired
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Autowired
    private IdamService idamService;

    private IdamTokens idamTokens;

    protected String caseReference;

    protected Long caseId;

    protected SscsCaseData caseData;

    @Autowired
    private CcdService ccdService;

    @Before
    public void setup() {

        idamTokens = idamService.getIdamTokens();

        createCase();
    }

    public AbstractFunctionalTest(int maxSecondsToWaitForNotification) {
        this.maxSecondsToWaitForNotification = maxSecondsToWaitForNotification;
    }

    private void createCase() {

        caseReference = generateRandomCaseReference();

        caseData = createCaseData();

        SscsCaseDetails caseDetails = ccdService.createCase(caseData, "appealCreated", "Appeal created summary", "Appeal created description", idamTokens);

        assertNotNull(caseDetails);
        caseId = caseDetails.getId();
        log.info("Created CCD case [" + caseId + "] successfully");
    }

    protected SscsCaseData createCaseData() {
        return buildSscsCaseData(caseReference, "Yes", "Yes", SYA_APPEAL_CREATED, "oral");
    }

    protected String generateRandomCaseReference() {
        String epoch = String.valueOf(Instant.now().toEpochMilli());
        Map<String,String> sscCodeMap = regionalProcessingCenterService.getSccodeRegionalProcessingCentermap();
        String scNumber = generateScNumber(Instant.now(),sscCodeMap);
        return scNumber
                + "/"
                + epoch.substring(6, 8)
                + "/"
                + epoch.substring(8, 13);
    }

    // Recursive function until we find the right sc number other than Glasgow
    private String generateScNumber(Instant currentEpoh, Map<String,String> sscCodeMap) {
        String epoch = String.valueOf(currentEpoh.toEpochMilli());
        String scNumber = "SC" + epoch.substring(3,6);
        String city = sscCodeMap.get(scNumber);
        if (city == null || city.equalsIgnoreCase("SSCS Glasgow")) {
            return generateScNumber(currentEpoh.plusSeconds(86400),sscCodeMap); //86400 is day
        }
        return scNumber;
    }

    public List<Notification> tryFetchNotificationsForTestCase(String... expectedTemplateIds) throws NotificationClientException {
        return tryFetchNotificationsForTestCaseWithFlag(false, expectedTemplateIds);
    }

    public List<Notification> tryFetchNotificationsForTestCaseWithFlag(boolean notificationNotFoundFlag, String... expectedTemplateIds) throws NotificationClientException {

        List<Notification> allNotifications = new ArrayList<>();
        List<Notification> matchingNotifications = new ArrayList<>();

        int waitForAtLeastNumberOfNotifications = expectedTemplateIds.length;

        int secondsLeft = maxSecondsToWaitForNotification;

        do {

            log.info("Waiting for all test case notifications to be delivered "
                    + "[" + matchingNotifications.size() + "/" + waitForAtLeastNumberOfNotifications + "] ..."
            );

            if (secondsLeft <= 0) {

                String allTemplateIds =
                        allNotifications
                                .stream()
                                .map(notification -> notification.getTemplateId().toString())
                                .collect(Collectors.joining("\n"));

                log.info("Timed out fetching notifications after "
                        + maxSecondsToWaitForNotification
                        + " seconds. Template IDs:\n"
                        + allTemplateIds);
                if (notificationNotFoundFlag) {
                    return Collections.emptyList();
                } else {
                    fail();
                }
            } else {

                secondsLeft -= 5;

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    // noop
                }

                allNotifications = client.getNotifications("", "", caseReference, "").getNotifications();

                matchingNotifications =
                        allNotifications
                                .stream()
                                .filter(notification -> asList(expectedTemplateIds).contains(notification.getTemplateId().toString()))
                                .collect(toList());
            }
            if (matchingNotifications.size() >= waitForAtLeastNumberOfNotifications) {

                for (Notification notification : matchingNotifications) {
                    assertFalse(notification.getStatus().contains("fail"));
                }

                log.info("Test case notifications have been delivered "
                        + "[" + matchingNotifications.size() + "/" + waitForAtLeastNumberOfNotifications + "]");

                log.info("Test case notification ids: ");

                for (Notification notification :  matchingNotifications) {
                    log.info("I am a template: ");
                    log.info(notification.getTemplateId().toString().subSequence(0, 12) + ", ");
                    log.info(notification.getTemplateId().toString() + ", ");
                }

                return matchingNotifications;
            }

        } while (true);
    }

    protected void simulateCcdCallback(NotificationEventType eventType) throws IOException {
        String resource = eventType.getId() + "Callback.json";
        simulateCcdCallback(eventType, resource);
    }

    public void simulateCcdCallback(NotificationEventType eventType, String resource) throws IOException {
        final String callbackUrl = getEnvOrEmpty("TEST_URL") + "/send";

        String path = getClass().getClassLoader().getResource(resource).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        json = updateJson(json, eventType);

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

    protected void simulateCohCallback(NotificationEventType eventType, String hearingId) throws IOException {

        final String callbackUrl = getEnvOrEmpty("TEST_URL") + "/coh-send";

        String path = getClass().getClassLoader().getResource("cohCallback.json").getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        json = json.replace("{eventType}", eventType.getId());
        json = json.replace("{caseId}", caseId.toString());
        json = json.replace("{hearingId}", hearingId);

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

    private String updateJson(String json, NotificationEventType eventType) {
        json = json.replace("12345656789", caseId.toString());
        json = json.replace("SC022/14/12423", caseReference);

        if (eventType.equals(NotificationEventType.HEARING_BOOKED_NOTIFICATION)) {
            json = json.replace("2016-01-01", LocalDate.now().toString());
        }

        return json;
    }

    protected void subscribeRepresentative() {
        subscribeRep(caseData);
    }

    protected void triggerEventWithHearingType(NotificationEventType eventType, String hearingType) {
        caseData.getAppeal().setHearingType(hearingType);
        triggerEvent(eventType);
    }

    protected void triggerEvent(NotificationEventType eventType) {

        Event events = Event.builder()
                .value(EventDetails.builder()
                        .type(eventType.getId())
                        .description(eventType.getId())
                        .date("2016-01-16T12:34:56.789")
                        .build())
                .build();

        List<Event> allEvents = new ArrayList<>(caseData.getEvents());
        allEvents.add(events);
        caseData.setEvents(allEvents);

        ccdService.updateCase(caseData, caseId, eventType.getId(), "CCD Case", "Notification Service updated case", idamTokens);
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
