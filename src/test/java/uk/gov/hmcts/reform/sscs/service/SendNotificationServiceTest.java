package uk.gov.hmcts.reform.sscs.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VALID_APPEAL;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.REP_SALUTATION;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.*;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.service.LetterUtils.getAddressToUseForLetter;
import static uk.gov.hmcts.reform.sscs.service.NotificationServiceTest.verifyExpectedErrorLogMessage;
import static uk.gov.hmcts.reform.sscs.service.NotificationServiceTest.verifyNoErrorsLogged;
import static uk.gov.hmcts.reform.sscs.service.NotificationValidService.BUNDLED_LETTER_EVENT_TYPES;
import static uk.gov.hmcts.reform.sscs.service.NotificationValidService.FALLBACK_LETTER_SUBSCRIPTION_TYPES;
import static uk.gov.hmcts.reform.sscs.service.SendNotificationHelper.getRepSalutation;
import static uk.gov.hmcts.reform.sscs.service.SendNotificationService.getBundledLetterDocumentUrl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import java.time.LocalDate;
import java.util.*;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
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
import uk.gov.hmcts.reform.sscs.service.docmosis.PdfLetterService;
import uk.gov.service.notify.NotificationClientException;

@RunWith(JUnitParamsRunner.class)
public class SendNotificationServiceTest {
    private static final String YES = "Yes";
    private static final String CASE_REFERENCE = "ABC123";
    private static final String CASE_ID = "1000001";

    private static Appellant APPELLANT_WITH_NO_ADDRESS = Appellant.builder()
        .name(Name.builder().firstName("Ap").lastName("pellant").build())
        .build();

    private static Appellant APPELLANT_WITH_EMPTY_ADDRESS = Appellant.builder()
        .name(Name.builder().firstName("Ap").lastName("pellant").build())
        .address(Address.builder().line1("").postcode("").build())
        .build();

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

    protected static Representative REP_WITH_ADDRESS = Representative.builder()
        .name(Name.builder().firstName("Re").lastName("Presentative").build())
        .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 3LL").build())
        .build();

    private static Representative REP_ORG_WITH_ADDRESS = Representative.builder()
        .organisation("Rep Org")
        .address(Address.builder().line1("Rep Org Line 1").town("Rep Town").county("Rep County").postcode("RE9 3LL").build())
        .build();

    private static Representative REP_ORG_WITH_NAME_AND_ADDRESS = Representative.builder()
        .organisation("Rep Org")
        .name(Name.builder().firstName("Re").lastName("Presentative").build())
        .address(Address.builder().line1("Rep Org Line 1").town("Rep Town").county("Rep County").postcode("RE9 3LL").build())
        .build();

    private static Subscription SMS_SUBSCRIPTION = Subscription.builder().mobile("07831292000").subscribeSms("Yes").wantSmsNotifications("Yes").build();

    private static Notification SMS_NOTIFICATION = Notification.builder()
        .destination(Destination.builder().sms("07831292000").build())
        .template(Template.builder().smsTemplateId(Arrays.asList("someSmsTemplateId")).build())
        .build();

    private static Notification WELSH_SMS_NOTIFICATION = Notification.builder()
            .destination(Destination.builder().sms("07831292000").build())
            .template(Template.builder().smsTemplateId(Arrays.asList("englishSmsTemplateId", "welshSmsTemplateId")).build())
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

    private static Notification DOCMOSIS_LETTER_NOTIFICATION = Notification.builder()
            .destination(Destination.builder().build())
            .template(Template.builder().docmosisTemplateId("AWord.doc").letterTemplateId("someLetterTemplateId").build())
            .placeholders(new HashMap<>())
            .build();
    @Mock
    private NotificationSender notificationSender;

    @Mock
    private EvidenceManagementService evidenceManagementService;

    @Mock
    private NotificationHandler notificationHandler;

    @Mock
    private NotificationValidService notificationValidService;

    @Mock
    private PdfLetterService pdfLetterService;

    private SendNotificationService classUnderTest;

    @Mock
    private Appender<ILoggingEvent> mockAppender;
    @Captor
    private ArgumentCaptor captorLoggingEvent;


    @Captor
    private ArgumentCaptor<String> smsTemplateIdCaptor;

