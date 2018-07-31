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
import uk.gov.hmcts.sscs.factory.NotificationFactory;
import uk.gov.service.notify.NotificationClientException;

public class NotificationServiceTest {

    private NotificationService notificationService;

    private CcdResponseWrapper wrapper;

    @Mock
    private NotificationSender notificationSender;

    @Mock
    private NotificationFactory factory;

    @Mock
    private ReminderService reminderService;

    CcdResponse response;

    @Before
    public void setup() {
        initMocks(this);
        notificationService = new NotificationService(notificationSender, factory, reminderService);

        Subscription appellantSubscription = Subscription.builder().tya("GLSCRR").email("test@email.com")
            .mobile("07983495065").subscribeEmail("Yes").subscribeSms("Yes").build();

        response = CcdResponse.builder().subscriptions(Subscriptions.builder().appellantSubscription(appellantSubscription).build()).caseReference("ABC123").notificationType(APPEAL_WITHDRAWN).build();
        wrapper = CcdResponseWrapper.builder().newCcdResponse(response).oldCcdResponse(response).build();
    }

    @Test
    public void sendEmailToGovNotifyWhenNotificationIsAnEmailAndTemplateNotBlank() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(wrapper);

        verify(notificationSender).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
    }

    @Test
    public void sendSmsToGovNotifyWhenNotificationIsAnSmsAndTemplateNotBlank() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId("123").build(), Destination.builder().email(null).sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(wrapper);

        verify(notificationSender).sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference());
    }

    @Test
    public void sendSmsAndEmailToGovNotifyWhenNotificationIsAnSmsAndEmailAndTemplateNotBlank() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId("123").build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(wrapper);

        verify(notificationSender).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
        verify(notificationSender).sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference());
    }

    @Test
    public void doNotSendEmailToGovNotifyWhenNotificationIsNotAnEmail() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId("123").build(), Destination.builder().email(null).sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(wrapper);

        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
    }

    @Test
    public void doNotSendSmsToGovNotifyWhenNotificationIsNotAnSms() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId("123").build(), Destination.builder().email("test@testing.com").sms(null).build(), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(wrapper);

        verify(notificationSender, never()).sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference());
    }

    @Test
    public void doNotSendEmailToGovNotifyWhenEmailTemplateIsBlank() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId("123").build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(wrapper);

        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
    }

    @Test
    public void doNotSendSmsToGovNotifyWhenSmsTemplateIsBlank() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(wrapper);

        verify(notificationSender, never()).sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference());
    }

    @Test(expected = NotificationClientRuntimeException.class)
    public void shouldThrowNotificationClientRuntimeExceptionForAnyNotificationException() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);

        doThrow(new NotificationClientException(new UnknownHostException()))
            .when(notificationSender)
            .sendEmail(
                notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference()
            );

        notificationService.createAndSendNotification(wrapper);
    }

    @Test(expected = NotificationServiceException.class)
    public void shouldCorrectlyHandleAGovNotifyException() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);

        doThrow(new RuntimeException())
            .when(notificationSender)
            .sendEmail(
                notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference()
            );

        notificationService.createAndSendNotification(wrapper);
    }

    @Test
    public void doNotSendEmailOrSmsWhenNoActiveSubscription() throws Exception {
        Subscription appellantSubscription = Subscription.builder().tya("GLSCRR").email("test@email.com")
            .mobile("07983495065").subscribeEmail("No").subscribeSms("No").build();

        response = CcdResponse.builder().subscriptions(Subscriptions.builder().appellantSubscription(appellantSubscription).build()).caseReference("ABC123").notificationType(APPEAL_WITHDRAWN).build();
        wrapper = CcdResponseWrapper.builder().newCcdResponse(response).oldCcdResponse(response).build();

        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId("123").build(), Destination.builder().email(null).sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);

        notificationService.createAndSendNotification(wrapper);

        verify(notificationSender, never()).sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference());
        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
    }

    @Test
    public void createsReminders() {

        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId("123").build(), Destination.builder().email(null).sms("07823456746").build(), null, new Reference(), null);

        when(factory.create(wrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(wrapper);

        verify(reminderService, times(1)).createReminders(response);
    }

}
