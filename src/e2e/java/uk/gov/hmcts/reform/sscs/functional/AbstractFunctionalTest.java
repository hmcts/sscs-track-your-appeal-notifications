package uk.gov.hmcts.reform.sscs.functional;

import static helper.EnvironmentProfileValueSource.getEnvOrEmpty;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.sscs.SscsCaseDataUtils.buildSscsCaseData;
import static uk.gov.hmcts.reform.sscs.SscsCaseDataUtils.buildSscsCaseDataWelsh;
import static uk.gov.hmcts.reform.sscs.SscsCaseDataUtils.subscribeRep;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SYA_APPEAL_CREATED;

import helper.EnvironmentProfileValueSource;
import io.restassured.RestAssured;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import junitparams.JUnitParamsRunner;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.Event;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
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
    public Retry retry = new Retry(2);


    private final int maxSecondsToWaitForNotification;

    @Autowired
    @Qualifier("testNotificationClient")
    @Getter
    private NotificationClient client;

    @Autowired
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Autowired
    protected IdamService idamService;

    protected IdamTokens idamTokens;

    protected String caseReference;

    protected Long caseId;

    protected SscsCaseData caseData;

    @Autowired
    protected CcdService ccdService;

    @Before
    public void setup() {
        idamTokens = idamService.getIdamTokens();
        createCase();
    }

    public AbstractFunctionalTest(int maxSecondsToWaitForNotification) {
        this.maxSecondsToWaitForNotification = maxSecondsToWaitForNotification;
    }

    protected void createCase() {
        this.createCase(false);

    }

    protected void createCase(boolean isWelsh) {

        caseReference = generateRandomCaseReference();

        caseData = isWelsh ? createWelshCaseData() : createCaseData();

        SscsCaseDetails caseDetails = ccdService.createCase(caseData, "appealCreated", "Appeal created summary", "Appeal created description", idamTokens);

        assertNotNull(caseDetails);
        caseId = caseDetails.getId();
        log.info("Created CCD case [" + caseId + "] successfully");
    }

    protected SscsCaseData createCaseData() {
        return buildSscsCaseData(caseReference, "Yes", "Yes", SYA_APPEAL_CREATED, "oral");
    }

    protected SscsCaseData createWelshCaseData() {
        return buildSscsCaseDataWelsh(caseReference, "Yes", "Yes", SYA_APPEAL_CREATED, "oral");
    }

    private String generateRandomCaseReference() {
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

    public List<Notification> tryFetchNotificationsForTestCaseWithExpectedText(String expectedText, String... expectedTemplateIds) throws NotificationClientException {
        return tryFetchNotificationsForTestCaseWithFlag(false, expectedText, expectedTemplateIds);
    }

    public List<Notification> tryFetchNotificationsForTestCase(String... expectedTemplateIds) throws NotificationClientException {
        return tryFetchNotificationsForTestCaseWithFlag(false, null, expectedTemplateIds);
    }

    public List<Notification> tryFetchNotificationsForTestCaseWithFlag(boolean notificationNotFoundFlag,
                                                                       String expectedText,
                                                                       String... expectedTemplateIds)
        throws NotificationClientException {

        List<Notification> allNotifications = new ArrayList<>();
        Set<Notification> matchingNotifications = new HashSet<>();

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

                String expected = null;
                for (String templateId : expectedTemplateIds) {
                    expected += templateId + "\n";
                }

                log.info("Timed out fetching notifications after "
                        + maxSecondsToWaitForNotification
                        + " seconds. Template IDs delivered:\n"
                        + allTemplateIds
                        + "\n Template IDs expected:\n"
                        + expected);
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
                                .filter(notification -> expectedText == null || StringUtils.contains(notification.getBody(), expectedText))
                                .filter(notification -> asList(expectedTemplateIds).contains(notification.getTemplateId().toString()))
                                .collect(toSet());
            }
            if (matchingNotifications.size() >= waitForAtLeastNumberOfNotifications) {

                for (Notification notification : matchingNotifications) {
                    assertFalse(notification.getStatus().contains("fail"));
                }

                log.info("Test case notifications have been delivered "
                        + "[" + matchingNotifications.size() + "/" + waitForAtLeastNumberOfNotifications + "]");

                return new ArrayList<>(matchingNotifications);
            }

        } while (true);
    }

    public List<Notification> fetchLetters() throws NotificationClientException {
        List<Notification> allNotifications = new ArrayList<>();
        allNotifications = client.getNotifications("", "letter", caseId.toString(), "").getNotifications();
        int secondsLeft = maxSecondsToWaitForNotification;
        while (allNotifications.size() == 0 && secondsLeft < 0) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // noop
            }
            secondsLeft -= 5;
            allNotifications = client.getNotifications("", "letter", caseId.toString(), "").getNotifications();

        }
        return allNotifications;
    }

    protected void simulateWelshCcdCallback(NotificationEventType eventType) throws IOException {
        String resource = eventType.getId() + "CallbackWelsh.json";
        simulateCcdCallback(eventType, resource);
    }

    protected void simulateCcdCallback(NotificationEventType eventType) throws IOException {
        String resource = eventType.getId() + "Callback.json";
        simulateCcdCallback(eventType, resource);
    }

    public void simulateCcdCallback(NotificationEventType eventType, String resource) throws IOException {
        final String callbackUrl = getEnvOrEmpty("TEST_URL") + "/send";

        String path = Objects.requireNonNull(getClass().getClassLoader().getResource(resource)).getFile();
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

    private String updateJson(String json, NotificationEventType eventType) {
        json = json.replace("12345656789", caseId.toString());
        json = json.replace("SC022/14/12423", caseReference);

        if (eventType.equals(NotificationEventType.HEARING_BOOKED_NOTIFICATION)) {
            json = json.replace("2048-01-01", LocalDate.now().toString());
            json = json.replace("2016-01-01", LocalDate.now().toString());
        }

        return json;
    }

    void subscribeRepresentative() {
        subscribeRep(caseData);
    }

    void triggerEventWithHearingType(NotificationEventType eventType, String hearingType) {
        caseData.getAppeal().setHearingType(hearingType);
        triggerEvent(eventType);
    }

    void triggerEvent(NotificationEventType eventType) {

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

        log.info("Functional test: updating case [" + caseId + "] with gaps event [" + eventType.getId() + "]");

        ccdService.updateCase(caseData, caseId, "caseUpdated", "CCD Case", "Functional test: notification Service updated case with eventType " + eventType.getId(), idamTokens);
    }

    void assertNotificationSubjectContains(List<Notification> notifications, String templateId, String... matches) {
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

        if (templateId != null) {
            String bodies =
                    notifications
                            .stream()
                            .filter(notification -> notification.getTemplateId().equals(UUID.fromString(templateId)))
                            .map(Notification::getBody)
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


}
