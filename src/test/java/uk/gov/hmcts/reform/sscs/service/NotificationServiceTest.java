package uk.gov.hmcts.reform.sscs.service;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_WITHDRAWN_NOTIFICATION;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.Destination;
import uk.gov.hmcts.reform.sscs.domain.notify.Notification;
import uk.gov.hmcts.reform.sscs.domain.notify.Reference;
import uk.gov.hmcts.reform.sscs.domain.notify.Template;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.factory.NotificationFactory;

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

    @Mock
    private NotificationHandler notificationHandler;

    private SscsCaseData response;
    private CcdNotificationWrapper notificationWrapper;

    @Before
    public void setup() {
        initMocks(this);
        when((notificationValidService).isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when((notificationValidService).isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        notificationService = new NotificationService(notificationSender, factory, reminderService, notificationValidService, notificationHandler);

        Subscription appellantSubscription = Subscription.builder().tya("GLSCRR").email("test@email.com")
            .mobile("07983495065").subscribeEmail("Yes").subscribeSms("Yes").build();

        response = SscsCaseData.builder()
                .appeal(Appeal.builder().hearingOptions(HearingOptions.builder().wantsToAttend("Yes").build()).build())
                .subscriptions(Subscriptions.builder().appellantSubscription(appellantSubscription).build())
                .caseReference("ABC123").build();
        SscsCaseDataWrapper wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(response).oldSscsCaseData(response).notificationEventType(APPEAL_WITHDRAWN_NOTIFICATION).build();
        notificationWrapper = new CcdNotificationWrapper(wrapper);
    }

    @Test
    public void sendEmailToGovNotifyWhenNotificationIsAnEmailAndTemplateNotBlank() {
        String emailTemplateId = "abc";
        Notification notification = new Notification(Template.builder().emailTemplateId(emailTemplateId).smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), null, new Reference(), null);
        when(factory.create(notificationWrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(notificationWrapper);

        verify(notificationHandler).sendNotification(eq(notificationWrapper), eq(emailTemplateId), eq("Email"), any(NotificationHandler.SendNotification.class));
    }

    @Test
    public void sendSmsToGovNotifyWhenNotificationIsAnSmsAndTemplateNotBlank() {
        String smsTemplateId = "123";
        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId(smsTemplateId).build(), Destination.builder().email(null).sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(notificationWrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(notificationWrapper);

        verify(notificationHandler).sendNotification(eq(notificationWrapper), eq(smsTemplateId), eq("SMS"), any(NotificationHandler.SendNotification.class));
    }

    @Test
    public void sendSmsAndEmailToGovNotifyWhenNotificationIsAnSmsAndEmailAndTemplateNotBlank() {
        String emailTemplateId = "abc";
        String smsTemplateId = "123";
        Notification notification = new Notification(Template.builder().emailTemplateId(emailTemplateId).smsTemplateId(smsTemplateId).build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(notificationWrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(notificationWrapper);

        verify(notificationHandler).sendNotification(eq(notificationWrapper), eq(emailTemplateId), eq("Email"), any(NotificationHandler.SendNotification.class));
        verify(notificationHandler).sendNotification(eq(notificationWrapper), eq(smsTemplateId), eq("SMS"), any(NotificationHandler.SendNotification.class));
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

    @Test
    public void doNotSendEmailOrSmsWhenNoActiveSubscription() throws Exception {
        Subscription appellantSubscription = Subscription.builder().tya("GLSCRR").email("test@email.com")
            .mobile("07983495065").subscribeEmail("No").subscribeSms("No").build();

        response = SscsCaseData.builder().subscriptions(Subscriptions.builder().appellantSubscription(appellantSubscription).build()).caseReference("ABC123").build();
        SscsCaseDataWrapper wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(response).oldSscsCaseData(response).notificationEventType(APPEAL_WITHDRAWN_NOTIFICATION).build();

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

        verify(reminderService).createReminders(notificationWrapper);
    }

    @Test
    public void doNotSendNotificationWhenNotificationNotValidToSend() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), null, new Reference(), null);
        when(factory.create(notificationWrapper)).thenReturn(notification);
        when((notificationValidService).isNotificationStillValidToSend(any(), any())).thenReturn(false);

        notificationService.createAndSendNotification(notificationWrapper);

        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
    }

    @Test
    public void doNotSendNotificationWhenHearingTypeIsNotValidToSend() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), null, new Reference(), null);
        when(factory.create(notificationWrapper)).thenReturn(notification);
        when((notificationValidService).isHearingTypeValidToSendNotification(any(), any())).thenReturn(false);

        notificationService.createAndSendNotification(notificationWrapper);

        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
    }

}
