package uk.gov.hmcts.sscs.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.sscs.domain.notify.EventType.APPEAL_WITHDRAWN;
import static uk.gov.hmcts.sscs.domain.notify.EventType.DWP_RESPONSE_RECEIVED;

import java.net.UnknownHostException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.domain.Subscription;
import uk.gov.hmcts.sscs.domain.Subscriptions;
import uk.gov.hmcts.sscs.domain.notify.*;
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

    @Mock
    private ReminderService reminderService;

    CcdResponse response;

    @Before
    public void setup() {
        initMocks(this);
        notificationService = new NotificationService(client, factory, reminderService);

        Subscription appellantSubscription = Subscription.builder()
                .firstName("Harry").surname("Kane").title("Mr").tya("GLSCRR").email("test@email.com")
                .mobile("07983495065").subscribeEmail("Yes").subscribeSms("Yes").build();

        response = CcdResponse.builder().subscriptions(Subscriptions.builder().appellantSubscription(appellantSubscription).build()).caseReference("ABC123").notificationType(APPEAL_WITHDRAWN).build();
        wrapper = CcdResponseWrapper.builder().newCcdResponse(response).oldCcdResponse(response).build();
    }

    @Test
    public void sendEmailToGovNotifyWhenNotificationIsAnEmailAndTemplateNotBlank() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(wrapper);

        verify(client).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
    }

    @Test
    public void sendSmsToGovNotifyWhenNotificationIsAnSmsAndTemplateNotBlank() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId("123").build(), Destination.builder().email(null).sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(wrapper);

        verify(client).sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference());
    }

    @Test
    public void sendSmsAndEmailToGovNotifyWhenNotificationIsAnSmsAndEmailAndTemplateNotBlank() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId("123").build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(wrapper);

        verify(client).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
        verify(client).sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference());
    }

    @Test
    public void doNotSendEmailToGovNotifyWhenNotificationIsNotAnEmail() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId("123").build(), Destination.builder().email(null).sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(wrapper);

        verify(client, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
    }

    @Test
    public void doNotSendSmsToGovNotifyWhenNotificationIsNotAnSms() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId("123").build(), Destination.builder().email("test@testing.com").sms(null).build(), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(wrapper);

        verify(client, never()).sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference());
    }

    @Test
    public void doNotSendEmailToGovNotifyWhenEmailTemplateIsBlank() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId("123").build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(wrapper);

        verify(client, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
    }

    @Test
    public void doNotSendSmsToGovNotifyWhenSmsTemplateIsBlank() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(wrapper);

        verify(client, never()).sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference());
    }

    @Test(expected = NotificationClientRuntimeException.class)
    public void shouldThrowNotificationClientRuntimeExceptionForAnyNotificationException() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);

        when(client.sendEmail(
                notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference()))
                .thenThrow(new NotificationClientException(new UnknownHostException()));

        notificationService.createAndSendNotification(wrapper);
    }

    @Test(expected = NotificationServiceException.class)
    public void shouldCorrectlyHandleAGovNotifyException() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);

        when(client.sendEmail(
                notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference()))
                .thenThrow(new RuntimeException());

        notificationService.createAndSendNotification(wrapper);
    }

    @Test
    public void createAReminderJobWhenNotificationIsDwpResponseReceived() throws Exception {
        ReflectionTestUtils.setField(notificationService, "isJobSchedulerEnabled", true);

        response.setNotificationType(DWP_RESPONSE_RECEIVED);

        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId("123").build(), Destination.builder().email(null).sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(wrapper);

        verify(reminderService).createJob(response);
    }

    @Test
    public void doNotCreateAReminderJobWhenNotificationIsDwpResponseReceivedAndJobSchedulerIsNotEnabled() throws Exception {
        ReflectionTestUtils.setField(notificationService, "isJobSchedulerEnabled", false);

        response.setNotificationType(DWP_RESPONSE_RECEIVED);

        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId("123").build(), Destination.builder().email(null).sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(wrapper);

        verify(reminderService, never()).createJob(response);
    }

    @Test
    public void doNotCreateAReminderJobWhenReminderIsNotRequired() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId("123").build(), Destination.builder().email(null).sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(wrapper);

        verify(reminderService, never()).createJob(response);
    }

    @Test
    public void doNotSendEmailOrSmsWhenNoActiveSubscription() throws Exception {
        Subscription appellantSubscription = Subscription.builder()
                .firstName("Harry").surname("Kane").title("Mr").tya("GLSCRR").email("test@email.com")
                .mobile("07983495065").subscribeEmail("No").subscribeSms("No").build();

        response = CcdResponse.builder().subscriptions(Subscriptions.builder().appellantSubscription(appellantSubscription).build()).caseReference("ABC123").notificationType(APPEAL_WITHDRAWN).build();
        wrapper = CcdResponseWrapper.builder().newCcdResponse(response).oldCcdResponse(response).build();

        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId("123").build(), Destination.builder().email(null).sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(wrapper)).thenReturn(notification);

        notificationService.createAndSendNotification(wrapper);

        verify(client, never()).sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference());
        verify(client, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
    }
}
