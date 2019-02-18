package uk.gov.hmcts.reform.sscs.service;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.factory.NotificationFactory;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.service.notify.NotificationClientException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class NotificationServiceForSubscriptionUpdatedTest {

    private static final String DATE = "2018-01-01T14:01:18.243";
    private static final String APPEAL_NUMBER = "GLSCRR";
    private static final String YES = "Yes";
    private static final String NO = "No";
    private static final String CASE_REFERENCE = "ABC123";
    private static final String CASE_ID = "1000001";
    private static final String EMAIL_TEST_1 = "test1@email.com";
    private static final String EMAIL_TEST_2 = "test2@email.com";
    private static final String MOBILE_NUMBER_1 = "07983495065";
    private static final String MOBILE_NUMBER_2 = "07123456789";

    private NotificationService notificationService;

    @Value("${notification.appealReceived.appellant.emailId}")
    private String appealReceivedAppellantEmailId;

    @Value("${notification.appealReceived.appellant.smsId}")
    private String appealReceivedAppellantSmsId;

    @Value("${notification.subscriptionUpdated.emailId}")
    private String subscriptionUpdatedEmailId;

    @Value("${notification.subscriptionUpdated.smsId}")
    private String subscriptionUpdatedSmsId;

    @Value("${notification.subscriptionOld.emailId}")
    private String subscriptionOldEmailId;

    @Value("${notification.subscriptionOld.smsId}")
    private String subscriptionOldSmsId;

    @Autowired
    private NotificationValidService notificationValidService;

    @Autowired
    private NotificationFactory notificationFactory;

    @Autowired
    private NotificationConfig notificationConfig;

    @Autowired
    private NotificationHandler notificationHandler;

    @Mock
    private NotificationSender notificationSender;

    @Mock
    private ReminderService reminderService;

    @Mock
    private OutOfHoursCalculator outOfHoursCalculator;

    @Mock
    private EvidenceManagementService evidenceManagementService;

    @Mock
    private SscsGeneratePdfService sscsGeneratePdfService;

    @Mock
    private IdamService idamService;

    private final Subscription subscription = Subscription.builder().tya(APPEAL_NUMBER).email(EMAIL_TEST_1)
                .mobile(MOBILE_NUMBER_1).subscribeEmail(YES).subscribeSms(YES).build();

    @Before
    public void setup() {
        initMocks(this);
        notificationService = getNotificationService();

        when(outOfHoursCalculator.isItOutOfHours()).thenReturn(false);

        String authHeader = "authHeader";
        String serviceAuthHeader = "serviceAuthHeader";
        IdamTokens idamTokens = IdamTokens.builder().idamOauth2Token(authHeader).serviceAuthorization(serviceAuthHeader).build();

        when(idamService.getIdamTokens()).thenReturn(idamTokens);
    }

    private NotificationService getNotificationService() {
        SendNotificationService sendNotificationService = new SendNotificationService(notificationSender, evidenceManagementService, sscsGeneratePdfService, notificationHandler);
        ReflectionTestUtils.setField(sendNotificationService, "noncompliantcaseletterTemplate", "/templates/non_compliant_case_letter_template.html");
        ReflectionTestUtils.setField(sendNotificationService, "bundledLettersOn", false);
        return new NotificationService(notificationFactory, reminderService,
                notificationValidService, notificationHandler, outOfHoursCalculator, notificationConfig, sendNotificationService
        );
    }

    @Test
    public void unsubscribeFromSmsAndEmail_doesNotSendAnyEmailsOrSms() {
        Subscription newSubscription = subscription.toBuilder().subscribeEmail(NO).subscribeSms(NO).build();
        unsubscribeAssertions(newSubscription);
    }

    @Test
    public void unsubscribeFromEmail_doesNotSendAnyEmailsOrSms() {
        Subscription newSubscription = subscription.toBuilder().subscribeEmail(NO).subscribeSms(YES).build();
        unsubscribeAssertions(newSubscription);
    }

    @Test
    public void unsubscribeFromSms_doesNotSendAnyEmailsOrSms() {
        Subscription newSubscription = subscription.toBuilder().subscribeEmail(YES).subscribeSms(NO).build();
        unsubscribeAssertions(newSubscription);
    }

    private void unsubscribeAssertions(Subscription newSubscription) {
        SscsCaseData newSscsCaseData = getSscsCaseData(newSubscription);
        SscsCaseData oldSscsCaseData = getSscsCaseData(subscription);

        SscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData);

        notificationService.manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));

        verifyNoMoreInteractions(notificationSender);
    }

    @Test
    public void subscribeEmail_willSendSubscriptionEmail_and_ResendLastEvent() throws NotificationClientException {
        Subscription newSubscription = subscription.toBuilder().email(EMAIL_TEST_2).subscribeEmail(YES).subscribeSms(NO).build();
        Subscription oldSubscription = subscription.toBuilder().subscribeEmail(NO).subscribeSms(NO).build();
        SscsCaseData newSscsCaseData = getSscsCaseData(newSubscription);
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription);
        SscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData);

        notificationService.manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));
        verify(notificationSender).sendEmail(eq(subscriptionUpdatedEmailId), eq(newSubscription.getEmail()), any(), any(), any());
        verify(notificationSender).sendEmail(eq(appealReceivedAppellantEmailId), eq(newSubscription.getEmail()), any(), any(), any());
        verifyNoMoreInteractions(notificationSender);
    }

    @Test
    public void subscribeMobile_willSendSubscriptionSms_and_ResendLastEvent() throws NotificationClientException {
        Subscription newSubscription = subscription.toBuilder().mobile(MOBILE_NUMBER_2).subscribeEmail(NO).subscribeSms(YES).build();
        Subscription oldSubscription = subscription.toBuilder().subscribeEmail(NO).subscribeSms(NO).build();
        SscsCaseData newSscsCaseData = getSscsCaseData(newSubscription);
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription);
        SscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData);

        notificationService.manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));
        verify(notificationSender).sendSms(eq(subscriptionUpdatedSmsId), eq(newSubscription.getMobile()), any(), any(), any(), any());
        verify(notificationSender).sendSms(eq(appealReceivedAppellantSmsId), eq(newSubscription.getMobile()), any(), any(), any(), any());
        verifyNoMoreInteractions(notificationSender);
    }

    @Test
    public void subscribeMobileAndEmail_willSendSubscriptionEmailAndSms_and_ResendLastEvent() throws NotificationClientException {
        Subscription newSubscription = subscription.toBuilder().email(EMAIL_TEST_2).mobile(MOBILE_NUMBER_2).subscribeEmail(YES).subscribeSms(YES).build();
        Subscription oldSubscription = subscription.toBuilder().subscribeEmail(NO).subscribeSms(NO).build();
        SscsCaseData newSscsCaseData = getSscsCaseData(newSubscription);
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription);
        SscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData);

        notificationService.manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));
        verify(notificationSender).sendEmail(eq(subscriptionUpdatedEmailId), eq(newSubscription.getEmail()), any(), any(), any());
        verify(notificationSender).sendEmail(eq(appealReceivedAppellantEmailId), eq(newSubscription.getEmail()), any(), any(), any());
        verify(notificationSender).sendSms(eq(subscriptionUpdatedSmsId), eq(newSubscription.getMobile()), any(), any(), any(), any());
        verify(notificationSender).sendSms(eq(appealReceivedAppellantSmsId), eq(newSubscription.getMobile()), any(), any(), any(), any());

        verifyNoMoreInteractions(notificationSender);
    }

    @Test
    public void changeEmail_willSendChangeEmailToOldAndNewEmail() throws NotificationClientException {
        SscsCaseData newSscsCaseData = getSscsCaseData(subscription);

        Subscription oldSubscription = subscription.toBuilder().email(EMAIL_TEST_2).build();
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription);

        SscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData);

        notificationService.manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));

        verify(notificationSender).sendEmail(eq(subscriptionUpdatedEmailId), eq(subscription.getEmail()), any(), any(), any());
        verify(notificationSender).sendEmail(eq(subscriptionOldEmailId), eq(oldSubscription.getEmail()), any(), any(), any());

        verifyNoMoreInteractions(notificationSender);
    }

    @Test
    public void changeMobile_willSendChangeSmsToOldAndNewMobile() throws NotificationClientException {
        SscsCaseData newSscsCaseData = getSscsCaseData(subscription);

        Subscription oldSubscription = subscription.toBuilder().mobile(MOBILE_NUMBER_2).build();
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription);

        SscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData);

        notificationService.manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));

        verify(notificationSender).sendSms(eq(subscriptionUpdatedSmsId), eq(subscription.getMobile()), any(), any(), any(), any());
        verify(notificationSender).sendSms(eq(subscriptionOldSmsId), eq(oldSubscription.getMobile()), any(), any(), any(), any());

        verifyNoMoreInteractions(notificationSender);
    }

    @Test
    public void changeMobileAndEmail_willSendChangeSmsToOldAndNewMobileAndEmail() throws NotificationClientException {
        SscsCaseData newSscsCaseData = getSscsCaseData(subscription);

        Subscription oldSubscription = subscription.toBuilder().mobile(MOBILE_NUMBER_2).email(EMAIL_TEST_2).build();
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription);

        SscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData);

        notificationService.manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));

        verify(notificationSender).sendEmail(eq(subscriptionUpdatedEmailId), eq(subscription.getEmail()), any(), any(), any());
        verify(notificationSender).sendEmail(eq(subscriptionOldEmailId), eq(oldSubscription.getEmail()), any(), any(), any());
        verify(notificationSender).sendSms(eq(subscriptionUpdatedSmsId), eq(subscription.getMobile()), any(), any(), any(), any());
        verify(notificationSender).sendSms(eq(subscriptionOldSmsId), eq(oldSubscription.getMobile()), any(), any(), any(), any());

        verifyNoMoreInteractions(notificationSender);
    }


    private SscsCaseDataWrapper getSscsCaseDataWrapper(SscsCaseData newSscsCaseData, SscsCaseData oldSscsCaseData) {
        return SscsCaseDataWrapper.builder()
                .newSscsCaseData(newSscsCaseData)
                .oldSscsCaseData(oldSscsCaseData)
                .notificationEventType(SUBSCRIPTION_UPDATED_NOTIFICATION).build();
    }

    private SscsCaseData getSscsCaseData(Subscription subscription) {

        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(APPEAL_RECEIVED.getCcdType()).build()).build());

        return SscsCaseData.builder().ccdCaseId(CASE_ID).events(events)
                .appeal(Appeal.builder()
                        .mrnDetails(MrnDetails.builder().mrnDate(DATE).dwpIssuingOffice("office").build())
                        .appealReasons(AppealReasons.builder().build())
                        .appellant(Appellant.builder()
                                .name(Name.builder().firstName("firstName").lastName("lastName").build())
                                .address(Address.builder().line1("122 Breach Street").line2("The Village").town("My town").county("Cardiff").postcode("CF11 2HB").build())
                                .contact(Contact.builder().email(EMAIL_TEST_1).phone(MOBILE_NUMBER_1).build())
                                .identity(Identity.builder().nino("NP 27 28 67 B").dob("12 March 1971").build()).build())
                        .hearingType(AppealHearingType.ORAL.name())
                        .benefitType(BenefitType.builder().code(Benefit.PIP.name()).build())
                        .hearingOptions(HearingOptions.builder()
                                .wantsToAttend(YES)
                                .build())
                        .build())
                .subscriptions(Subscriptions.builder().appellantSubscription(subscription).build()).caseReference(CASE_REFERENCE).build();
    }
}
