package uk.gov.hmcts.reform.sscs.service;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.config.NotificationBlacklist;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendSmsResponse;

public class NotificationSenderTest {

    public static final String CCD_CASE_ID = "78980909090099";
    public static final String SMS_SENDER = "sms-sender";
    private NotificationClient notificationClient;
    private NotificationClient testNotificationClient;
    private NotificationBlacklist blacklist;
    private NotificationSender notificationSender;
    private String templateId;
    private Map<String, String> personalisation;
    private String reference;

    @Mock
    private SendEmailResponse sendEmailResponse;

    @Mock
    private SendSmsResponse sendSmsResponse;

    @Before
    public void setUp() {
        initMocks(this);

        notificationClient = mock(NotificationClient.class);
        testNotificationClient = mock(NotificationClient.class);
        blacklist = mock(NotificationBlacklist.class);
        templateId = "templateId";
        personalisation = Collections.emptyMap();
        reference = "reference";

        notificationSender = new NotificationSender(notificationClient, testNotificationClient, blacklist);
    }

    @Test
    public void sendEmailToTestSenderIfMatchesPattern() throws NotificationClientException {
        String emailAddress = "test123@hmcts.net";
        when(testNotificationClient.sendEmail(templateId, emailAddress, personalisation, reference))
                .thenReturn(sendEmailResponse);
        when(sendEmailResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        notificationSender.sendEmail(templateId, emailAddress, personalisation, reference, CCD_CASE_ID);

        verifyZeroInteractions(notificationClient);
        verify(testNotificationClient).sendEmail(templateId, emailAddress, personalisation, reference);
    }

    @Test
    public void sendEmailToNormalSender() throws NotificationClientException {
        String emailAddress = "random@example.com";
        when(notificationClient.sendEmail(templateId, emailAddress, personalisation, reference))
                .thenReturn(sendEmailResponse);
        when(sendEmailResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        notificationSender.sendEmail(templateId, emailAddress, personalisation, reference, CCD_CASE_ID);

        verifyZeroInteractions(testNotificationClient);
        verify(notificationClient).sendEmail(templateId, emailAddress, personalisation, reference);
    }

    @Test
    public void sendEmailToTestSenderIfOnBlacklist() throws NotificationClientException {
        String emailAddress = "random@example.com";
        when(blacklist.getTestRecipients()).thenReturn(singletonList(emailAddress));
        when(testNotificationClient.sendEmail(templateId, emailAddress, personalisation, reference))
                .thenReturn(sendEmailResponse);
        when(sendEmailResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        notificationSender.sendEmail(templateId, emailAddress, personalisation, reference, CCD_CASE_ID);

        verifyZeroInteractions(notificationClient);
        verify(testNotificationClient).sendEmail(templateId, emailAddress, personalisation, reference);
    }


    @Test
    public void sendSmsToNormalSender() throws NotificationClientException {
        String phoneNumber = "07777777777";
        when(notificationClient.sendSms(templateId, phoneNumber, personalisation, reference, SMS_SENDER))
                .thenReturn(sendSmsResponse);
        when(sendSmsResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        notificationSender.sendSms(templateId, phoneNumber, personalisation, reference, SMS_SENDER, CCD_CASE_ID);

        verifyZeroInteractions(testNotificationClient);
        verify(notificationClient).sendSms(templateId, phoneNumber, personalisation, reference, SMS_SENDER);
    }


    @Test
    public void sendSmsToTestSenderIfOnBlacklist() throws NotificationClientException {
        String phoneNumber = "07777777777";
        when(blacklist.getTestRecipients()).thenReturn(singletonList(phoneNumber));
        when(testNotificationClient.sendSms(templateId, phoneNumber, personalisation, reference, SMS_SENDER))
                .thenReturn(sendSmsResponse);
        when(sendSmsResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        notificationSender.sendSms(templateId, phoneNumber, personalisation, reference, SMS_SENDER, CCD_CASE_ID);

        verifyZeroInteractions(notificationClient);
        verify(testNotificationClient).sendSms(templateId, phoneNumber, personalisation, reference, SMS_SENDER);
    }


}