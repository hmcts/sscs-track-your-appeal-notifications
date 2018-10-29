package uk.gov.hmcts.reform.sscs.service;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_WITHDRAWN_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.QUESTION_ROUND_ISSUED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SUBSCRIPTION_UPDATED_NOTIFICATION;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.Destination;
import uk.gov.hmcts.reform.sscs.domain.notify.Notification;
import uk.gov.hmcts.reform.sscs.domain.notify.Reference;
import uk.gov.hmcts.reform.sscs.domain.notify.Template;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.factory.CohNotificationWrapper;
import uk.gov.hmcts.reform.sscs.factory.NotificationFactory;

public class NotificationServiceTest {

    public static final String APPEAL_NUMBER = "GLSCRR";
    public static final String YES = "Yes";
    public static final String CASE_REFERENCE = "ABC123";
    public static final String EMAIL_TEMPLATE_ID = "email-template-id";
    public static final String SMS_TEMPLATE_ID = "sms-template-id";
    public static final String SAMETEST_EMAIL_COM = "sametest@email.com";
    public static final String NEWTEST_EMAIL_COM = "newtest@email.com";
    public static final String PIP = "PIP";
    public static final String EMAIL = "Email";
    public static final String SMS = "SMS";
    public static final String MOBILE_NUMBER_1 = "07983495065";
    public static final String MOBILE_NUMBER_2 = "07983495067";

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

    @Mock
    private OutOfHoursCalculator outOfHoursCalculator;

    @Mock
    private NotificationConfig notificationConfig;

    private SscsCaseData response;
    private CcdNotificationWrapper notificationWrapper;

    @Before
    public void setup() {
        initMocks(this);
        when((notificationValidService).isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when((notificationValidService).isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        notificationService = new NotificationService(notificationSender, factory, reminderService,
                notificationValidService, notificationHandler, outOfHoursCalculator, notificationConfig);

        Subscription appellantSubscription = Subscription.builder().tya(APPEAL_NUMBER).email("test@email.com")
            .mobile(MOBILE_NUMBER_1).subscribeEmail(YES).subscribeSms(YES).build();

        response = SscsCaseData.builder()
                .appeal(Appeal.builder().hearingType(AppealHearingType.ORAL.name()).hearingOptions(HearingOptions.builder().wantsToAttend(YES).build()).build())
                .subscriptions(Subscriptions.builder().appellantSubscription(appellantSubscription).build())
                .caseReference(CASE_REFERENCE).build();
        SscsCaseDataWrapper wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(response).oldSscsCaseData(response).notificationEventType(APPEAL_WITHDRAWN_NOTIFICATION).build();
        notificationWrapper = new CcdNotificationWrapper(wrapper);
        when(outOfHoursCalculator.isItOutOfHours()).thenReturn(false);
    }

    @Test
    public void sendEmailToGovNotifyWhenNotificationIsAnEmailAndTemplateNotBlank() {
        String emailTemplateId = "abc";
        Notification notification = new Notification(Template.builder().emailTemplateId(emailTemplateId).smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), null, new Reference(), null);
        when(factory.create(notificationWrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(notificationWrapper);

        verify(notificationHandler, times(1)).sendNotification(eq(notificationWrapper), eq(emailTemplateId), eq(EMAIL), any(NotificationHandler.SendNotification.class));
    }

    @Test
    public void sendSmsToGovNotifyWhenNotificationIsAnSmsAndTemplateNotBlank() {
        String smsTemplateId = "123";
        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId(smsTemplateId).build(), Destination.builder().email(null).sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(notificationWrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(notificationWrapper);

        verify(notificationHandler, times(1)).sendNotification(eq(notificationWrapper), eq(smsTemplateId), eq(SMS), any(NotificationHandler.SendNotification.class));
    }

    @Test
    public void sendSmsAndEmailToGovNotifyWhenNotificationIsAnSmsAndEmailAndTemplateNotBlank() {
        String emailTemplateId = "abc";
        String smsTemplateId = "123";
        Notification notification = new Notification(Template.builder().emailTemplateId(emailTemplateId).smsTemplateId(smsTemplateId).build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(notificationWrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(notificationWrapper);

        verify(notificationHandler, times(1)).sendNotification(eq(notificationWrapper), eq(emailTemplateId), eq(EMAIL), any(NotificationHandler.SendNotification.class));
        verify(notificationHandler, times(1)).sendNotification(eq(notificationWrapper), eq(smsTemplateId), eq(SMS), any(NotificationHandler.SendNotification.class));
    }

    @Test
    public void doNotSendEmailToGovNotifyWhenNotificationIsNotAnEmail() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId("123").build(), Destination.builder().email(null).sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(notificationWrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(notificationWrapper);

        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference(), notificationWrapper.getCaseId());
    }

    @Test
    public void doNotSendSmsToGovNotifyWhenNotificationIsNotAnSms() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId("123").build(), Destination.builder().email("test@testing.com").sms(null).build(), null, new Reference(), null);
        when(factory.create(notificationWrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(notificationWrapper);

        verify(notificationSender, never()).sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference(), notification.getSmsSenderTemplate(), notificationWrapper.getCaseId());
    }

    @Test
    public void doNotSendEmailToGovNotifyWhenEmailTemplateIsBlank() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId("123").build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(notificationWrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(notificationWrapper);

        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference(), notificationWrapper.getCaseId());
    }

    @Test
    public void doNotSendSmsToGovNotifyWhenSmsTemplateIsBlank() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(notificationWrapper)).thenReturn(notification);
        notificationService.createAndSendNotification(notificationWrapper);

        verify(notificationSender, never()).sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference(), notification.getSmsSenderTemplate(), notificationWrapper.getCaseId());
    }

