package uk.gov.hmcts.sscs.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.UnknownHostException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.domain.Subscription;
import uk.gov.hmcts.sscs.domain.notify.Destination;
import uk.gov.hmcts.sscs.domain.notify.Notification;
import uk.gov.hmcts.sscs.domain.notify.Reference;
import uk.gov.hmcts.sscs.domain.notify.Template;
import uk.gov.hmcts.sscs.exception.NotificationClientRuntimeException;
import uk.gov.hmcts.sscs.exception.NotificationServiceException;
import uk.gov.hmcts.sscs.factory.NotificationFactory;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

public class NotificationServiceTest {

    private NotificationService notificationService;

    private CcdResponseWrapper wrapper;

    @Mock
    private NotificationClient client;

    @Mock
    private NotificationFactory factory;

    @Before
    public void setup() {
        initMocks(this);
        notificationService = new NotificationService(client, factory);
        CcdResponse response = new CcdResponse();
        response.setAppellantSubscription(new Subscription("", "", "", "", "", "", true, true));
        response.setCaseReference("ABC123");
        wrapper = new CcdResponseWrapper(response, response);
    }

    @Test
    public void sendEmailToGovNotifyWhenNotificationIsAnEmailAndTemplateNotBlank() throws Exception {
        Notification notification = new Notification(new Template("abc", null), new Destination("test@testing.com", null), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(wrapper);

        verify(client).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
    }

    @Test
    public void sendSmsToGovNotifyWhenNotificationIsAnSmsAndTemplateNotBlank() throws Exception {
        Notification notification = new Notification(new Template(null, "123"), new Destination(null, "07823456746"), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(wrapper);

        verify(client).sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference());
    }

    @Test
    public void sendSmsAndEmailToGovNotifyWhenNotificationIsAnSmsAndEmailAndTemplateNotBlank() throws Exception {
        Notification notification = new Notification(new Template("abc", "123"), new Destination("test@testing.com", "07823456746"), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(wrapper);

        verify(client).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
        verify(client).sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference());
    }

    @Test
    public void doNotSendEmailToGovNotifyWhenNotificationIsNotAnEmail() throws Exception {
        Notification notification = new Notification(new Template("abc", "123"), new Destination(null, "07823456746"), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(wrapper);

        verify(client, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
    }

    @Test
    public void doNotSendSmsToGovNotifyWhenNotificationIsNotAnSms() throws Exception {
        Notification notification = new Notification(new Template("abc", "123"), new Destination("test@testing.com", null), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(wrapper);

        verify(client, never()).sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference());
    }

    @Test
    public void doNotSendEmailToGovNotifyWhenEmailTemplateIsBlank() throws Exception {
        Notification notification = new Notification(new Template(null, "123"), new Destination("test@testing.com", "07823456746"), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(wrapper);

        verify(client, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
    }

    @Test
    public void doNotSendSmsToGovNotifyWhenSmsTemplateIsBlank() throws Exception {
        Notification notification = new Notification(new Template("abc", null), new Destination("test@testing.com", "07823456746"), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(wrapper);

        verify(client, never()).sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference());
    }

    @Test(expected = NotificationClientRuntimeException.class)
    public void shouldThrowNotificationClientRuntimeExceptionForAnyNotificationException() throws Exception {
        Notification notification = new Notification(new Template("abc", null), new Destination("test@testing.com", null), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);

        when(client.sendEmail(
                notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference()))
                .thenThrow(new NotificationClientException(new UnknownHostException()));

        notificationService.createAndSendNotification(wrapper);
    }

    @Test(expected = NotificationServiceException.class)
    public void shouldCorrectlyHandleAGovNotifyException() throws Exception {
        Notification notification = new Notification(new Template("abc", null), new Destination("test@testing.com", null), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);

        when(client.sendEmail(
                notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference()))
                .thenThrow(new RuntimeException());

        notificationService.createAndSendNotification(wrapper);
    }
}