    @Before
    public void setup() {
        initMocks(this);

        classUnderTest = new SendNotificationService(notificationSender, evidenceManagementService, notificationHandler, notificationValidService, pdfLetterService);

        Logger logger = (Logger) LoggerFactory.getLogger(SendNotificationService.class.getName());
        logger.addAppender(mockAppender);
    }

    @Test
    public void getAppellantAddressToUseForLetter() {
        Address expectedAddress = APPELLANT_WITH_ADDRESS.getAddress();
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS);

        Address actualAddress = getAddressToUseForLetter(wrapper, APPELLANT);
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

        Address actualAddress = getAddressToUseForLetter(wrapper, APPOINTEE);
        assertEquals(expectedAddress.getLine1(), actualAddress.getLine1());
        assertEquals(expectedAddress.getLine2(), actualAddress.getLine2());
        assertEquals(expectedAddress.getTown(), actualAddress.getTown());
        assertEquals(expectedAddress.getCounty(), actualAddress.getCounty());
        assertEquals(expectedAddress.getPostcode(), actualAddress.getPostcode());
    }

    @Test
    public void getRepAddressToUseForLetter() {
        Address expectedAddress = REP_WITH_ADDRESS.getAddress();
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS_AND_APPOINTEE, STRUCK_OUT, REP_WITH_ADDRESS);

        Address actualAddress = getAddressToUseForLetter(wrapper, REPRESENTATIVE);
        assertEquals(expectedAddress.getLine1(), actualAddress.getLine1());
        assertEquals(expectedAddress.getLine2(), actualAddress.getLine2());
        assertEquals(expectedAddress.getTown(), actualAddress.getTown());
        assertEquals(expectedAddress.getCounty(), actualAddress.getCounty());
        assertEquals(expectedAddress.getPostcode(), actualAddress.getPostcode());
    }

    @Test
    public void doNotSendFallbackLetterNotificationToAppellantWhenSubscribedForSms() {
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        SubscriptionWithType appellantSmsSubscription = new SubscriptionWithType(SMS_SUBSCRIPTION, APPELLANT);
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, CASE_UPDATED, READY_TO_LIST.getId());
        classUnderTest.sendEmailSmsLetterNotification(wrapper, SMS_NOTIFICATION, appellantSmsSubscription, FALLBACK_LETTER_SUBSCRIPTION_TYPES.get(0));

        verify(notificationHandler).sendNotification(any(), eq(SMS_NOTIFICATION.getSmsTemplate().get(0)), any(), any());
        verifyExpectedErrorLogMessage(mockAppender, captorLoggingEvent, wrapper.getNewSscsCaseData().getCcdCaseId(), "Did not send a notification for event");
    }

    @Test
    public void doNotSendFallbackLetterNotificationToAppellantWhenSubscribedForSms_Welsh() {
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        SubscriptionWithType appellantSmsSubscription = new SubscriptionWithType(SMS_SUBSCRIPTION, APPELLANT);
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, CASE_UPDATED, READY_TO_LIST.getId());
        classUnderTest.sendEmailSmsLetterNotification(wrapper, WELSH_SMS_NOTIFICATION, appellantSmsSubscription, FALLBACK_LETTER_SUBSCRIPTION_TYPES.get(0));

        verify(notificationHandler, times(2)).sendNotification(any(), smsTemplateIdCaptor.capture(), any(), any());
        List<String> smsTemplateIdCaptorAllValues = smsTemplateIdCaptor.getAllValues();
        assertThat(smsTemplateIdCaptorAllValues, Matchers.contains(WELSH_SMS_NOTIFICATION.getTemplate().getSmsTemplateId().get(0), WELSH_SMS_NOTIFICATION.getTemplate().getSmsTemplateId().get(1)));
        verifyExpectedErrorLogMessage(mockAppender, captorLoggingEvent, wrapper.getNewSscsCaseData().getCcdCaseId(), "Did not send a notification for event");
    }

    @Test
    public void noSmstemplateSet() {
        Notification notification = Notification.builder()
                .destination(Destination.builder().sms("07831292000").build())
                .template(Template.builder().smsTemplateId(Collections.EMPTY_LIST).build())
                .build();
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        SubscriptionWithType appellantSmsSubscription = new SubscriptionWithType(SMS_SUBSCRIPTION, APPELLANT);
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, CASE_UPDATED, READY_TO_LIST.getId());
        boolean result =  classUnderTest.sendEmailSmsLetterNotification(wrapper, notification, appellantSmsSubscription, FALLBACK_LETTER_SUBSCRIPTION_TYPES.get(0));
        assertThat(result, CoreMatchers.equalTo(false));
        verify(notificationHandler, never()).sendNotification(any(), any(), any(), any());
        verifyExpectedErrorLogMessage(mockAppender, captorLoggingEvent, wrapper.getNewSscsCaseData().getCcdCaseId(), "Did not send a notification for event");
    }

    @Test
    public void nullSmstemplateSet() {
        Notification notification = Notification.builder()
                .destination(Destination.builder().sms("07831292000").build())
                .template(Template.builder().build())
                .build();
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        SubscriptionWithType appellantSmsSubscription = new SubscriptionWithType(SMS_SUBSCRIPTION, APPELLANT);
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, CASE_UPDATED, READY_TO_LIST.getId());
        boolean result =  classUnderTest.sendEmailSmsLetterNotification(wrapper, notification, appellantSmsSubscription, FALLBACK_LETTER_SUBSCRIPTION_TYPES.get(0));
        assertThat(result, CoreMatchers.equalTo(false));
        verify(notificationHandler, never()).sendNotification(any(), any(), any(), any());
        verifyExpectedErrorLogMessage(mockAppender, captorLoggingEvent, wrapper.getNewSscsCaseData().getCcdCaseId(), "Did not send a notification for event");
    }

    @Test
    public void firstSmsFailed() {
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(notificationHandler.sendNotification(any(), anyString(), any(), any())).thenReturn(false, true);
        SubscriptionWithType appellantSmsSubscription = new SubscriptionWithType(SMS_SUBSCRIPTION, APPELLANT);
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, CASE_UPDATED, READY_TO_LIST.getId());
        boolean result =  classUnderTest.sendEmailSmsLetterNotification(wrapper, WELSH_SMS_NOTIFICATION, appellantSmsSubscription, FALLBACK_LETTER_SUBSCRIPTION_TYPES.get(0));

        assertThat(result, CoreMatchers.equalTo(false));

        verify(notificationHandler, times(2)).sendNotification(any(), any(), any(), any());
        verifyExpectedErrorLogMessage(mockAppender, captorLoggingEvent, wrapper.getNewSscsCaseData().getCcdCaseId(), "Did not send a notification for event");
    }

    @Test
    public void bothSmsSuccessfull() {
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(notificationHandler.sendNotification(any(), any(), any(), any())).thenReturn(true);
        SubscriptionWithType appellantSmsSubscription = new SubscriptionWithType(SMS_SUBSCRIPTION, APPELLANT);
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, CASE_UPDATED, READY_TO_LIST.getId());
        boolean result =  classUnderTest.sendEmailSmsLetterNotification(wrapper, WELSH_SMS_NOTIFICATION, appellantSmsSubscription, FALLBACK_LETTER_SUBSCRIPTION_TYPES.get(0));

        assertThat(result, CoreMatchers.equalTo(true));
        verify(notificationHandler, times(2)).sendNotification(any(), any(), any(), any());
    }

    @Test
    public void doNotSendFallbackLetterNotificationToAppellantWhenSubscribedForEmail() {
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        SubscriptionWithType appellantEmailSubscription = new SubscriptionWithType(EMAIL_SUBSCRIPTION, APPELLANT);
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, CASE_UPDATED, READY_TO_LIST.getId());
        classUnderTest.sendEmailSmsLetterNotification(wrapper, EMAIL_NOTIFICATION, appellantEmailSubscription, FALLBACK_LETTER_SUBSCRIPTION_TYPES.get(0));

        verify(notificationHandler).sendNotification(any(), eq(EMAIL_NOTIFICATION.getEmailTemplate()), any(), any());
        verifyExpectedErrorLogMessage(mockAppender, captorLoggingEvent, wrapper.getNewSscsCaseData().getCcdCaseId(), "Did not send a notification for event");
    }

    @Test
    public void doNotSendFallbackLetterNotificationToAppellantWhenNoLetterTemplate() {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, APPELLANT);

        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, CASE_UPDATED, READY_TO_LIST.getId());
        classUnderTest.sendEmailSmsLetterNotification(wrapper, EMPTY_TEMPLATE_NOTIFICATION, appellantEmptySubscription, FALLBACK_LETTER_SUBSCRIPTION_TYPES.get(0));

        verifyNoInteractions(notificationHandler);
        verifyExpectedErrorLogMessage(mockAppender, captorLoggingEvent, wrapper.getNewSscsCaseData().getCcdCaseId(), "Did not send a notification for event");
    }

    @Test
    public void sendFallbackLetterNotificationToAppellant() {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, APPELLANT);

        when(notificationValidService.isFallbackLetterRequiredForSubscriptionType(any(), any(), any())).thenReturn(true);
        when(notificationHandler.sendNotification(any(), eq(LETTER_NOTIFICATION.getLetterTemplate()), any(), any())).thenReturn(true);

        classUnderTest.sendEmailSmsLetterNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, READY_TO_LIST.getId()), LETTER_NOTIFICATION, appellantEmptySubscription, FALLBACK_LETTER_SUBSCRIPTION_TYPES.get(0));

        verify(notificationHandler).sendNotification(any(), eq(LETTER_NOTIFICATION.getLetterTemplate()), any(), any());
        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    public void doNotSendFallbackLetterNotificationToRepWhenSubscribedForSms() {
        SubscriptionWithType appellantSmsSubscription = new SubscriptionWithType(SMS_SUBSCRIPTION, SubscriptionType.REPRESENTATIVE);

        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, CASE_UPDATED, REP_WITH_ADDRESS);
        classUnderTest.sendEmailSmsLetterNotification(wrapper, SMS_NOTIFICATION, appellantSmsSubscription, FALLBACK_LETTER_SUBSCRIPTION_TYPES.get(0));

        verify(notificationHandler).sendNotification(any(), eq(SMS_NOTIFICATION.getSmsTemplate().get(0)), any(), any());
        verifyExpectedErrorLogMessage(mockAppender, captorLoggingEvent, wrapper.getNewSscsCaseData().getCcdCaseId(), "Did not send a notification for event");
    }

    @Test
    public void doNotSendFallbackLetterNotificationToRepWhenSubscribedForEmail() {
        SubscriptionWithType appellantEmailSubscription = new SubscriptionWithType(EMAIL_SUBSCRIPTION, SubscriptionType.REPRESENTATIVE);

        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, CASE_UPDATED, REP_WITH_ADDRESS);
        classUnderTest.sendEmailSmsLetterNotification(wrapper, EMAIL_NOTIFICATION, appellantEmailSubscription, FALLBACK_LETTER_SUBSCRIPTION_TYPES.get(0));

        verify(notificationHandler).sendNotification(any(), eq(EMAIL_NOTIFICATION.getEmailTemplate()), any(), any());
        verifyExpectedErrorLogMessage(mockAppender, captorLoggingEvent, wrapper.getNewSscsCaseData().getCcdCaseId(), "Did not send a notification for event");
    }

    @Test
    public void doNotSendFallbackLetterNotificationToRepWhenNoLetterTemplate() {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, SubscriptionType.REPRESENTATIVE);

        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_WITH_ADDRESS);
        classUnderTest.sendEmailSmsLetterNotification(wrapper, EMPTY_TEMPLATE_NOTIFICATION, appellantEmptySubscription, FALLBACK_LETTER_SUBSCRIPTION_TYPES.get(0));

        verifyNoInteractions(notificationHandler);
        verifyExpectedErrorLogMessage(mockAppender, captorLoggingEvent, wrapper.getNewSscsCaseData().getCcdCaseId(), "Did not send a notification for event");
    }

    @Test
    public void givenDigitalCase_sendFallbackLetterNotificationToRep() {
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);
        when(notificationValidService.isFallbackLetterRequiredForSubscriptionType(any(), any(), any())).thenReturn(true);
        when(notificationHandler.sendNotification(any(), eq(LETTER_NOTIFICATION.getLetterTemplate()), any(), any())).thenReturn(true);

        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, SubscriptionType.REPRESENTATIVE);
        classUnderTest.sendEmailSmsLetterNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_WITH_ADDRESS), LETTER_NOTIFICATION, appellantEmptySubscription, FALLBACK_LETTER_SUBSCRIPTION_TYPES.get(0));

        verify(notificationHandler).sendNotification(any(), eq(LETTER_NOTIFICATION.getLetterTemplate()), any(), any());
        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    public void sendLetterNotificationForAppellant() throws NotificationClientException {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, SubscriptionType.APPELLANT);
        classUnderTest.sendLetterNotificationToAddress(buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, READY_TO_LIST.getId()), LETTER_NOTIFICATION, APPELLANT_WITH_ADDRESS.getAddress(), appellantEmptySubscription.getSubscriptionType());

        verify(notificationSender).sendLetter(eq(LETTER_NOTIFICATION.getLetterTemplate()), eq(APPELLANT_WITH_ADDRESS.getAddress()), any(), any(), any(), any());
        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    public void sendLetterNotificationForRep() throws NotificationClientException {
        SubscriptionWithType representativeEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, SubscriptionType.REPRESENTATIVE);
        classUnderTest.sendLetterNotificationToAddress(buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_WITH_ADDRESS), LETTER_NOTIFICATION, REP_WITH_ADDRESS.getAddress(), representativeEmptySubscription.getSubscriptionType());

        verify(notificationSender).sendLetter(eq(LETTER_NOTIFICATION.getLetterTemplate()), eq(REP_WITH_ADDRESS.getAddress()), any(), any(), any(), any());
        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    public void sendLetterNotificationForRepWithOrgName() throws NotificationClientException {
        SubscriptionWithType representativeEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, SubscriptionType.REPRESENTATIVE);
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_ORG_WITH_ADDRESS);
        classUnderTest.sendLetterNotificationToAddress(wrapper, LETTER_NOTIFICATION, REP_WITH_ADDRESS.getAddress(), representativeEmptySubscription.getSubscriptionType());

        verify(notificationSender).sendLetter(eq(LETTER_NOTIFICATION.getLetterTemplate()), eq(REP_WITH_ADDRESS.getAddress()), any(), any(), any(),any());
        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    public void doNotSendLetterNotificationIfAddressEmpty() throws NotificationClientException {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, SubscriptionType.APPELLANT);
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_EMPTY_ADDRESS, NotificationEventType.CASE_UPDATED, READY_TO_LIST.getId());
        classUnderTest.sendLetterNotification(wrapper, appellantEmptySubscription.getSubscription(), LETTER_NOTIFICATION, appellantEmptySubscription, NotificationEventType.CASE_UPDATED);

        verifyNoInteractions(notificationSender);
        verifyExpectedErrorLogMessage(mockAppender, captorLoggingEvent, wrapper.getNewSscsCaseData().getCcdCaseId(), "Failed to send letter for event id");
    }

    @Test
    public void doNotSendLetterNotificationIfNoAddress() {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, SubscriptionType.APPELLANT);
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_NO_ADDRESS, NotificationEventType.CASE_UPDATED, READY_TO_LIST.getId());
        classUnderTest.sendLetterNotification(wrapper, appellantEmptySubscription.getSubscription(), LETTER_NOTIFICATION, appellantEmptySubscription, NotificationEventType.CASE_UPDATED);

        verifyNoInteractions(notificationSender);
        verifyExpectedErrorLogMessage(mockAppender, captorLoggingEvent, wrapper.getNewSscsCaseData().getCcdCaseId(), "Failed to send letter for event id");
    }

    @Test
    public void logErrorMessageWhenNoNotificationSent() {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, SubscriptionType.APPELLANT);
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_NO_ADDRESS, NotificationEventType.CASE_UPDATED, READY_TO_LIST.getId());
        classUnderTest.sendEmailSmsLetterNotification(
            wrapper,
            LETTER_NOTIFICATION,
            appellantEmptySubscription,
            APPEAL_RECEIVED_NOTIFICATION
        );

        verifyExpectedErrorLogMessage(mockAppender, captorLoggingEvent, wrapper.getNewSscsCaseData().getCcdCaseId(), "Did not send a notification for event");
    }

    @Test
    public void getRepNameWhenRepHasName() {
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_WITH_ADDRESS);
        assertEquals(REP_WITH_ADDRESS.getName().getFullNameNoTitle(), getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep(), false));
    }

    @Test
    public void getRepOrganisationWhenRepHasOrgButNoName() {
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_ORG_WITH_ADDRESS);
        assertEquals(wrapper.getNewSscsCaseData().getAppeal().getRep().getOrganisation(), getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep(), false));
    }

    @Test
    public void getRepSalutationWhenRepHasOrgAndNoNameAndIngnoreOrgFlagSet() {
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_ORG_WITH_ADDRESS);
        assertEquals(REP_SALUTATION, getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep(), true));
    }

    @Test
    public void getRepOrganisationWhenRepHasOrgButNameSetToUndefined() {
        Representative repOrgWithAddressUndefinedName = Representative.builder()
            .organisation("Rep Org")
            .name(Name.builder().firstName("undefined").lastName("undefined").build())
            .address(Address.builder().line1("Rep Org Line 1").town("Rep Town").county("Rep County").postcode("RE9 3LL").build())
            .build();

        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_ORG_WITH_ADDRESS);
        assertEquals(repOrgWithAddressUndefinedName.getOrganisation(), getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep(), false));
    }

    @Test
    public void getRepSalutationWhenRepHasNoOrgAndNameSetToUndefined() {
        Representative repWithAddressAndUndefinedName = Representative.builder()
                .name(Name.builder().firstName("undefined").lastName("undefined").build())
                .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 3LL").build())
                .build();

        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, repWithAddressAndUndefinedName);
        assertEquals(REP_SALUTATION, getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep(), false));
    }

    @Test
    public void getRepSalutationWhenRepHasNoOrgAndNameSetToEmptyString() {
        Representative repWithAddressNoName = Representative.builder()
                .name(Name.builder().firstName("").lastName("").build())
                .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 3LL").build())
                .build();

        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, repWithAddressNoName);
        assertEquals(REP_SALUTATION, getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep(), false));
    }

    @Test
    public void getRepSalutationWhenOrgAndNameBothSetToEmptyString() {
        Representative repWithAddressNoName = Representative.builder()
                .organisation("")
                .name(Name.builder().firstName("").lastName("").build())
                .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 3LL").build())
                .build();

        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, repWithAddressNoName);
        assertEquals(REP_SALUTATION, getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep(), false));
    }

    @Test
    public void getRepNameWhenRepHasOrgAndName() {
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_ORG_WITH_NAME_AND_ADDRESS);
        assertEquals(REP_ORG_WITH_NAME_AND_ADDRESS.getName().getFullNameNoTitle(), getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep(), false));
    }

    @Test
    public void getRepNameWhenNameIsNotnull() {
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_WITH_ADDRESS);
        assertEquals(REP_WITH_ADDRESS.getName().getFullNameNoTitle(), getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep(), false));
    }

    @Test
    public void getRepOrganisationWhenNameNoFirstName() {
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_ORG_WITH_ADDRESS);
        assertEquals(wrapper.getNewSscsCaseData().getAppeal().getRep().getOrganisation(), getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep(), false));
    }

    @Test
    public void getRepNameWhenNameHasFirstNameLastNameAndOrg() {
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_ORG_WITH_NAME_AND_ADDRESS);
        assertEquals(REP_ORG_WITH_NAME_AND_ADDRESS.getName().getFullNameNoTitle(), getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep(), false));
    }

    @Test
    @Parameters(method = "bundledLetterTemplates")
    public void validBundledLetterType(NotificationEventType eventType) {
        assertNotNull(getBundledLetterDocumentUrl(eventType, buildBaseWrapper(APPELLANT_WITH_ADDRESS, eventType, READY_TO_LIST.getId()).getNewSscsCaseData()));
    }

    @Test
    @Parameters(method = "nonBundledLetterTemplates")
    public void invalidBundledLetterTileType(NotificationEventType eventType) {
        assertNull(getBundledLetterDocumentUrl(eventType, buildBaseWrapper(APPELLANT_WITH_ADDRESS, eventType, READY_TO_LIST.getId()).getNewSscsCaseData()));
    }

    @Test
    @Parameters({"APPEAL_RECEIVED_NOTIFICATION", "DIRECTION_ISSUED",  "DIRECTION_ISSUED_WELSH", "DECISION_ISSUED", "DECISION_ISSUED_WELSH", "ISSUE_FINAL_DECISION", "ISSUE_ADJOURNMENT_NOTICE", "DWP_UPLOAD_RESPONSE_NOTIFICATION"})
    public void sendLetterForNotificationType(NotificationEventType notificationEventType) {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, APPELLANT);
        when(pdfLetterService.generateLetter(any(), any(), any())).thenReturn("PDF".getBytes());
        classUnderTest.sendEmailSmsLetterNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, notificationEventType, VALID_APPEAL.getId()), DOCMOSIS_LETTER_NOTIFICATION, appellantEmptySubscription, NotificationEventType.APPEAL_RECEIVED_NOTIFICATION);
        verify(pdfLetterService).generateLetter(any(), any(), any());
        verify(pdfLetterService).buildCoversheet(any(), any());
        verifyNoMoreInteractions(pdfLetterService);
        verify(notificationHandler, atLeastOnce()).sendNotification(any(), any(), eq("Letter"), any());
    }

    @Test
    public void givenNonDigitalCase_willNotSendAppealLodgedLetters() {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, APPELLANT);
        classUnderTest.sendEmailSmsLetterNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.APPEAL_RECEIVED_NOTIFICATION, State.VALID_APPEAL.getId()), LETTER_NOTIFICATION, appellantEmptySubscription, NotificationEventType.APPEAL_RECEIVED_NOTIFICATION);
        verifyNoInteractions(notificationHandler);
    }

    private CcdNotificationWrapper buildBaseWrapper(Appellant appellant) {
        return buildBaseWrapper(appellant, STRUCK_OUT, null, Benefit.PIP, "Online", READY_TO_LIST.getId());
    }

    private CcdNotificationWrapper buildBaseWrapper(Appellant appellant, NotificationEventType eventType, String createdInGapsFrom) {
        return buildBaseWrapper(appellant, eventType, null, Benefit.PIP, "Online", createdInGapsFrom);
    }

    private CcdNotificationWrapper buildBaseWrapper(Appellant appellant, NotificationEventType eventType, Representative representative) {
        return buildBaseWrapper(appellant, eventType, representative, Benefit.PIP, "Online", READY_TO_LIST.getId());
    }

    private CcdNotificationWrapper buildBaseWrapper(Appellant appellant, NotificationEventType eventType, Representative representative, Benefit benefit, String receivedVia, String createdInGapsFrom) {
        Subscription repSubscription = null;
        if (null != representative) {
            repSubscription = Subscription.builder().email("test@test.com").subscribeEmail(YES).mobile("07800000000").subscribeSms(YES).build();
        }

        Subscription appellantSubscription = null;
        if (null != appellant) {
            appellantSubscription = Subscription.builder().tya("GLSCRR").email("Email").mobile("07983495065").subscribeEmail(YES).subscribeSms(YES).build();
        }

        List<SscsDocument> documents = new ArrayList<>();

        documents.add(SscsDocument.builder().value(
                SscsDocumentDetails.builder().documentType(DocumentType.DIRECTION_NOTICE.getValue())
                        .documentLink(DocumentLink.builder().documentUrl("testUrl").build())
                        .documentDateAdded(LocalDate.now().minusDays(1).toString())
                    .build())
                .build());

        documents.add(SscsDocument.builder().value(
                SscsDocumentDetails.builder().documentType(DocumentType.DECISION_NOTICE.getValue())
                        .documentLink(DocumentLink.builder().documentUrl("testUrl2").build())
                        .documentDateAdded(LocalDate.now().minusDays(1).toString())
                        .build())
                .build());

        documents.add(SscsDocument.builder().value(
                SscsDocumentDetails.builder().documentType(DocumentType.FINAL_DECISION_NOTICE.getValue())
                        .documentLink(DocumentLink.builder().documentUrl("testUrl3").build())
                        .documentDateAdded(LocalDate.now().minusDays(1).toString())
                        .build())
                .build());

        documents.add(SscsDocument.builder().value(
                SscsDocumentDetails.builder().documentType(DocumentType.ADJOURNMENT_NOTICE.getValue())
                        .documentLink(DocumentLink.builder().documentUrl("testUrl4").build())
                        .documentDateAdded(LocalDate.now().minusDays(1).toString())
                        .build())
                .build());

        List<SscsWelshDocument> welshDocuments = new ArrayList<>();

        welshDocuments.add(SscsWelshDocument.builder().value(
                SscsWelshDocumentDetails.builder().documentType(DocumentType.DIRECTION_NOTICE.getValue())
                        .documentLink(DocumentLink.builder().documentUrl("testUrl").build())
                        .documentDateAdded(LocalDate.now().minusDays(1).toString())
                        .build())
                .build());

        welshDocuments.add(SscsWelshDocument.builder().value(
                SscsWelshDocumentDetails.builder().documentType(DocumentType.DECISION_NOTICE.getValue())
                        .documentLink(DocumentLink.builder().documentUrl("testUrl2").build())
                        .documentDateAdded(LocalDate.now().minusDays(1).toString())
                        .build())
                .build());

        welshDocuments.add(SscsWelshDocument.builder().value(
                SscsWelshDocumentDetails.builder().documentType(DocumentType.FINAL_DECISION_NOTICE.getValue())
                        .documentLink(DocumentLink.builder().documentUrl("testUrl3").build())
                        .documentDateAdded(LocalDate.now().minusDays(1).toString())
                        .build())
                .build());

        welshDocuments.add(SscsWelshDocument.builder().value(
                SscsWelshDocumentDetails.builder().documentType(DocumentType.ADJOURNMENT_NOTICE.getValue())
                        .documentLink(DocumentLink.builder().documentUrl("testUrl4").build())
                        .documentDateAdded(LocalDate.now().minusDays(1).toString())
                        .build())
                .build());

        SscsCaseData sscsCaseDataWithDocuments = SscsCaseData.builder()
            .appeal(
                Appeal
                    .builder()
                    .benefitType(BenefitType.builder().code(benefit.name()).description(benefit.getDescription()).build())
                    .hearingType(AppealHearingType.ORAL.name())
                    .hearingOptions(HearingOptions.builder().wantsToAttend(YES).build())
                    .appellant(appellant)
                    .rep(representative)
                    .receivedVia(receivedVia)
                    .build())
            .subscriptions(
                Subscriptions.builder()
                    .appellantSubscription(appellantSubscription)
                    .representativeSubscription(repSubscription)
                    .build())
            .createdInGapsFrom(createdInGapsFrom)
            .sscsDocument(documents)
            .sscsWelshDocuments(welshDocuments)
            .caseReference(CASE_REFERENCE)
            .ccdCaseId(CASE_ID)
            .sscsInterlocDecisionDocument(SscsInterlocDecisionDocument.builder().documentLink(DocumentLink.builder().documentUrl("testUrl").build()).build())
            .sscsStrikeOutDocument(SscsStrikeOutDocument.builder().documentLink(DocumentLink.builder().documentUrl("testUrl").build()).build())
            .build();

        SscsCaseDataWrapper struckOutSscsCaseDataWrapper = SscsCaseDataWrapper.builder()
                .newSscsCaseData(sscsCaseDataWithDocuments)
                .oldSscsCaseData(sscsCaseDataWithDocuments)
                .notificationEventType(eventType)
                .build();
        return new CcdNotificationWrapper(struckOutSscsCaseDataWrapper);
    }

    public Object[] bundledLetterTemplates() {
        return BUNDLED_LETTER_EVENT_TYPES.toArray();
    }

    public Object[] nonBundledLetterTemplates() {
        Object[] originalValues = Arrays.stream(NotificationEventType.values())
            .filter(type -> !BUNDLED_LETTER_EVENT_TYPES.contains(type))
            .toArray();

        ArrayList<Object> x = new ArrayList<Object>(Arrays.asList(originalValues));
        x.add(null);

        return x.toArray();
    }
}
