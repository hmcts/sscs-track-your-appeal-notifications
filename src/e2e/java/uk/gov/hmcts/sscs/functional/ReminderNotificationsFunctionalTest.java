package uk.gov.hmcts.sscs.functional;

import static org.junit.Assert.*;
import static uk.gov.hmcts.sscs.CcdResponseUtils.buildCcdResponse;
import static uk.gov.hmcts.sscs.domain.notify.EventType.DWP_RESPONSE_RECEIVED;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
@SpringBootTest
public class ReminderNotificationsFunctionalTest {

    @Autowired
    private CreateCcdService createCcdService;
    @Autowired
    private UpdateCcdService updateCcdService;

    @Autowired
    private IdamService idamService;

    private CcdResponse caseData;
    private IdamTokens idamTokens;
    private Long caseId;

    @Value("${notification.responseReceived.emailId}")
    private String evidenceResponseReceivedTemplateId;

    @Value("${notification.evidenceReminder.emailId}")
    private String evidenceReminderEmailTemplateId;

    @Value("${notification.evidenceReminder.smsId}")
    private String evidenceReminderSmsTemplateId;

    @Autowired
    private NotificationClient client;

    String testCaseReference;

    private static final int EXPECTED_EMAIL_NOTIFICATIONS = 2;
    private static final int EXPECTED_SMS_NOTIFICATIONS = 1;

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

        int maxSecondsToWaitForNotification = 30;
        while (!testCaseReferenceNotificationsObserved()
               && maxSecondsToWaitForNotification-- > 0) {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // noop
            }
        }

        List<Notification> sentEmailNotifications = client.getNotifications("delivered", "email", testCaseReference, "").getNotifications();

        assertEquals(EXPECTED_EMAIL_NOTIFICATIONS, sentEmailNotifications.size());

        Set<String> actualTemplateIds = new HashSet<>();
        actualTemplateIds.add(sentEmailNotifications.get(0).getTemplateId().toString());
        actualTemplateIds.add(sentEmailNotifications.get(1).getTemplateId().toString());

        assertTrue(actualTemplateIds.contains(evidenceResponseReceivedTemplateId));
        assertTrue(actualTemplateIds.contains(evidenceReminderEmailTemplateId));

        assertTrue(sentEmailNotifications.get(0).getBody().contains(testCaseReference));
        assertTrue(sentEmailNotifications.get(1).getBody().contains(testCaseReference));

        List<Notification> sentSmsNotifications = client.getNotifications("delivered", "sms", testCaseReference, "").getNotifications();

        assertEquals(EXPECTED_SMS_NOTIFICATIONS, sentSmsNotifications.size());
        assertEquals(evidenceReminderSmsTemplateId, sentSmsNotifications.get(0).getTemplateId().toString());
    }

    private boolean testCaseReferenceNotificationsObserved() throws NotificationClientException {

        return client.getNotifications("delivered", "email", testCaseReference, "")
            .getNotifications()
            .size() >= EXPECTED_EMAIL_NOTIFICATIONS
               && client.getNotifications("delivered", "sms", testCaseReference, "")
            .getNotifications()
            .size() >= EXPECTED_SMS_NOTIFICATIONS;
    }

}
