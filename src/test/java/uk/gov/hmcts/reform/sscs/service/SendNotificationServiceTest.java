package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.service.LetterUtils.getAddressToUseForLetter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.domain.notify.Destination;
import uk.gov.hmcts.reform.sscs.domain.notify.Notification;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.domain.notify.Template;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;

@RunWith(JUnitParamsRunner.class)
public class SendNotificationServiceTest {
    private static final String YES = "Yes";
    private static final String CASE_REFERENCE = "ABC123";
    private static final String CASE_ID = "1000001";

    private static Appellant APPELLANT_WITH_ADDRESS = Appellant.builder()
            .name(Name.builder().firstName("Ap").lastName("pellant").build())
            .address(Address.builder().line1("Appellant Line 1").town("Appellant Town").county("Appellant County").postcode("AP9 3LL").build())
            .build();

    private static Appointee APPOINTEE_WITH_ADDRESS = Appointee.builder()
            .address(Address.builder().line1("Appointee Line 1").town("Appointee Town").county("Appointee County").postcode("AP9 0IN").build())
            .name(Name.builder().firstName("Ap").lastName("Pointee").build())
            .build();

    protected static Appellant APPELLANT_WITH_ADDRESS_AND_APPOINTEE = Appellant.builder()
            .name(Name.builder().firstName("Ap").lastName("Pellant").build())
            .address(Address.builder().line1("Appellant Line 1").town("Appellant Town").county("Appellant County").postcode("AP9 3LL").build())
            .appointee(APPOINTEE_WITH_ADDRESS)
            .build();

    private static Subscription SMS_SUBSCRIPTION = Subscription.builder().mobile("07831292000").subscribeSms("Yes").build();

    private static Notification SMS_NOTIFICATION = Notification.builder()
        .destination(Destination.builder().sms("07831292000").build())
        .template(Template.builder().smsTemplateId("someSmsTemplateId").build())
        .build();

    private static Subscription EMAIL_SUBSCRIPTION = Subscription.builder().email("test@some.com").subscribeEmail("Yes").build();

    private static Notification EMAIL_NOTIFICATION = Notification.builder()
        .destination(Destination.builder().email("test@some.com").build())
        .template(Template.builder().emailTemplateId("someEmailTemplateId").build())
        .build();

    private static Subscription EMPTY_SUBSCRIPTION = Subscription.builder().build();

    private static Notification EMPTY_TEMPLATE_NOTIFICATION = Notification.builder()
        .destination(Destination.builder().build())
        .template(Template.builder().build())
        .build();

    private static Notification LETTER_NOTIFICATION = Notification.builder()
        .destination(Destination.builder().build())
        .template(Template.builder().letterTemplateId("someLetterTemplateId").build())
        .placeholders(new HashMap<>())
        .build();

    @Mock
    private NotificationSender notificationSender;

    @Mock
    private EvidenceManagementService evidenceManagementService;

    @Mock
    private SscsGeneratePdfService pdfService;

    @Mock
    private NotificationHandler notificationHandler;

    private SendNotificationService classUnderTest;

    @Before
    public void setup() {
        initMocks(this);

        classUnderTest = new SendNotificationService(notificationSender, evidenceManagementService, pdfService, notificationHandler);
        classUnderTest.bundledLettersOn = true;
    }

    @Test
    public void getAppellantAddressToUseForLetter() {
        Address expectedAddress = APPELLANT_WITH_ADDRESS.getAddress();
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS);

