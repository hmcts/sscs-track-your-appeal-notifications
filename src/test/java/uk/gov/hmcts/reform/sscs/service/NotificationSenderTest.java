package uk.gov.hmcts.reform.sscs.service;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.config.NotificationBlacklist;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

public class NotificationSenderTest {

    private NotificationClient notificationClient;
    private NotificationClient testNotificationClient;
    private NotificationBlacklist blacklist;
    private NotificationSender underTest;
    private String templateId;
    private Map<String, String> personalisation;
    private String reference;

    @Before
    public void setUp() throws Exception {
        notificationClient = mock(NotificationClient.class);
        testNotificationClient = mock(NotificationClient.class);
        blacklist = mock(NotificationBlacklist.class);
        templateId = "templateId";
        personalisation = Collections.emptyMap();
        reference = "reference";

        underTest = new NotificationSender(notificationClient, testNotificationClient, blacklist);
    }

    @Test
    public void sendEmailToTestSenderIfMatchesPattern() throws NotificationClientException {
        String emailAddress = "test123@hmcts.net";
        underTest.sendEmail(templateId, emailAddress, personalisation, reference);

        verifyZeroInteractions(notificationClient);
        verify(testNotificationClient).sendEmail(templateId, emailAddress, personalisation, reference);
    }

    @Test
    public void sendEmailToNormalSender() throws NotificationClientException {
        String emailAddress = "random@example.com";
        underTest.sendEmail(templateId, emailAddress, personalisation, reference);

        verifyZeroInteractions(testNotificationClient);
        verify(notificationClient).sendEmail(templateId, emailAddress, personalisation, reference);
    }

    @Test
    public void sendEmailToTestSenderIfOnBlacklist() throws NotificationClientException {
        String emailAddress = "random@example.com";
        when(blacklist.getTestRecipients()).thenReturn(singletonList(emailAddress));
        underTest.sendEmail(templateId, emailAddress, personalisation, reference);

        verifyZeroInteractions(notificationClient);
        verify(testNotificationClient).sendEmail(templateId, emailAddress, personalisation, reference);
    }
}