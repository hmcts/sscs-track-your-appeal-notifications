package uk.gov.hmcts.sscs.functional;

import static org.junit.Assert.*;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.sscs.CcdResponseUtils.buildCcdResponse;
import static uk.gov.hmcts.sscs.domain.notify.EventType.DWP_RESPONSE_RECEIVED;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
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
@SpringBootTest(
    properties = {
        "org.quartz.scheduler.idleWaitTime=5000"
    }
)
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

    @Autowired
    private NotificationClient client;

    String testCaseReference;

    private static final int EXPECTED_EMAIL_NOTIFICATIONS = 2;
    private static final int EXPECTED_SMS_NOTIFICATIONS = 1;
    private static final int MAX_SECONDS_TO_WAIT_FOR_NOTIFICATIONS = 120;

    @Before
    public void setup() {

        String epoch = String.valueOf(Instant.now().toEpochMilli());
        testCaseReference =
            "SC"
            + epoch.substring(3, 6)
            + "/"
            + epoch.substring(6, 8)
            + "/"
            + epoch.substring(8, 13);

        caseData = buildCcdResponse(testCaseReference, "Yes", "Yes", EventType.DWP_RESPONSE_RECEIVED);

        idamTokens = IdamTokens.builder()
            .authenticationService(idamService.generateServiceAuthorization())
            .idamOauth2Token(idamService.getIdamOauth2Token())
            .build();

        CaseDetails caseDetails = createCcdService.create(caseData, idamTokens);

        assertNotNull(caseDetails);
        caseId = caseDetails.getId();
    }

    @Test
    public void shouldSendResponseReceivedNotification() throws NotificationClientException {

        CaseDetails updatedCaseDetails = updateCcdService.update(caseData, caseId, DWP_RESPONSE_RECEIVED.getId(), idamTokens);

        assertEquals("COMPLETED", updatedCaseDetails.getCallbackResponseStatus());

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
                    sentEmailNotification.getTemplateId().equals(UUID.fromString(evidenceReminderEmailTemplateId))
                )
        );

        assertTrue(
            sentEmailNotifications.stream()
                .anyMatch(sentEmailNotification ->
                    sentEmailNotification.getBody().contains(testCaseReference)
                )
        );

        List<Notification> sentSmsNotifications = notifications.get().getRight();

        assertTrue(sentSmsNotifications.size() >= EXPECTED_SMS_NOTIFICATIONS);

        assertTrue(
            sentSmsNotifications.stream()
                .anyMatch(sentSmsNotification ->
                    sentSmsNotification.getTemplateId().equals(UUID.fromString(evidenceReminderSmsTemplateId))
                )
        );
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

}