        Address actualAddress = getAddressToUseForLetter(wrapper);
        assertEquals(expectedAddress.getLine1(), actualAddress.getLine1());
        assertEquals(expectedAddress.getLine2(), actualAddress.getLine2());
        assertEquals(expectedAddress.getTown(), actualAddress.getTown());
        assertEquals(expectedAddress.getCounty(), actualAddress.getCounty());
        assertEquals(expectedAddress.getPostcode(), actualAddress.getPostcode());
    }

    @Test
    public void getAppointeeAddressToUseForLetter() {
        Address expectedAddress = APPOINTEE_WITH_ADDRESS.getAddress();
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS_AND_APPOINTEE);

        Address actualAddress = getAddressToUseForLetter(wrapper);
        assertEquals(expectedAddress.getLine1(), actualAddress.getLine1());
        assertEquals(expectedAddress.getLine2(), actualAddress.getLine2());
        assertEquals(expectedAddress.getTown(), actualAddress.getTown());
        assertEquals(expectedAddress.getCounty(), actualAddress.getCounty());
        assertEquals(expectedAddress.getPostcode(), actualAddress.getPostcode());
    }

    @Test
    public void doNotSendFallbackLetterNoficationToAppellantWhenSubscribedForSms() {
        SubscriptionWithType appellantSmsSubscription = new SubscriptionWithType(SMS_SUBSCRIPTION, SubscriptionType.APPELLANT);

        classUnderTest.sendEmailSmsLetterNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.INTERLOC_VALID_APPEAL), SMS_SUBSCRIPTION, SMS_NOTIFICATION, appellantSmsSubscription);

        verify(notificationHandler).sendNotification(any(), eq(SMS_NOTIFICATION.getSmsTemplate()), any(), any());
    }

    @Test
    public void doNotSendFallbackLetterNoficationToAppellantWhenSubscribedForEmail() {
        SubscriptionWithType appellantEmailSubscription = new SubscriptionWithType(EMAIL_SUBSCRIPTION, SubscriptionType.APPELLANT);

        classUnderTest.sendEmailSmsLetterNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.INTERLOC_VALID_APPEAL), EMAIL_SUBSCRIPTION, EMAIL_NOTIFICATION, appellantEmailSubscription);

        verify(notificationHandler).sendNotification(any(), eq(EMAIL_NOTIFICATION.getEmailTemplate()), any(), any());
    }

    @Test
    public void doNotSendFallbackLetterNoficationToAppellantWhenNoLetterTemplate() {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, SubscriptionType.APPELLANT);

        classUnderTest.sendEmailSmsLetterNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.INTERLOC_VALID_APPEAL), EMPTY_SUBSCRIPTION, EMPTY_TEMPLATE_NOTIFICATION, appellantEmptySubscription);

        verifyZeroInteractions(notificationHandler);
    }

    @Test
    public void sendFallbackLetterNoficationToAppellant() {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, SubscriptionType.APPELLANT);

        classUnderTest.sendEmailSmsLetterNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.INTERLOC_VALID_APPEAL), EMPTY_SUBSCRIPTION, LETTER_NOTIFICATION, appellantEmptySubscription);

        verify(notificationHandler).sendNotification(any(), eq(LETTER_NOTIFICATION.getLetterTemplate()), any(), any());
    }

    @Test
    public void doNotSendFallbackLetterNoficationToRepWhenSubscribedForSms() {
        SubscriptionWithType appellantSmsSubscription = new SubscriptionWithType(SMS_SUBSCRIPTION, SubscriptionType.REPRESENTATIVE);

        classUnderTest.sendEmailSmsLetterNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.INTERLOC_VALID_APPEAL), SMS_SUBSCRIPTION, SMS_NOTIFICATION, appellantSmsSubscription);

        verify(notificationHandler).sendNotification(any(), eq(SMS_NOTIFICATION.getSmsTemplate()), any(), any());
    }

    @Test
    public void doNotSendFallbackLetterNoficationToRepWhenSubscribedForEmail() {
        SubscriptionWithType appellantEmailSubscription = new SubscriptionWithType(EMAIL_SUBSCRIPTION, SubscriptionType.REPRESENTATIVE);

        classUnderTest.sendEmailSmsLetterNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.INTERLOC_VALID_APPEAL), EMAIL_SUBSCRIPTION, EMAIL_NOTIFICATION, appellantEmailSubscription);

        verify(notificationHandler).sendNotification(any(), eq(EMAIL_NOTIFICATION.getEmailTemplate()), any(), any());
    }

    @Test
    public void doNotSendFallbackLetterNoficationToRepWhenNoLetterTemplate() {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, SubscriptionType.REPRESENTATIVE);

        classUnderTest.sendEmailSmsLetterNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.INTERLOC_VALID_APPEAL), EMPTY_SUBSCRIPTION, EMPTY_TEMPLATE_NOTIFICATION, appellantEmptySubscription);

        verifyZeroInteractions(notificationHandler);
    }

    @Test
    public void sendFallbackLetterNoficationToRep() {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, SubscriptionType.REPRESENTATIVE);

        classUnderTest.sendEmailSmsLetterNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.INTERLOC_VALID_APPEAL), EMPTY_SUBSCRIPTION, LETTER_NOTIFICATION, appellantEmptySubscription);

        verify(notificationHandler).sendNotification(any(), eq(LETTER_NOTIFICATION.getLetterTemplate()), any(), any());
    }

    private CcdNotificationWrapper buildBaseWrapper(Appellant appellant) {
        return buildBaseWrapper(appellant, NotificationEventType.STRUCK_OUT);
    }

    private CcdNotificationWrapper buildBaseWrapper(Appellant appellant, NotificationEventType eventType) {
        SscsCaseData sscsCaseDataWithDocuments = SscsCaseData.builder()
                .appeal(
                        Appeal
                                .builder()
                                .hearingType(AppealHearingType.ORAL.name())
                                .hearingOptions(HearingOptions.builder().wantsToAttend(YES).build())
                                .appellant(appellant)
                                .rep(null)
                                .build())
                .subscriptions(Subscriptions.builder().appellantSubscription(Subscription.builder()
                        .tya("GLSCRR")
                        .email("Email")
                        .mobile("07983495065")
                        .subscribeEmail(YES)
                        .subscribeSms(YES)
                        .build()).build())
                .caseReference(CASE_REFERENCE)
                .ccdCaseId(CASE_ID)
                .sscsDocument(new ArrayList<>(Collections.singletonList(null)))
                .build();

        SscsCaseDataWrapper struckOutSscsCaseDataWrapper = SscsCaseDataWrapper.builder()
                .newSscsCaseData(sscsCaseDataWithDocuments)
                .oldSscsCaseData(sscsCaseDataWithDocuments)
                .notificationEventType(eventType)
                .build();
        return new CcdNotificationWrapper(struckOutSscsCaseDataWrapper);
    }

}
