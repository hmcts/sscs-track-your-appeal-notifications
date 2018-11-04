package uk.gov.hmcts.reform.sscs.service;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_WITHDRAWN_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.QUESTION_ROUND_ISSUED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SUBSCRIPTION_UPDATED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SYA_APPEAL_CREATED_NOTIFICATION;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscriptions;
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
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;

public class NotificationServiceTest {

    private static final String APPEAL_NUMBER = "GLSCRR";
    private static final String YES = "Yes";
    private static final String CASE_REFERENCE = "ABC123";
    private static final String EMAIL_TEMPLATE_ID = "email-template-id";
    private static final String SMS_TEMPLATE_ID = "sms-template-id";
    private static final String SAME_TEST_EMAIL_COM = "sametest@email.com";
    private static final String NEW_TEST_EMAIL_COM = "newtest@email.com";
    private static final String PIP = "PIP";
    private static final String EMAIL = "Email";
    private static final String SMS = "SMS";
    private static final String MOBILE_NUMBER_1 = "07983495065";
    private static final String MOBILE_NUMBER_2 = "07983495067";

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

    private SscsCaseData sscsCaseData;
    private Subscription appellantSubscription;
    private Subscription repsSubscription;
    private CcdNotificationWrapper ccdNotificationWrapper;
    private SscsCaseDataWrapper sscsCaseDataWrapper;
    private Notification notification;

