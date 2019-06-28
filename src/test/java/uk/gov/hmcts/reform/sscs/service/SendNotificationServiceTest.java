package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.REP_SALUTATION;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPOINTEE;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.service.LetterUtils.getAddressToUseForLetter;
import static uk.gov.hmcts.reform.sscs.service.NotificationValidService.BUNDLED_LETTER_EVENT_TYPES;
import static uk.gov.hmcts.reform.sscs.service.NotificationValidService.FALLBACK_LETTER_SUBSCRIPTION_TYPES;
import static uk.gov.hmcts.reform.sscs.service.SendNotificationService.getBundledLetterDocumentUrl;
import static uk.gov.hmcts.reform.sscs.service.SendNotificationService.getRepSalutation;

import java.util.*;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
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
        .address(Address.builder().build())
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

    @Mock
    private NotificationValidService notificationValidService;

    @Mock
    private BundledLetterTemplateUtil bundledLetterTemplateUtil;

    private SendNotificationService classUnderTest;

    @Before
    public void setup() {
        initMocks(this);

        classUnderTest = new SendNotificationService(notificationSender, evidenceManagementService, pdfService, notificationHandler, notificationValidService, bundledLetterTemplateUtil);
        classUnderTest.bundledLettersOn = true;
        classUnderTest.lettersOn = true;
        classUnderTest.interlocLettersOn = true;
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
        classUnderTest.sendEmailSmsLetterNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED), SMS_NOTIFICATION, appellantSmsSubscription, FALLBACK_LETTER_SUBSCRIPTION_TYPES.get(0));

        verify(notificationHandler).sendNotification(any(), eq(SMS_NOTIFICATION.getSmsTemplate()), any(), any());
    }

    @Test
    public void doNotSendFallbackLetterNotificationToAppellantWhenSubscribedForEmail() {
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        SubscriptionWithType appellantEmailSubscription = new SubscriptionWithType(EMAIL_SUBSCRIPTION, APPELLANT);
        classUnderTest.sendEmailSmsLetterNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED), EMAIL_NOTIFICATION, appellantEmailSubscription, FALLBACK_LETTER_SUBSCRIPTION_TYPES.get(0));

        verify(notificationHandler).sendNotification(any(), eq(EMAIL_NOTIFICATION.getEmailTemplate()), any(), any());
    }

    @Test
    public void doNotSendFallbackLetterNotificationToAppellantWhenNoLetterTemplate() {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, APPELLANT);

        classUnderTest.sendEmailSmsLetterNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED), EMPTY_TEMPLATE_NOTIFICATION, appellantEmptySubscription, FALLBACK_LETTER_SUBSCRIPTION_TYPES.get(0));

        verifyZeroInteractions(notificationHandler);
    }

    @Test
    public void sendFallbackLetterNotificationToAppellant() {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, APPELLANT);

        when(notificationValidService.isFallbackLetterRequiredForSubscriptionType(any(), any(), any())).thenReturn(true);

        classUnderTest.sendEmailSmsLetterNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED), LETTER_NOTIFICATION, appellantEmptySubscription, FALLBACK_LETTER_SUBSCRIPTION_TYPES.get(0));

        verify(notificationHandler).sendNotification(any(), eq(LETTER_NOTIFICATION.getLetterTemplate()), any(), any());
    }

    @Test
    @Parameters(method = "getMandatoryLettersEventTypes")
    public void doNotSendMandatoryLetterNotificationToAppellantWhenToggledOff(NotificationEventType eventType) {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, APPELLANT);

        classUnderTest.lettersOn = false;
        classUnderTest.interlocLettersOn = false;
        classUnderTest.sendEmailSmsLetterNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, eventType), LETTER_NOTIFICATION, appellantEmptySubscription, eventType);

        verifyZeroInteractions(notificationHandler);
    }

    private Object[] getMandatoryLettersEventTypes() {
        return new Object[] {
            APPEAL_WITHDRAWN_NOTIFICATION,
            STRUCK_OUT,
            HEARING_BOOKED_NOTIFICATION,
            DIRECTION_ISSUED,
            REQUEST_INFO_INCOMPLETE
        };
    }

    @Test
    @Parameters(method = "getInterlocLettersEventTypes")
    public void doNotSendInterlocLetterNotificationToAppellantWhenToggledOff(NotificationEventType eventType) {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, APPELLANT);

        classUnderTest.interlocLettersOn = false;
        classUnderTest.sendEmailSmsLetterNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, eventType), LETTER_NOTIFICATION, appellantEmptySubscription, eventType);

        verifyZeroInteractions(notificationHandler);
    }

    private Object[] getInterlocLettersEventTypes() {
        return new Object[] {
            STRUCK_OUT,
            DIRECTION_ISSUED,
            REQUEST_INFO_INCOMPLETE,
            JUDGE_DECISION_APPEAL_TO_PROCEED,
            TCW_DECISION_APPEAL_TO_PROCEED,
            NON_COMPLIANT_NOTIFICATION
        };
    }

    @Test
    public void doNotSendFallbackLetterNotificationToRepWhenSubscribedForSms() {
        SubscriptionWithType appellantSmsSubscription = new SubscriptionWithType(SMS_SUBSCRIPTION, SubscriptionType.REPRESENTATIVE);

        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        classUnderTest.sendEmailSmsLetterNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_WITH_ADDRESS), SMS_NOTIFICATION, appellantSmsSubscription, FALLBACK_LETTER_SUBSCRIPTION_TYPES.get(0));

        verify(notificationHandler).sendNotification(any(), eq(SMS_NOTIFICATION.getSmsTemplate()), any(), any());
    }

    @Test
    public void doNotSendFallbackLetterNotificationToRepWhenSubscribedForEmail() {
        SubscriptionWithType appellantEmailSubscription = new SubscriptionWithType(EMAIL_SUBSCRIPTION, SubscriptionType.REPRESENTATIVE);

        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        classUnderTest.sendEmailSmsLetterNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_WITH_ADDRESS), EMAIL_NOTIFICATION, appellantEmailSubscription, FALLBACK_LETTER_SUBSCRIPTION_TYPES.get(0));

        verify(notificationHandler).sendNotification(any(), eq(EMAIL_NOTIFICATION.getEmailTemplate()), any(), any());
    }

    @Test
    public void doNotSendFallbackLetterNotificationToRepWhenNoLetterTemplate() {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, SubscriptionType.REPRESENTATIVE);

        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        classUnderTest.sendEmailSmsLetterNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_WITH_ADDRESS), EMPTY_TEMPLATE_NOTIFICATION, appellantEmptySubscription, FALLBACK_LETTER_SUBSCRIPTION_TYPES.get(0));

        verifyZeroInteractions(notificationHandler);
    }

    @Test
    public void sendFallbackLetterNotificationToRep() {
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);
        when(notificationValidService.isFallbackLetterRequiredForSubscriptionType(any(), any(), any())).thenReturn(true);

        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, SubscriptionType.REPRESENTATIVE);
        classUnderTest.sendEmailSmsLetterNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_WITH_ADDRESS), LETTER_NOTIFICATION, appellantEmptySubscription, FALLBACK_LETTER_SUBSCRIPTION_TYPES.get(0));

        verify(notificationHandler).sendNotification(any(), eq(LETTER_NOTIFICATION.getLetterTemplate()), any(), any());
    }

    @Test
    public void sendLetterNotificationForAppellant() throws NotificationClientException {
        classUnderTest.sendLetterNotificationToAddress(buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED), LETTER_NOTIFICATION, APPELLANT_WITH_ADDRESS.getAddress());

        verify(notificationSender).sendLetter(eq(LETTER_NOTIFICATION.getLetterTemplate()), eq(APPELLANT_WITH_ADDRESS.getAddress()), any(), any());
    }

    @Test
    public void sendLetterNotificationForRep() throws NotificationClientException {
        classUnderTest.sendLetterNotificationToAddress(buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED), LETTER_NOTIFICATION, REP_WITH_ADDRESS.getAddress());

        verify(notificationSender).sendLetter(eq(LETTER_NOTIFICATION.getLetterTemplate()), eq(REP_WITH_ADDRESS.getAddress()), any(), any());
    }

    @Test
    public void doNotSendLetterNotificationIfAddressEmpty() throws NotificationClientException {
        classUnderTest.sendLetterNotificationToAddress(buildBaseWrapper(APPELLANT_WITH_EMPTY_ADDRESS, NotificationEventType.CASE_UPDATED), LETTER_NOTIFICATION, APPELLANT_WITH_EMPTY_ADDRESS.getAddress());

        verifyZeroInteractions(notificationSender);
    }

    @Test
    public void doNotSendLetterNotificationIfNoAddress() throws NotificationClientException {
        classUnderTest.sendLetterNotificationToAddress(buildBaseWrapper(APPELLANT_WITH_NO_ADDRESS, NotificationEventType.CASE_UPDATED), LETTER_NOTIFICATION, APPELLANT_WITH_NO_ADDRESS.getAddress());

        verifyZeroInteractions(notificationSender);
    }

    @Test
    public void getRepSalutationWhenRepHasName() {
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_WITH_ADDRESS);
        assertEquals(REP_WITH_ADDRESS.getName().getFullNameNoTitle(), getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep()));
    }

    @Test
    public void getRepSalutationWhenRepHasOrgButNoName() {
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_ORG_WITH_ADDRESS);
        assertEquals(REP_SALUTATION, getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep()));
    }

    @Test
    public void getRepSalutationWhenRepHasOrgAndName() {
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_ORG_WITH_NAME_AND_ADDRESS);
        assertEquals(REP_ORG_WITH_NAME_AND_ADDRESS.getName().getFullNameNoTitle(), getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep()));
    }

    @Test
    @Parameters(method = "bundledLetterTemplates")
    public void validBundledLetterType(NotificationEventType eventType) {
        assertNotNull(getBundledLetterDocumentUrl(eventType, buildBaseWrapper(APPELLANT_WITH_ADDRESS, eventType).getNewSscsCaseData()));
    }

    @Test
    @Parameters(method = "nonBundledLetterTemplates")
    public void invalidBundledLetterTileType(NotificationEventType eventType) {
        assertNull(getBundledLetterDocumentUrl(eventType, buildBaseWrapper(APPELLANT_WITH_ADDRESS, eventType).getNewSscsCaseData()));
    }

    private CcdNotificationWrapper buildBaseWrapper(Appellant appellant) {
        return buildBaseWrapper(appellant, STRUCK_OUT, null);
    }

    private CcdNotificationWrapper buildBaseWrapper(Appellant appellant, NotificationEventType eventType) {
        return buildBaseWrapper(appellant, eventType, null);
    }

    private CcdNotificationWrapper buildBaseWrapper(Appellant appellant, NotificationEventType eventType, Representative representative) {
        Subscription repSubscription = null;
        if (null != representative) {
            repSubscription = Subscription.builder().email("test@test.com").subscribeEmail(YES).mobile("07800000000").subscribeSms(YES).build();
        }

        Subscription appellantSubscription = null;
        if (null != appellant) {
            appellantSubscription = Subscription.builder().tya("GLSCRR").email("Email").mobile("07983495065").subscribeEmail(YES).subscribeSms(YES).build();
        }

        SscsCaseData sscsCaseDataWithDocuments = SscsCaseData.builder()
            .appeal(
                Appeal
                    .builder()
                    .hearingType(AppealHearingType.ORAL.name())
                    .hearingOptions(HearingOptions.builder().wantsToAttend(YES).build())
                    .appellant(appellant)
                    .rep(representative)
                    .build())
            .subscriptions(
                Subscriptions.builder()
                    .appellantSubscription(appellantSubscription)
                    .representativeSubscription(repSubscription)
                    .build())
            .caseReference(CASE_REFERENCE)
            .ccdCaseId(CASE_ID)
            .sscsInterlocDecisionDocument(SscsInterlocDecisionDocument.builder().documentLink(DocumentLink.builder().documentUrl("testUrl").build()).build())
            .sscsInterlocDirectionDocument(SscsInterlocDirectionDocument.builder().documentLink(DocumentLink.builder().documentUrl("testUrl").build()).build())
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