    @Test
    public void doNotSendEmailOrSmsWhenNoActiveSubscription() throws Exception {
        Subscription appellantSubscription = Subscription.builder().tya(APPEAL_NUMBER).email("test@email.com")
            .mobile(MOBILE_NUMBER_1).subscribeEmail("No").subscribeSms("No").build();

        response = SscsCaseData.builder().subscriptions(Subscriptions.builder().appellantSubscription(appellantSubscription).build()).caseReference(CASE_REFERENCE).build();
        SscsCaseDataWrapper wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(response).oldSscsCaseData(response).notificationEventType(APPEAL_WITHDRAWN_NOTIFICATION).build();

        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId("123").build(), Destination.builder().email(null).sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(notificationWrapper)).thenReturn(notification);

        notificationService.createAndSendNotification(new CcdNotificationWrapper(wrapper));

        verify(notificationSender, never()).sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference(), notification.getSmsSenderTemplate(), notificationWrapper.getCaseId());
        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference(), notificationWrapper.getCaseId());
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

        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference(), notificationWrapper.getCaseId());
    }

    @Test
    public void doNotSendNotificationWhenHearingTypeIsNotValidToSend() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), null, new Reference(), null);
        when(factory.create(notificationWrapper)).thenReturn(notification);
        when((notificationValidService).isHearingTypeValidToSendNotification(any(), any())).thenReturn(false);

        notificationService.createAndSendNotification(notificationWrapper);

        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference(), notificationWrapper.getCaseId());
    }

    @Test
    public void doNotSendNotificationsOutOfHours() {
        String emailTemplateId = "abc";
        String smsTemplateId = "123";
        Notification notification = new Notification(Template.builder().emailTemplateId(emailTemplateId).smsTemplateId(smsTemplateId).build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), null, new Reference(), null);
        SscsCaseDataWrapper wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(response).oldSscsCaseData(response).notificationEventType(QUESTION_ROUND_ISSUED_NOTIFICATION).build();
        notificationWrapper = new CohNotificationWrapper("someHearingId", wrapper);
        when(factory.create(notificationWrapper)).thenReturn(notification);
        when(outOfHoursCalculator.isItOutOfHours()).thenReturn(true);
        notificationService.createAndSendNotification(notificationWrapper);

        verify(notificationHandler, never()).sendNotification(any(), any(), any(), any());
        verify(notificationHandler).scheduleNotification(notificationWrapper);
    }

    @Test
    public void shouldSendEmailAndSmsToOldEmailAddressForEmailSubscriptionUpdateForPaperCase() {
        Subscription appellantNewSubscription = Subscription.builder().tya(APPEAL_NUMBER).email(NEWTEST_EMAIL_COM)
                .mobile(MOBILE_NUMBER_1).subscribeEmail(YES).subscribeSms(YES).build();
        Subscription appellantOldSubscription = Subscription.builder().tya(APPEAL_NUMBER).email("oldtest@email.com")
                .mobile(MOBILE_NUMBER_2).subscribeEmail(YES).subscribeSms(YES).build();

        SscsCaseData newSscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder().hearingType(AppealHearingType.PAPER.name()).benefitType(BenefitType.builder().code(PIP).build()).build())
                .subscriptions(Subscriptions.builder().appellantSubscription(appellantNewSubscription).build())
                .caseReference(CASE_REFERENCE).build();

        SscsCaseData oldSscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder().hearingType(AppealHearingType.PAPER.name()).benefitType(BenefitType.builder().code(PIP).build()).build())
                .subscriptions(Subscriptions.builder().appellantSubscription(appellantOldSubscription).build())
                .caseReference(CASE_REFERENCE).build();

        SscsCaseDataWrapper wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(newSscsCaseData).oldSscsCaseData(oldSscsCaseData).notificationEventType(SUBSCRIPTION_UPDATED_NOTIFICATION).build();
        notificationWrapper = new CcdNotificationWrapper(wrapper);

        Notification notification = new Notification(
                Template.builder().emailTemplateId(EMAIL_TEMPLATE_ID).smsTemplateId(SMS_TEMPLATE_ID).build(),
                Destination.builder().email(NEWTEST_EMAIL_COM).sms(MOBILE_NUMBER_2).build(), null, new Reference(), null);
        when(factory.create(notificationWrapper)).thenReturn(notification);
        when(notificationConfig.getTemplate(any(), any(), any(), any())).thenReturn(Template.builder().emailTemplateId(EMAIL_TEMPLATE_ID).smsTemplateId(SMS_TEMPLATE_ID).build());


        notificationService.createAndSendNotification(notificationWrapper);


        verify(notificationHandler, times(2)).sendNotification(eq(notificationWrapper), any(), eq(EMAIL), any(NotificationHandler.SendNotification.class));
        verify(notificationHandler, times(2)).sendNotification(eq(notificationWrapper), any(), eq(SMS), any(NotificationHandler.SendNotification.class));

    }


    @Test
    public void shouldNotSendEmailOrSmsToOldEmailAddressIfOldAndNewEmailAndSmsAreSame() {
        Subscription appellantNewSubscription = Subscription.builder().tya(APPEAL_NUMBER).email(SAMETEST_EMAIL_COM)
                .mobile(MOBILE_NUMBER_1).subscribeEmail(YES).subscribeSms(YES).build();
        Subscription appellantOldSubscription = Subscription.builder().tya(APPEAL_NUMBER).email(SAMETEST_EMAIL_COM)
                .mobile(MOBILE_NUMBER_1).subscribeEmail(YES).subscribeSms(YES).build();

        SscsCaseData newSscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder().hearingType(AppealHearingType.PAPER.name()).benefitType(BenefitType.builder().code(PIP).build()).build())
                .subscriptions(Subscriptions.builder().appellantSubscription(appellantNewSubscription).build())
                .caseReference(CASE_REFERENCE).build();

        SscsCaseData oldSscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder().hearingType(AppealHearingType.PAPER.name()).benefitType(BenefitType.builder().code(PIP).build()).build())
                .subscriptions(Subscriptions.builder().appellantSubscription(appellantOldSubscription).build())
                .caseReference(CASE_REFERENCE).build();

        SscsCaseDataWrapper wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(newSscsCaseData).oldSscsCaseData(oldSscsCaseData).notificationEventType(SUBSCRIPTION_UPDATED_NOTIFICATION).build();
        notificationWrapper = new CcdNotificationWrapper(wrapper);

        Notification notification = new Notification(
                Template.builder().emailTemplateId(EMAIL_TEMPLATE_ID).smsTemplateId(SMS_TEMPLATE_ID).build(),
                Destination.builder().email(NEWTEST_EMAIL_COM).sms(MOBILE_NUMBER_2).build(), null, new Reference(), null);
        when(factory.create(notificationWrapper)).thenReturn(notification);
        when(notificationConfig.getTemplate(any(), any(), any(), any())).thenReturn(Template.builder().emailTemplateId(EMAIL_TEMPLATE_ID).smsTemplateId(SMS_TEMPLATE_ID).build());


        notificationService.createAndSendNotification(notificationWrapper);


        verify(notificationHandler, times(1)).sendNotification(eq(notificationWrapper), any(), eq(EMAIL), any(NotificationHandler.SendNotification.class));
        verify(notificationHandler, times(1)).sendNotification(eq(notificationWrapper), any(), eq(SMS), any(NotificationHandler.SendNotification.class));

    }

    @Test
    public void shouldNotSendEmailAndSmsToOldEmailAddressIfOldEmailAddressAndSmsNotPresent() {
        Subscription appellantNewSubscription = Subscription.builder().tya(APPEAL_NUMBER).email(SAMETEST_EMAIL_COM)
                .mobile(MOBILE_NUMBER_1).subscribeEmail(YES).subscribeSms(YES).build();
        Subscription appellantOldSubscription = Subscription.builder().tya(APPEAL_NUMBER).build();

        SscsCaseData newSscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder().hearingType(AppealHearingType.PAPER.name()).benefitType(BenefitType.builder().code(PIP).build()).build())
                .subscriptions(Subscriptions.builder().appellantSubscription(appellantNewSubscription).build())
                .caseReference(CASE_REFERENCE).build();

        SscsCaseData oldSscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder().hearingType(AppealHearingType.PAPER.name()).benefitType(BenefitType.builder().code(PIP).build()).build())
                .subscriptions(Subscriptions.builder().appellantSubscription(appellantOldSubscription).build())
                .caseReference(CASE_REFERENCE).build();

        SscsCaseDataWrapper wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(newSscsCaseData).oldSscsCaseData(oldSscsCaseData).notificationEventType(SUBSCRIPTION_UPDATED_NOTIFICATION).build();
        notificationWrapper = new CcdNotificationWrapper(wrapper);

        Notification notification = new Notification(
                Template.builder().emailTemplateId(EMAIL_TEMPLATE_ID).smsTemplateId(SMS_TEMPLATE_ID).build(),
                Destination.builder().email(NEWTEST_EMAIL_COM).sms(MOBILE_NUMBER_2).build(), null, new Reference(), null);
        when(factory.create(notificationWrapper)).thenReturn(notification);
        when(notificationConfig.getTemplate(any(), any(), any(), any())).thenReturn(Template.builder().emailTemplateId(EMAIL_TEMPLATE_ID).smsTemplateId(SMS_TEMPLATE_ID).build());


        notificationService.createAndSendNotification(notificationWrapper);


        verify(notificationHandler, times(1)).sendNotification(eq(notificationWrapper), any(), eq(EMAIL), any(NotificationHandler.SendNotification.class));
        verify(notificationHandler, times(1)).sendNotification(eq(notificationWrapper), any(), eq(SMS), any(NotificationHandler.SendNotification.class));

    }
}