    @Before
    public void setup() {
        initMocks(this);
        notificationService = new NotificationService(notificationSender, factory, reminderService,
                notificationValidService, notificationHandler, outOfHoursCalculator, notificationConfig);

        appellantSubscription = Subscription.builder()
                .tya(APPEAL_NUMBER)
                .email(EMAIL)
                .mobile(MOBILE_NUMBER_1)
                .subscribeEmail(YES)
                .subscribeSms(YES)
                .build();

        sscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder().hearingType(AppealHearingType.ORAL.name()).hearingOptions(HearingOptions.builder().wantsToAttend(YES).build()).build())
                .subscriptions(Subscriptions.builder().appellantSubscription(appellantSubscription).build())
                .caseReference(CASE_REFERENCE).build();
        sscsCaseDataWrapper = SscsCaseDataWrapper.builder().newSscsCaseData(sscsCaseData).oldSscsCaseData(sscsCaseData).notificationEventType(APPEAL_WITHDRAWN_NOTIFICATION).build();
        ccdNotificationWrapper = new CcdNotificationWrapper(sscsCaseDataWrapper);
        when(outOfHoursCalculator.isItOutOfHours()).thenReturn(false);
    }

    @Test
    public void givenAppealCreatedAndAppellantAndRepsSubscriptionsPresent_shouldSendNotificationToBoth() {
        ccdNotificationWrapper = buildNotificationWrapperForAppealCreatedNotificationTypeAndAppeallantAndRepsSubscriptions();

        given(notificationValidService.isHearingTypeValidToSendNotification(
                any(SscsCaseData.class), eq(SYA_APPEAL_CREATED_NOTIFICATION))).willReturn(true);

        given(notificationValidService.isNotificationStillValidToSend(anyList(), eq(SYA_APPEAL_CREATED_NOTIFICATION)))
                .willReturn(true);

        notification = new Notification(
                Template.builder()
                        .emailTemplateId(EMAIL_TEMPLATE_ID)
                        .smsTemplateId(null)
                        .build(),
                Destination.builder()
                        .email(EMAIL)
                        .sms(null)
                        .build(),
                null,
                new Reference(),
                null);

        given(factory.create(any(NotificationWrapper.class))).willReturn(notification);

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        then(notificationHandler).should(times(2)).sendNotification(
                eq(ccdNotificationWrapper), eq(EMAIL_TEMPLATE_ID), eq("Email"),
                any(NotificationHandler.SendNotification.class));

    }

    private CcdNotificationWrapper buildNotificationWrapperForAppealCreatedNotificationTypeAndAppeallantAndRepsSubscriptions() {
        appellantSubscription = Subscription.builder()
                .tya(APPEAL_NUMBER)
                .email(EMAIL)
                .subscribeEmail(YES)
                .build();
        repsSubscription = Subscription.builder()
                .tya(APPEAL_NUMBER)
                .email(EMAIL)
                .subscribeEmail(YES)
                .build();

        sscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder()
                        .hearingType(AppealHearingType.ORAL.name())
                        .hearingOptions(HearingOptions.builder()
                                .wantsToAttend(YES)
                                .build())
                        .build())
                .subscriptions(Subscriptions.builder()
                        .appellantSubscription(appellantSubscription)
                        .representativeSubscription(repsSubscription)
                        .build())
                .caseReference(CASE_REFERENCE)
                .hearings(Collections.singletonList(Hearing.builder().build()))
                .build();

        sscsCaseDataWrapper = SscsCaseDataWrapper.builder()
                .newSscsCaseData(sscsCaseData)
                .notificationEventType(SYA_APPEAL_CREATED_NOTIFICATION)
                .build();

        return new CcdNotificationWrapper(sscsCaseDataWrapper);
    }

    @Test
    public void sendEmailToGovNotifyWhenNotificationIsAnEmailAndTemplateNotBlank() {
        String emailTemplateId = "abc";
        Notification notification = new Notification(Template.builder().emailTemplateId(emailTemplateId).smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), null, new Reference(), null);
        when((notificationValidService).isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when((notificationValidService).isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(factory.create(ccdNotificationWrapper)).thenReturn(notification);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        verify(notificationHandler, times(1)).sendNotification(eq(ccdNotificationWrapper), eq(emailTemplateId), eq(EMAIL), any(NotificationHandler.SendNotification.class));
    }

    @Test
    public void sendSmsToGovNotifyWhenNotificationIsAnSmsAndTemplateNotBlank() {
        String smsTemplateId = "123";
        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId(smsTemplateId).build(), Destination.builder().email(null).sms("07823456746").build(), null, new Reference(), null);
        when((notificationValidService).isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when((notificationValidService).isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(factory.create(ccdNotificationWrapper)).thenReturn(notification);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        verify(notificationHandler, times(1)).sendNotification(eq(ccdNotificationWrapper), eq(smsTemplateId), eq(SMS), any(NotificationHandler.SendNotification.class));
    }

    @Test
    public void sendSmsAndEmailToGovNotifyWhenNotificationIsAnSmsAndEmailAndTemplateNotBlank() {
        String emailTemplateId = "abc";
        String smsTemplateId = "123";
        Notification notification = new Notification(Template.builder().emailTemplateId(emailTemplateId).smsTemplateId(smsTemplateId).build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), null, new Reference(), null);
        when((notificationValidService).isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when((notificationValidService).isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(factory.create(ccdNotificationWrapper)).thenReturn(notification);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        verify(notificationHandler, times(1)).sendNotification(eq(ccdNotificationWrapper), eq(emailTemplateId), eq(EMAIL), any(NotificationHandler.SendNotification.class));
        verify(notificationHandler, times(1)).sendNotification(eq(ccdNotificationWrapper), eq(smsTemplateId), eq(SMS), any(NotificationHandler.SendNotification.class));
    }

    @Test
    public void doNotSendEmailToGovNotifyWhenNotificationIsNotAnEmail() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId("123").build(), Destination.builder().email(null).sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(ccdNotificationWrapper)).thenReturn(notification);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference(), ccdNotificationWrapper.getCaseId());
    }

    @Test
    public void doNotSendSmsToGovNotifyWhenNotificationIsNotAnSms() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId("123").build(), Destination.builder().email("test@testing.com").sms(null).build(), null, new Reference(), null);
        when(factory.create(ccdNotificationWrapper)).thenReturn(notification);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        verify(notificationSender, never()).sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference(), notification.getSmsSenderTemplate(), ccdNotificationWrapper.getCaseId());
    }

    @Test
    public void doNotSendEmailToGovNotifyWhenEmailTemplateIsBlank() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId("123").build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(ccdNotificationWrapper)).thenReturn(notification);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference(), ccdNotificationWrapper.getCaseId());
    }

    @Test
    public void doNotSendSmsToGovNotifyWhenSmsTemplateIsBlank() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(ccdNotificationWrapper)).thenReturn(notification);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        verify(notificationSender, never()).sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference(), notification.getSmsSenderTemplate(), ccdNotificationWrapper.getCaseId());
    }

    @Test
    public void doNotSendEmailOrSmsWhenNoActiveSubscription() throws Exception {
        Subscription appellantSubscription = Subscription.builder().tya(APPEAL_NUMBER).email("test@email.com")
                .mobile(MOBILE_NUMBER_1).subscribeEmail("No").subscribeSms("No").build();

        sscsCaseData = SscsCaseData.builder().subscriptions(Subscriptions.builder().appellantSubscription(appellantSubscription).build()).caseReference(CASE_REFERENCE).build();
        SscsCaseDataWrapper wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(sscsCaseData).oldSscsCaseData(sscsCaseData).notificationEventType(APPEAL_WITHDRAWN_NOTIFICATION).build();

        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId("123").build(), Destination.builder().email(null).sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(ccdNotificationWrapper)).thenReturn(notification);

        notificationService.manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));

        verify(notificationSender, never()).sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference(), notification.getSmsSenderTemplate(), ccdNotificationWrapper.getCaseId());
        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference(), ccdNotificationWrapper.getCaseId());
    }

    @Test
    public void createsReminders() {

        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId("123").build(), Destination.builder().email(null).sms("07823456746").build(), null, new Reference(), null);

        when((notificationValidService).isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when((notificationValidService).isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(factory.create(ccdNotificationWrapper)).thenReturn(notification);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        verify(reminderService).createReminders(ccdNotificationWrapper);
    }

    @Test
    public void doNotSendNotificationWhenNotificationNotValidToSend() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), null, new Reference(), null);
        when(factory.create(ccdNotificationWrapper)).thenReturn(notification);
        when((notificationValidService).isNotificationStillValidToSend(any(), any())).thenReturn(false);

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference(), ccdNotificationWrapper.getCaseId());
    }

    @Test
    public void doNotSendNotificationWhenHearingTypeIsNotValidToSend() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), null, new Reference(), null);
        when(factory.create(ccdNotificationWrapper)).thenReturn(notification);
        when((notificationValidService).isHearingTypeValidToSendNotification(any(), any())).thenReturn(false);

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference(), ccdNotificationWrapper.getCaseId());
    }

    @Test
    public void doNotSendNotificationsOutOfHours() {
        String emailTemplateId = "abc";
        String smsTemplateId = "123";
        Notification notification = new Notification(Template.builder().emailTemplateId(emailTemplateId).smsTemplateId(smsTemplateId).build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), null, new Reference(), null);
        SscsCaseDataWrapper wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(sscsCaseData).oldSscsCaseData(sscsCaseData).notificationEventType(QUESTION_ROUND_ISSUED_NOTIFICATION).build();
        ccdNotificationWrapper = new CohNotificationWrapper("someHearingId", wrapper);
        when((notificationValidService).isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when((notificationValidService).isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(factory.create(ccdNotificationWrapper)).thenReturn(notification);
        when(outOfHoursCalculator.isItOutOfHours()).thenReturn(true);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        verify(notificationHandler, never()).sendNotification(any(), any(), any(), any());
        verify(notificationHandler).scheduleNotification(ccdNotificationWrapper);
    }

    @Test
    public void shouldSendEmailAndSmsToOldEmailAddressForEmailSubscriptionUpdateForPaperCase() {
        Subscription appellantNewSubscription = Subscription.builder().tya(APPEAL_NUMBER).email(NEW_TEST_EMAIL_COM)
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
        ccdNotificationWrapper = new CcdNotificationWrapper(wrapper);

        Notification notification = new Notification(
                Template.builder().emailTemplateId(EMAIL_TEMPLATE_ID).smsTemplateId(SMS_TEMPLATE_ID).build(),
                Destination.builder().email(NEW_TEST_EMAIL_COM).sms(MOBILE_NUMBER_2).build(), null, new Reference(), null);
        when((notificationValidService).isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when((notificationValidService).isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(factory.create(ccdNotificationWrapper)).thenReturn(notification);
        when(notificationConfig.getTemplate(any(), any(), any(), any())).thenReturn(Template.builder().emailTemplateId(EMAIL_TEMPLATE_ID).smsTemplateId(SMS_TEMPLATE_ID).build());

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        verify(notificationHandler, times(2)).sendNotification(eq(ccdNotificationWrapper), any(), eq(EMAIL), any(NotificationHandler.SendNotification.class));
        verify(notificationHandler, times(2)).sendNotification(eq(ccdNotificationWrapper), any(), eq(SMS), any(NotificationHandler.SendNotification.class));
    }


    @Test
    public void shouldNotSendEmailOrSmsToOldEmailAddressIfOldAndNewEmailAndSmsAreSame() {
        Subscription appellantNewSubscription = Subscription.builder().tya(APPEAL_NUMBER).email(SAME_TEST_EMAIL_COM)
                .mobile(MOBILE_NUMBER_1).subscribeEmail(YES).subscribeSms(YES).build();
        Subscription appellantOldSubscription = Subscription.builder().tya(APPEAL_NUMBER).email(SAME_TEST_EMAIL_COM)
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
        ccdNotificationWrapper = new CcdNotificationWrapper(wrapper);

        Notification notification = new Notification(
                Template.builder().emailTemplateId(EMAIL_TEMPLATE_ID).smsTemplateId(SMS_TEMPLATE_ID).build(),
                Destination.builder().email(NEW_TEST_EMAIL_COM).sms(MOBILE_NUMBER_2).build(), null, new Reference(), null);

        when((notificationValidService).isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when((notificationValidService).isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(factory.create(ccdNotificationWrapper)).thenReturn(notification);
        when(notificationConfig.getTemplate(any(), any(), any(), any())).thenReturn(Template.builder().emailTemplateId(EMAIL_TEMPLATE_ID).smsTemplateId(SMS_TEMPLATE_ID).build());

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        verify(notificationHandler, times(1)).sendNotification(eq(ccdNotificationWrapper), any(), eq(EMAIL), any(NotificationHandler.SendNotification.class));
        verify(notificationHandler, times(1)).sendNotification(eq(ccdNotificationWrapper), any(), eq(SMS), any(NotificationHandler.SendNotification.class));

    }

    @Test
    public void shouldNotSendEmailAndSmsToOldEmailAddressIfOldEmailAddressAndSmsNotPresent() {
        Subscription appellantNewSubscription = Subscription.builder().tya(APPEAL_NUMBER).email(SAME_TEST_EMAIL_COM)
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
        ccdNotificationWrapper = new CcdNotificationWrapper(wrapper);

        Notification notification = new Notification(
                Template.builder().emailTemplateId(EMAIL_TEMPLATE_ID).smsTemplateId(SMS_TEMPLATE_ID).build(),
                Destination.builder().email(NEW_TEST_EMAIL_COM).sms(MOBILE_NUMBER_2).build(), null, new Reference(), null);

        when((notificationValidService).isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when((notificationValidService).isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(factory.create(ccdNotificationWrapper)).thenReturn(notification);
        when(notificationConfig.getTemplate(any(), any(), any(), any())).thenReturn(Template.builder().emailTemplateId(EMAIL_TEMPLATE_ID).smsTemplateId(SMS_TEMPLATE_ID).build());

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        verify(notificationHandler, times(1)).sendNotification(eq(ccdNotificationWrapper), any(), eq(EMAIL), any(NotificationHandler.SendNotification.class));
        verify(notificationHandler, times(1)).sendNotification(eq(ccdNotificationWrapper), any(), eq(SMS), any(NotificationHandler.SendNotification.class));

    }
}
