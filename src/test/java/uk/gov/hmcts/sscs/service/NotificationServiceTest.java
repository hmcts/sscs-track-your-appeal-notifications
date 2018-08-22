package uk.gov.hmcts.sscs.service;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.sscs.domain.notify.EventType.APPEAL_WITHDRAWN;

import java.net.UnknownHostException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.domain.Subscription;
import uk.gov.hmcts.sscs.domain.Subscriptions;
import uk.gov.hmcts.sscs.domain.notify.Destination;
import uk.gov.hmcts.sscs.domain.notify.Notification;
import uk.gov.hmcts.sscs.domain.notify.Reference;
import uk.gov.hmcts.sscs.domain.notify.Template;
import uk.gov.hmcts.sscs.exception.NotificationClientRuntimeException;
import uk.gov.hmcts.sscs.exception.NotificationServiceException;
import uk.gov.hmcts.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.sscs.factory.NotificationFactory;
import uk.gov.service.notify.NotificationClientException;

public class NotificationServiceTest {

    private NotificationService notificationService;

    @Mock
    private NotificationSender notificationSender;

    @Mock
    private NotificationFactory factory;

    @Mock
    private ReminderService reminderService;

    @Mock
    private NotificationValidService notificationValidService;

    CcdResponse response;
    private CcdNotificationWrapper notificationWrapper;

    @Before
    public void setup() {
        initMocks(this);
        when((notificationValidService).isNotificationStillValidToSend(any(), any())).thenReturn(true);
        notificationService = new NotificationService(notificationSender, factory, reminderService, notificationValidService);

        Subscription appellantSubscription = Subscription.builder().tya("GLSCRR").email("test@email.com")
            .mobile("07983495065").subscribeEmail("Yes").subscribeSms("Yes").build();

        response = CcdResponse.builder().subscriptions(Subscriptions.builder().appellantSubscription(appellantSubscription).build()).caseReference("ABC123").notificationType(APPEAL_WITHDRAWN).build();
        CcdResponseWrapper wrapper = CcdResponseWrapper.builder().newCcdResponse(response).oldCcdResponse(response).build();
        notificationWrapper = new CcdNotificationWrapper(wrapper);
    }

    @Test
    public void sendEmailToGovNotifyWhenNotificationIsAnEmailAndTemplateNotBlank() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), null, new Reference(), null);
        when(factory.create(notificationWrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(notificationWrapper);

        verify(notificationSender).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
    }

    @Test
    public void sendSmsToGovNotifyWhenNotificationIsAnSmsAndTemplateNotBlank() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId("123").build(), Destination.builder().email(null).sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(notificationWrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(notificationWrapper);

        verify(notificationSender).sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference(), notification.getSmsSenderTemplate());
    }

    @Test
    public void sendSmsAndEmailToGovNotifyWhenNotificationIsAnSmsAndEmailAndTemplateNotBlank() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId("123").build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(notificationWrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(notificationWrapper);

        verify(notificationSender).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
        verify(notificationSender).sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference(), notification.getSmsSenderTemplate());
    }

    @Test
    public void doNotSendEmailToGovNotifyWhenNotificationIsNotAnEmail() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId("123").build(), Destination.builder().email(null).sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(notificationWrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(notificationWrapper);

        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
    }

    @Test
    public void doNotSendSmsToGovNotifyWhenNotificationIsNotAnSms() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId("123").build(), Destination.builder().email("test@testing.com").sms(null).build(), null, new Reference(), null);
        when(factory.create(notificationWrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(notificationWrapper);

        verify(notificationSender, never()).sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference(), notification.getSmsSenderTemplate());
    }

    @Test
    public void doNotSendEmailToGovNotifyWhenEmailTemplateIsBlank() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId("123").build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(notificationWrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(notificationWrapper);

        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
    }

    @Test
    public void doNotSendSmsToGovNotifyWhenSmsTemplateIsBlank() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(notificationWrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(notificationWrapper);

        verify(notificationSender, never()).sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference(), notification.getSmsSenderTemplate());
    }

    @Test(expected = NotificationClientRuntimeException.class)
    public void shouldThrowNotificationClientRuntimeExceptionForAnyNotificationException() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), null, new Reference(), null);
        when(factory.create(notificationWrapper)).thenReturn(notification);

        doThrow(new NotificationClientException(new UnknownHostException()))
            .when(notificationSender)
            .sendEmail(
                notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference()
            );

        notificationService.createAndSendNotification(notificationWrapper);
    }

    @Test(expected = NotificationServiceException.class)
    public void shouldCorrectlyHandleAGovNotifyException() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), null, new Reference(), null);
        when(factory.create(notificationWrapper)).thenReturn(notification);

        doThrow(new RuntimeException())
            .when(notificationSender)
            .sendEmail(
                notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference()
            );

        notificationService.createAndSendNotification(notificationWrapper);
    }

    @Test
    public void doNotSendEmailOrSmsWhenNoActiveSubscription() throws Exception {
        Subscription appellantSubscription = Subscription.builder().tya("GLSCRR").email("test@email.com")
            .mobile("07983495065").subscribeEmail("No").subscribeSms("No").build();

        response = CcdResponse.builder().subscriptions(Subscriptions.builder().appellantSubscription(appellantSubscription).build()).caseReference("ABC123").notificationType(APPEAL_WITHDRAWN).build();
        CcdResponseWrapper wrapper = CcdResponseWrapper.builder().newCcdResponse(response).oldCcdResponse(response).build();

        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId("123").build(), Destination.builder().email(null).sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(notificationWrapper)).thenReturn(notification);

        notificationService.createAndSendNotification(new CcdNotificationWrapper(wrapper));

        verify(notificationSender, never()).sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference(), notification.getSmsSenderTemplate());
        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
    }

    @Test
    public void createsReminders() {

        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId("123").build(), Destination.builder().email(null).sms("07823456746").build(), null, new Reference(), null);

        when(factory.create(notificationWrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(notificationWrapper);

        verify(reminderService).createReminders(response);
    }

    @Test
    public void doNotSendNotificationWhenNotificationNotValidToSend() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), null, new Reference(), null);
        when(factory.create(notificationWrapper)).thenReturn(notification);
        when((notificationValidService).isNotificationStillValidToSend(any(), any())).thenReturn(false);

        notificationService.createAndSendNotification(notificationWrapper);

        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
    }

}
