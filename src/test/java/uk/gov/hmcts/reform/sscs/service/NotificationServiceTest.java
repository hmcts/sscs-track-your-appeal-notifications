package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.*;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.service.SendNotificationService.DM_STORE_USER_ID;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.pdfbox.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.Destination;
import uk.gov.hmcts.reform.sscs.domain.notify.Notification;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.domain.notify.Reference;
import uk.gov.hmcts.reform.sscs.domain.notify.Template;
import uk.gov.hmcts.reform.sscs.exception.NotificationServiceException;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.factory.CohNotificationWrapper;
import uk.gov.hmcts.reform.sscs.factory.NotificationFactory;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(JUnitParamsRunner.class)
public class NotificationServiceTest {

    static Appellant APPELLANT_WITH_ADDRESS = Appellant.builder()
            .name(Name.builder().firstName("Ap").lastName("pellant").build())
            .address(Address.builder().line1("Appellant Line 1").town("Appellant Town").county("Appellant County").postcode("AP9 3LL").build())
            .build();

    private static final String APPEAL_NUMBER = "GLSCRR";
    private static final String YES = "Yes";
    private static final String CASE_REFERENCE = "ABC123";
    private static final String CASE_ID = "1000001";
    private static final String EMAIL_TEMPLATE_ID = "email-template-id";
    private static final String SMS_TEMPLATE_ID = "sms-template-id";
    private static final String LETTER_TEMPLATE_ID_STRUCKOUT = "struckOut";
    private static final String SAME_TEST_EMAIL_COM = "sametest@email.com";
    private static final String NEW_TEST_EMAIL_COM = "newtest@email.com";
    private static final String PIP = "PIP";
    private static final String EMAIL = "Email";
    private static final String SMS = "SMS";
    private static final String SMS_MOBILE = "07123456789";
    private static final String LETTER = "Letter";
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

    @Mock
    private EvidenceManagementService evidenceManagementService;

    @Mock
    private SscsGeneratePdfService sscsGeneratePdfService;

    @Mock
    private IdamService idamService;

    private SscsCaseData sscsCaseData;
    private CcdNotificationWrapper ccdNotificationWrapper;
    private SscsCaseDataWrapper sscsCaseDataWrapper;

    @Before
    public void setup() {
        initMocks(this);

        notificationService = getNotificationService(true, true);

        sscsCaseData = SscsCaseData.builder()
            .appeal(
                Appeal.builder()
                    .hearingType(AppealHearingType.ORAL.name())
                    .hearingOptions(HearingOptions.builder().wantsToAttend(YES).build())
                    .appellant(APPELLANT_WITH_ADDRESS)
                    .build()
            )
            .subscriptions(Subscriptions.builder().appellantSubscription(Subscription.builder()
                .tya(APPEAL_NUMBER)
                .email(EMAIL)
                .mobile(MOBILE_NUMBER_1)
                .subscribeEmail(YES)
                .subscribeSms(YES)
                .build()).build())
            .caseReference(CASE_REFERENCE)
            .build();
        sscsCaseDataWrapper = SscsCaseDataWrapper.builder().newSscsCaseData(sscsCaseData).oldSscsCaseData(sscsCaseData).notificationEventType(APPEAL_WITHDRAWN_NOTIFICATION).build();
        ccdNotificationWrapper = new CcdNotificationWrapper(sscsCaseDataWrapper);
        when(outOfHoursCalculator.isItOutOfHours()).thenReturn(false);

        String authHeader = "authHeader";
        String serviceAuthHeader = "serviceAuthHeader";
        IdamTokens idamTokens = IdamTokens.builder().idamOauth2Token(authHeader).serviceAuthorization(serviceAuthHeader).build();

        when(idamService.getIdamTokens()).thenReturn(idamTokens);
    }

    @Test
    @Parameters(method = "generateNotificationTypeAndSubscriptionsScenarios")
    public void givenNotificationEventTypeAndDifferentSubscriptionCombinations_shouldManageNotificationAndSubscriptionAccordingly(
            NotificationEventType notificationEventType, int wantedNumberOfEmailNotificationsSent,
            int wantedNumberOfSmsNotificationsSent, Subscription appellantSubscription, Subscription repsSubscription,
            Subscription appointeeSubscription, SubscriptionType[] expectedSubscriptionTypes) {

        ccdNotificationWrapper = buildNotificationWrapperGivenNotificationTypeAndSubscriptions(
                notificationEventType, appellantSubscription, repsSubscription, appointeeSubscription);

        given(notificationValidService.isHearingTypeValidToSendNotification(
                any(SscsCaseData.class), eq(notificationEventType))).willReturn(true);

        given(notificationValidService.isNotificationStillValidToSend(anyList(), eq(notificationEventType)))
                .willReturn(true);

        given(notificationValidService.isFallbackLetterRequiredForSubscriptionType(any(), any(), any())).willReturn(true);

        given(factory.create(any(NotificationWrapper.class), any(SubscriptionType.class)))
                .willReturn(new Notification(
                        Template.builder()
                                .emailTemplateId(EMAIL_TEMPLATE_ID)
                                .smsTemplateId(SMS_TEMPLATE_ID)
                                .build(),
                        Destination.builder()
                                .email(EMAIL)
                                .sms(SMS_MOBILE)
                                .build(),
                        null,
                        new Reference(),
                        null));

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        ArgumentCaptor<SubscriptionType> subscriptionTypeCaptor = ArgumentCaptor.forClass(SubscriptionType.class);
        then(factory).should(times(expectedSubscriptionTypes.length))
                .create(any(NotificationWrapper.class), subscriptionTypeCaptor.capture());
        assertArrayEquals(expectedSubscriptionTypes, subscriptionTypeCaptor.getAllValues().toArray());

        then(notificationHandler).should(times(wantedNumberOfEmailNotificationsSent)).sendNotification(
                eq(ccdNotificationWrapper), eq(EMAIL_TEMPLATE_ID), eq("Email"),
                any(NotificationHandler.SendNotification.class));
        then(notificationHandler).should(times(wantedNumberOfSmsNotificationsSent)).sendNotification(
                eq(ccdNotificationWrapper), eq(SMS_TEMPLATE_ID), eq("SMS"),
                any(NotificationHandler.SendNotification.class));

    }

    @Test
    @Parameters(method = "generateNotificationTypeAndSubscriptionsWhenOldCaseReferenceScenarios")
    public void givenNotificationEventTypeAndDifferentSubscriptionCombinationsWhenOldCaseReference_shouldManageNotificationAndSubscriptionAccordingly(
        NotificationEventType notificationEventType, int wantedNumberOfEmailNotificationsSent,
        int wantedNumberOfSmsNotificationsSent, Subscription appellantSubscription, Subscription repsSubscription,
        Subscription appointeeSubscription, SubscriptionType[] expectedSubscriptionTypes) {

        SscsCaseData oldCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().appellant(Appellant.builder().build())
                .hearingType(AppealHearingType.ORAL.name())
                .hearingOptions(HearingOptions.builder()
                    .wantsToAttend(YES)
                    .build())
                .build())
            .subscriptions(Subscriptions.builder()
                .appellantSubscription(Subscription.builder().build())
                .appointeeSubscription(appointeeSubscription)
                .representativeSubscription(repsSubscription)
                .build())
            .caseReference(CASE_REFERENCE)
            .hearings(Collections.singletonList(Hearing.builder().build()))
            .build();

        ccdNotificationWrapper = buildNotificationWrapperGivenNotificationTypeAndSubscriptions(
            notificationEventType, appellantSubscription, repsSubscription, appointeeSubscription, oldCaseData);

        given(notificationValidService.isHearingTypeValidToSendNotification(
            any(SscsCaseData.class), eq(notificationEventType))).willReturn(true);

        given(notificationValidService.isNotificationStillValidToSend(anyList(), eq(notificationEventType)))
            .willReturn(true);

        if (0 != expectedSubscriptionTypes.length) {
            given(notificationValidService.isFallbackLetterRequiredForSubscriptionType(any(), any(), any())).willReturn(true);
        }

        given(factory.create(any(NotificationWrapper.class), any(SubscriptionType.class)))
            .willReturn(new Notification(
                Template.builder()
                    .emailTemplateId(EMAIL_TEMPLATE_ID)
                    .smsTemplateId(SMS_TEMPLATE_ID)
                    .build(),
                Destination.builder()
                    .email(EMAIL)
                    .sms(SMS_MOBILE)
                    .build(),
                null,
                new Reference(),
                null));

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        ArgumentCaptor<SubscriptionType> subscriptionTypeCaptor = ArgumentCaptor.forClass(SubscriptionType.class);
        then(factory).should(times(expectedSubscriptionTypes.length))
            .create(any(NotificationWrapper.class), subscriptionTypeCaptor.capture());
        assertArrayEquals(expectedSubscriptionTypes, subscriptionTypeCaptor.getAllValues().toArray());

        then(notificationHandler).should(times(wantedNumberOfEmailNotificationsSent)).sendNotification(
            eq(ccdNotificationWrapper), eq(EMAIL_TEMPLATE_ID), eq("Email"),
            any(NotificationHandler.SendNotification.class));
        then(notificationHandler).should(times(wantedNumberOfSmsNotificationsSent)).sendNotification(
            eq(ccdNotificationWrapper), eq(SMS_TEMPLATE_ID), eq("SMS"),
            any(NotificationHandler.SendNotification.class));

    }

    @Test
    @Parameters(method = "generateNotificationTypeAndSubscriptionsAppointeeScenarios")
    public void givenNotificationEventTypeAndAppointeeSubscriptionCombinations_shouldManageNotificationAndSubscriptionAccordingly(
            NotificationEventType notificationEventType, int wantedNumberOfEmailNotificationsSent,
            int wantedNumberOfSmsNotificationsSent, Subscription appointeeSubscription, Subscription repsSubscription,
            SubscriptionType[] expectedSubscriptionTypes) {

        ccdNotificationWrapper = buildNotificationWrapperGivenNotificationTypeAndAppointeeSubscriptions(
                notificationEventType, appointeeSubscription, repsSubscription);

        given(notificationValidService.isHearingTypeValidToSendNotification(
                any(SscsCaseData.class), eq(notificationEventType))).willReturn(true);

        given(notificationValidService.isNotificationStillValidToSend(anyList(), eq(notificationEventType)))
                .willReturn(true);

        given(notificationValidService.isFallbackLetterRequiredForSubscriptionType(any(), any(), any())).willReturn(true);

        given(factory.create(any(NotificationWrapper.class), any(SubscriptionType.class)))
            .willReturn(new Notification(
                Template.builder()
                    .emailTemplateId(EMAIL_TEMPLATE_ID)
                    .smsTemplateId(SMS_TEMPLATE_ID)
                    .build(),
                Destination.builder()
                    .email(EMAIL)
                    .sms(SMS_MOBILE)
                    .build(),
                null,
                new Reference(),
                null));

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        ArgumentCaptor<SubscriptionType> subscriptionTypeCaptor = ArgumentCaptor.forClass(SubscriptionType.class);
        then(factory).should(times(expectedSubscriptionTypes.length))
            .create(any(NotificationWrapper.class), subscriptionTypeCaptor.capture());
        assertArrayEquals(expectedSubscriptionTypes, subscriptionTypeCaptor.getAllValues().toArray());

        then(notificationHandler).should(times(wantedNumberOfEmailNotificationsSent)).sendNotification(
            eq(ccdNotificationWrapper), eq(EMAIL_TEMPLATE_ID), eq("Email"),
            any(NotificationHandler.SendNotification.class));
        then(notificationHandler).should(times(wantedNumberOfSmsNotificationsSent)).sendNotification(
            eq(ccdNotificationWrapper), eq(SMS_TEMPLATE_ID), eq("SMS"),
            any(NotificationHandler.SendNotification.class));

    }

    @Test
    @Parameters(method = "generateNotificationTypeAndSubscriptionsAppointeeWhenOldCaseReferenceScenarios")
    public void givenNotificationEventTypeAndAppointeeSubscriptionCombinationsWhenOldCaseReference_shouldManageNotificationAndSubscriptionAccordingly(
        NotificationEventType notificationEventType, int wantedNumberOfEmailNotificationsSent,
        int wantedNumberOfSmsNotificationsSent, Subscription appointeeSubscription, Subscription repsSubscription,
        SubscriptionType[] expectedSubscriptionTypes) {

        SscsCaseData oldCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().appellant(Appellant.builder().build())
                .hearingType(AppealHearingType.ORAL.name())
                .hearingOptions(HearingOptions.builder()
                    .wantsToAttend(YES)
                    .build())
                .build())
            .subscriptions(Subscriptions.builder()
                .appellantSubscription(Subscription.builder().build())
                .appointeeSubscription(appointeeSubscription)
                .representativeSubscription(repsSubscription)
                .build())
            .caseReference(CASE_REFERENCE)
            .hearings(Collections.singletonList(Hearing.builder().build()))
            .build();

        ccdNotificationWrapper = buildNotificationWrapperGivenNotificationTypeAndAppointeeSubscriptions(
            notificationEventType, appointeeSubscription, repsSubscription, oldCaseData);

        given(notificationValidService.isHearingTypeValidToSendNotification(
            any(SscsCaseData.class), eq(notificationEventType))).willReturn(true);

        given(notificationValidService.isNotificationStillValidToSend(anyList(), eq(notificationEventType)))
            .willReturn(true);

        given(factory.create(any(NotificationWrapper.class), any(SubscriptionType.class)))
                .willReturn(new Notification(
                        Template.builder()
                                .emailTemplateId(EMAIL_TEMPLATE_ID)
                                .smsTemplateId(SMS_TEMPLATE_ID)
                                .build(),
                        Destination.builder()
                                .email(EMAIL)
                                .sms(SMS_MOBILE)
                                .build(),
                        null,
                        new Reference(),
                        null));

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        ArgumentCaptor<SubscriptionType> subscriptionTypeCaptor = ArgumentCaptor.forClass(SubscriptionType.class);
        then(factory).should(times(expectedSubscriptionTypes.length))
                .create(any(NotificationWrapper.class), subscriptionTypeCaptor.capture());
        assertArrayEquals(expectedSubscriptionTypes, subscriptionTypeCaptor.getAllValues().toArray());

        then(notificationHandler).should(times(wantedNumberOfEmailNotificationsSent)).sendNotification(
                eq(ccdNotificationWrapper), eq(EMAIL_TEMPLATE_ID), eq("Email"),
                any(NotificationHandler.SendNotification.class));
        then(notificationHandler).should(times(wantedNumberOfSmsNotificationsSent)).sendNotification(
                eq(ccdNotificationWrapper), eq(SMS_TEMPLATE_ID), eq("SMS"),
                any(NotificationHandler.SendNotification.class));

    }

    @SuppressWarnings({"Indentation", "UnusedPrivateMethod"})
    private Object[] generateNotificationTypeAndSubscriptionsScenarios() {
        return new Object[]{
                new Object[]{
                        SYA_APPEAL_CREATED_NOTIFICATION,
                        1,
                        0,
                        Subscription.builder()
                                .tya(APPEAL_NUMBER)
                                .email(EMAIL)
                                .subscribeEmail(YES)
                                .build(),
                        null,
                        null,
                        new SubscriptionType[]{APPELLANT},
                },
                new Object[]{
                        APPEAL_LAPSED_NOTIFICATION,
                        1,
                        1,
                        Subscription.builder()
                                .tya(APPEAL_NUMBER)
                                .email(EMAIL)
                                .subscribeEmail(YES)
                                .mobile(MOBILE_NUMBER_1)
                                .subscribeSms(YES)
                                .build(),
                        null,
                        null,
                        new SubscriptionType[]{APPELLANT},
                },
                new Object[]{
                        APPEAL_LAPSED_NOTIFICATION,
                        2,
                        1,
                        Subscription.builder()
                                .tya(APPEAL_NUMBER)
                                .email(EMAIL)
                                .subscribeEmail(YES)
                                .mobile(MOBILE_NUMBER_1)
                                .subscribeSms(YES)
                                .build(),
                        Subscription.builder()
                                .tya(APPEAL_NUMBER)
                                .email(EMAIL)
                                .subscribeEmail(YES)
                                .build(),
                        null,
                        new SubscriptionType[]{APPELLANT, REPRESENTATIVE},
                },
                new Object[]{
                        APPEAL_LAPSED_NOTIFICATION,
                        2,
                        2,
                        Subscription.builder()
                                .tya(APPEAL_NUMBER)
                                .email(EMAIL)
                                .subscribeEmail(YES)
                                .mobile(MOBILE_NUMBER_1)
                                .subscribeSms(YES)
                                .build(),
                        Subscription.builder()
                                .tya(APPEAL_NUMBER)
                                .email(EMAIL)
                                .subscribeEmail(YES)
                                .mobile(MOBILE_NUMBER_1)
                                .subscribeSms(YES)
                                .build(),
                        null,
                        new SubscriptionType[]{APPELLANT, REPRESENTATIVE},
                },
                new Object[]{
                    APPEAL_RECEIVED_NOTIFICATION,
                    1,
                    1,
                    Subscription.builder()
                        .tya(APPEAL_NUMBER)
                        .email(EMAIL)
                        .subscribeEmail(YES)
                        .mobile(MOBILE_NUMBER_1)
                        .subscribeSms(YES)
                        .build(),
                    null,
                    null,
                    new SubscriptionType[]{APPELLANT},
                },
                new Object[]{
                    APPEAL_RECEIVED_NOTIFICATION,
                    2,
                    1,
                    Subscription.builder()
                        .tya(APPEAL_NUMBER)
                        .email(EMAIL)
                        .subscribeEmail(YES)
                        .mobile(MOBILE_NUMBER_1)
                        .subscribeSms(YES)
                        .build(),
                    Subscription.builder()
                        .tya(APPEAL_NUMBER)
                        .email(EMAIL)
                        .subscribeEmail(YES)
                        .build(),
                    null,
                    new SubscriptionType[]{APPELLANT, REPRESENTATIVE},
                },
                new Object[]{
                    APPEAL_RECEIVED_NOTIFICATION,
                    2,
                    2,
                    Subscription.builder()
                        .tya(APPEAL_NUMBER)
                        .email(EMAIL)
                        .subscribeEmail(YES)
                        .mobile(MOBILE_NUMBER_1)
                        .subscribeSms(YES)
                        .build(),
                    Subscription.builder()
                        .tya(APPEAL_NUMBER)
                        .email(EMAIL)
                        .subscribeEmail(YES)
                        .mobile(MOBILE_NUMBER_1)
                        .subscribeSms(YES)
                        .build(),
                    null,
                    new SubscriptionType[]{APPELLANT, REPRESENTATIVE},
                },
                new Object[]{
                        SYA_APPEAL_CREATED_NOTIFICATION,
                        0,
                        0,
                        null,
                        null,
                        null,
                        new SubscriptionType[]{APPELLANT},  // Fallback letter
                },
                new Object[]{
                        SYA_APPEAL_CREATED_NOTIFICATION,
                        1,
                        0,
                        Subscription.builder()
                                .tya(APPEAL_NUMBER)
                                .email(EMAIL)
                                .subscribeEmail(YES)
                                .build(),
                        null,
                        null,
                        new SubscriptionType[]{APPELLANT},
                },
                new Object[]{
                        SYA_APPEAL_CREATED_NOTIFICATION,
                        1,
                        1,
                        Subscription.builder()
                                .tya(APPEAL_NUMBER)
                                .email(EMAIL)
                                .subscribeEmail(YES)
                                .subscribeSms(YES)
                                .mobile(MOBILE_NUMBER_1)
                                .build(),
                        null,
                        null,
                        new SubscriptionType[]{APPELLANT},
                },
                new Object[]{
                        SYA_APPEAL_CREATED_NOTIFICATION,
                        0,
                        0,
                        Subscription.builder().build(),
                        Subscription.builder().build(),
                        Subscription.builder().build(),
                        new SubscriptionType[]{APPOINTEE, REPRESENTATIVE},  // Fallback letter
                },
                new Object[]{
                    CASE_UPDATED,
                        2,
                        0,
                        null,
                        Subscription.builder()
                                .tya(APPEAL_NUMBER)
                                .email(EMAIL)
                                .subscribeEmail(YES)
                                .mobile(MOBILE_NUMBER_1)
                                .build(),
                        Subscription.builder()
                                .tya(APPEAL_NUMBER)
                                .email(EMAIL)
                                .subscribeEmail(YES)
                                .mobile(MOBILE_NUMBER_1)
                                .build(),
                        new SubscriptionType[]{APPOINTEE, REPRESENTATIVE},
                },
                new Object[]{
                    CASE_UPDATED,
                        1,
                        0,
                        null,
                        Subscription.builder()
                                .tya(APPEAL_NUMBER)
                                .email(EMAIL)
                                .subscribeEmail(YES)
                                .mobile(MOBILE_NUMBER_1)
                                .build(),
                        Subscription.builder()
                                .tya(APPEAL_NUMBER)
                                .mobile(MOBILE_NUMBER_1)
                                .build(),
                        new SubscriptionType[]{APPOINTEE, REPRESENTATIVE},
                },
                new Object[]{
                    CASE_UPDATED,
                        0,
                        0,
                        Subscription.builder()
                                .tya(APPEAL_NUMBER)
                                .mobile(MOBILE_NUMBER_1)
                                .build(),
                        Subscription.builder()
                                .tya(APPEAL_NUMBER)
                                .mobile(MOBILE_NUMBER_1)
                                .build(),
                        Subscription.builder()
                                .tya(APPEAL_NUMBER)
                                .mobile(MOBILE_NUMBER_1)
                                .build(),
                        new SubscriptionType[]{APPOINTEE, REPRESENTATIVE},
                }
        };
    }

    @SuppressWarnings({"Indentation", "UnusedPrivateMethod"})
    private Object[] generateNotificationTypeAndSubscriptionsWhenOldCaseReferenceScenarios() {
        return new Object[]{
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                1,
                0,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .build(),
                null,
                null,
                new SubscriptionType[]{APPELLANT},
            },
            new Object[]{
                APPEAL_LAPSED_NOTIFICATION,
                1,
                1,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES)
                    .build(),
                null,
                null,
                new SubscriptionType[]{APPELLANT},
            },
            new Object[]{
                APPEAL_LAPSED_NOTIFICATION,
                2,
                1,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES)
                    .build(),
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .build(),
                null,
                new SubscriptionType[]{APPELLANT, REPRESENTATIVE},
            },
            new Object[]{
                APPEAL_LAPSED_NOTIFICATION,
                2,
                2,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES)
                    .build(),
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES)
                    .build(),
                null,
                new SubscriptionType[]{APPELLANT, REPRESENTATIVE},
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                1,
                1,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES)
                    .build(),
                null,
                null,
                new SubscriptionType[]{APPELLANT},
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                2,
                1,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES)
                    .build(),
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .build(),
                null,
                new SubscriptionType[]{APPELLANT, REPRESENTATIVE},
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                2,
                2,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES)
                    .build(),
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES)
                    .build(),
                null,
                new SubscriptionType[]{APPELLANT, REPRESENTATIVE},
            },
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                0,
                0,
                null,
                null,
                null,
                new SubscriptionType[]{},
            },
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                1,
                0,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .build(),
                null,
                null,
                new SubscriptionType[]{APPELLANT},
            },
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                1,
                1,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .subscribeSms(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .build(),
                null,
                null,
                new SubscriptionType[]{APPELLANT},
            },
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                0,
                0,
                Subscription.builder().build(),
                Subscription.builder().build(),
                Subscription.builder().build(),
                new SubscriptionType[]{},
            },
            new Object[]{
                CASE_UPDATED,
                2,
                0,
                null,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .build(),
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .build(),
                new SubscriptionType[]{APPOINTEE, REPRESENTATIVE},
            },
            new Object[]{
                CASE_UPDATED,
                1,
                0,
                null,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .build(),
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .mobile(MOBILE_NUMBER_1)
                    .build(),
                new SubscriptionType[]{APPOINTEE, REPRESENTATIVE},
            },
            new Object[]{
                CASE_UPDATED,
                0,
                0,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .mobile(MOBILE_NUMBER_1)
                    .build(),
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .mobile(MOBILE_NUMBER_1)
                    .build(),
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .mobile(MOBILE_NUMBER_1)
                    .build(),
                new SubscriptionType[]{APPOINTEE, REPRESENTATIVE},
            }
        };
    }

    @SuppressWarnings("Indentation")
    private Object[] generateNotificationTypeAndSubscriptionsAppointeeScenarios() {
        return new Object[]{
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                1,
                0,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .build(),
                null,
                new SubscriptionType[]{APPOINTEE},
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                1,
                1,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES)
                    .build(),
                null,
                new SubscriptionType[]{APPOINTEE},
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                2,
                1,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES)
                    .build(),
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .build(),
                new SubscriptionType[]{APPOINTEE, REPRESENTATIVE},
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                2,
                2,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES)
                    .build(),
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES)
                    .build(),
                new SubscriptionType[]{APPOINTEE, REPRESENTATIVE},
            },
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                0,
                0,
                null,
                null,
                new SubscriptionType[]{APPELLANT},
            },
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                1,
                0,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .build(),
                null,
                new SubscriptionType[]{APPOINTEE},
            },
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                1,
                1,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .subscribeSms(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .build(),
                null,
                new SubscriptionType[]{APPOINTEE},
            },
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                0,
                0,
                Subscription.builder().build(),
                Subscription.builder().build(),
                new SubscriptionType[]{APPOINTEE, REPRESENTATIVE},
            }
        };
    }

    @SuppressWarnings("Indentation")
    private Object[] generateNotificationTypeAndSubscriptionsAppointeeWhenOldCaseReferenceScenarios() {
        return new Object[]{
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                1,
                0,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .build(),
                null,
                new SubscriptionType[]{APPOINTEE},
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                1,
                1,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES)
                    .build(),
                null,
                new SubscriptionType[]{APPOINTEE},
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                2,
                1,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES)
                    .build(),
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .build(),
                new SubscriptionType[]{APPOINTEE, REPRESENTATIVE},
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                2,
                2,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES)
                    .build(),
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES)
                    .build(),
                new SubscriptionType[]{APPOINTEE, REPRESENTATIVE},
            },
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                0,
                0,
                null,
                null,
                new SubscriptionType[]{},
            },
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                1,
                0,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .build(),
                null,
                new SubscriptionType[]{APPOINTEE},
            },
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                1,
                1,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .subscribeSms(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .build(),
                null,
                new SubscriptionType[]{APPOINTEE},
            },
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                0,
                0,
                Subscription.builder().build(),
                Subscription.builder().build(),
                new SubscriptionType[]{},
            }
        };
    }

    private CcdNotificationWrapper buildNotificationWrapperGivenNotificationTypeAndSubscriptions(
            NotificationEventType notificationEventType, Subscription appellantSubscription,
            Subscription repsSubscription, Subscription appointeeSubscription) {
        return buildNotificationWrapperGivenNotificationTypeAndSubscriptions(notificationEventType, appellantSubscription, repsSubscription, appointeeSubscription, null);
    }


    private CcdNotificationWrapper buildNotificationWrapperGivenNotificationTypeAndSubscriptions(
        NotificationEventType notificationEventType, Subscription appellantSubscription,
        Subscription repsSubscription, Subscription appointeeSubscription, SscsCaseData oldCaseData) {

        Representative rep = null;
        if (repsSubscription != null) {
            rep = Representative.builder()
                .hasRepresentative("Yes")
                .name(Name.builder().firstName("Joe").lastName("Bloggs").build())
                .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 7SE").build())
                .build();
        }

        Appellant appellant = Appellant.builder().build();
        if (appointeeSubscription != null) {
            appellant.setAppointee(Appointee.builder()
                .name(Name.builder().firstName("Jack").lastName("Smith").build())
                .build());
        }

        sscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .appellant(appellant)
                .rep(rep)
                .hearingType(AppealHearingType.ORAL.name())
                .hearingOptions(HearingOptions.builder()
                    .wantsToAttend(YES)
                    .build())
                .build())
            .subscriptions(Subscriptions.builder()
                .appellantSubscription(appellantSubscription)
                .representativeSubscription(repsSubscription)
                .appointeeSubscription(appointeeSubscription)
                .build())
            .caseReference(CASE_REFERENCE)
            .hearings(Collections.singletonList(Hearing.builder().build()))
            .build();

        sscsCaseDataWrapper = SscsCaseDataWrapper.builder()
            .oldSscsCaseData(oldCaseData)
            .newSscsCaseData(sscsCaseData)
            .notificationEventType(notificationEventType)
            .build();

        return new CcdNotificationWrapper(sscsCaseDataWrapper);
    }

    private CcdNotificationWrapper buildNotificationWrapperGivenNotificationTypeAndAppointeeSubscriptions(
        NotificationEventType notificationEventType, Subscription appointeeSubscription,
        Subscription repsSubscription) {
        return buildNotificationWrapperGivenNotificationTypeAndAppointeeSubscriptions(notificationEventType, appointeeSubscription, repsSubscription, null);
    }

    private CcdNotificationWrapper buildNotificationWrapperGivenNotificationTypeAndAppointeeSubscriptions(
        NotificationEventType notificationEventType, Subscription appointeeSubscription,
        Subscription repsSubscription, SscsCaseData oldCaseData) {

        Representative rep = null;
        if (repsSubscription != null) {
            rep = Representative.builder()
                .hasRepresentative("Yes")
                .name(Name.builder().firstName("Joe").lastName("Bloggs").build())
                .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 7SE").build())
                .build();
        }

        Appointee appointee = null;
        if (appointeeSubscription != null) {
            appointee = Appointee.builder()
                .name(Name.builder().firstName("Jack").lastName("Johnson").build())
                .address(Address.builder().line1("Appellant Line 1").town("Appellant Town").county("Appellant County").postcode("AP9 7LL").build())
                .build();
        }

        sscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().appellant(Appellant.builder().appointee(appointee).build())
                .rep(rep)
                .hearingType(AppealHearingType.ORAL.name())
                .hearingOptions(HearingOptions.builder()
                    .wantsToAttend(YES)
                    .build())
                .build())
            .subscriptions(Subscriptions.builder()
                .appellantSubscription(Subscription.builder().build())
                .appointeeSubscription(appointeeSubscription)
                .representativeSubscription(repsSubscription)
                .build())
            .caseReference(CASE_REFERENCE)
            .hearings(Collections.singletonList(Hearing.builder().build()))
            .build();

        sscsCaseDataWrapper = SscsCaseDataWrapper.builder()
            .oldSscsCaseData(oldCaseData)
            .newSscsCaseData(sscsCaseData)
            .notificationEventType(notificationEventType)
            .build();

        return new CcdNotificationWrapper(sscsCaseDataWrapper);
    }

    @Test
    public void sendEmailToGovNotifyWhenNotificationIsAnEmailAndTemplateNotBlank() {
        String emailTemplateId = "abc";
        Notification notification = new Notification(Template.builder().emailTemplateId(emailTemplateId).smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), null, new Reference(), null);
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(factory.create(ccdNotificationWrapper, APPELLANT)).thenReturn(notification);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        verify(notificationHandler, times(1)).sendNotification(eq(ccdNotificationWrapper), eq(emailTemplateId), eq(EMAIL), any(NotificationHandler.SendNotification.class));
    }

    @Test
    public void sendSmsToGovNotifyWhenNotificationIsAnSmsAndTemplateNotBlank() {
        String smsTemplateId = "123";
        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId(smsTemplateId).build(), Destination.builder().email(null).sms("07823456746").build(), null, new Reference(), null);
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(factory.create(ccdNotificationWrapper, APPELLANT)).thenReturn(notification);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        verify(notificationHandler, times(1)).sendNotification(eq(ccdNotificationWrapper), eq(smsTemplateId), eq(SMS), any(NotificationHandler.SendNotification.class));
    }

    @Test
    public void sendSmsAndEmailToGovNotifyWhenNotificationIsAnSmsAndEmailAndTemplateNotBlank() {
        String emailTemplateId = "abc";
        String smsTemplateId = "123";
        Notification notification = new Notification(Template.builder().emailTemplateId(emailTemplateId).smsTemplateId(smsTemplateId).build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), null, new Reference(), null);
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(factory.create(ccdNotificationWrapper, APPELLANT)).thenReturn(notification);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        verify(notificationHandler, times(1)).sendNotification(eq(ccdNotificationWrapper), eq(emailTemplateId), eq(EMAIL), any(NotificationHandler.SendNotification.class));
        verify(notificationHandler, times(1)).sendNotification(eq(ccdNotificationWrapper), eq(smsTemplateId), eq(SMS), any(NotificationHandler.SendNotification.class));
    }

    @Test
    public void doNotSendEmailToGovNotifyWhenNotificationIsNotAnEmail() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId("123").build(), Destination.builder().email(null).sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(ccdNotificationWrapper, APPELLANT)).thenReturn(notification);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference(), ccdNotificationWrapper.getCaseId());
    }

    @Test
    public void doNotSendSmsToGovNotifyWhenNotificationIsNotAnSms() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId("123").build(), Destination.builder().email("test@testing.com").sms(null).build(), null, new Reference(), null);
        when(factory.create(ccdNotificationWrapper, APPELLANT)).thenReturn(notification);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        verify(notificationSender, never()).sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference(), notification.getSmsSenderTemplate(), ccdNotificationWrapper.getCaseId());
    }

    @Test
    public void doNotSendEmailToGovNotifyWhenEmailTemplateIsBlank() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId("123").build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(ccdNotificationWrapper, APPELLANT)).thenReturn(notification);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference(), ccdNotificationWrapper.getCaseId());
    }

    @Test
    public void doNotSendSmsToGovNotifyWhenSmsTemplateIsBlank() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(ccdNotificationWrapper, APPELLANT)).thenReturn(notification);
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
        when(factory.create(ccdNotificationWrapper, APPELLANT)).thenReturn(notification);

        notificationService.manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));

        verify(notificationSender, never()).sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference(), notification.getSmsSenderTemplate(), ccdNotificationWrapper.getCaseId());
        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference(), ccdNotificationWrapper.getCaseId());
    }

    @Test
    public void createsReminders() {

        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId("123").build(), Destination.builder().email(null).sms("07823456746").build(), null, new Reference(), null);

        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(factory.create(ccdNotificationWrapper, APPELLANT)).thenReturn(notification);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        verify(reminderService).createReminders(ccdNotificationWrapper);
    }

    @Test
    public void doNotSendNotificationWhenNotificationNotValidToSend() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), null, new Reference(), null);
        when(factory.create(ccdNotificationWrapper, APPELLANT)).thenReturn(notification);
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(false);

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference(), ccdNotificationWrapper.getCaseId());
    }

    @Test
    public void doNotSendNotificationWhenHearingTypeIsNotValidToSend() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), null, new Reference(), null);
        when(factory.create(ccdNotificationWrapper, APPELLANT)).thenReturn(notification);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(false);

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference(), ccdNotificationWrapper.getCaseId());
    }

    @Test
    public void doNotSendNotificationsOutOfHours() {
        SscsCaseDataWrapper wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(sscsCaseData).oldSscsCaseData(sscsCaseData).notificationEventType(QUESTION_ROUND_ISSUED_NOTIFICATION).build();
        ccdNotificationWrapper = new CohNotificationWrapper("someHearingId", wrapper);
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId("123").build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), null, new Reference(), null);
        when(factory.create(ccdNotificationWrapper, APPELLANT)).thenReturn(notification);
        when(outOfHoursCalculator.isItOutOfHours()).thenReturn(true);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        verify(notificationHandler, never()).sendNotification(any(), any(), any(), any());
        verify(notificationHandler).scheduleNotification(ccdNotificationWrapper);
        verifyNoMoreInteractions(reminderService);
    }

    @Test
    public void shouldSendEmailAndSmsToOldEmailAddressForEmailSubscriptionUpdateForPaperCase() {
        Subscription appellantNewSubscription = Subscription.builder().tya(APPEAL_NUMBER).email(NEW_TEST_EMAIL_COM)
                .mobile(MOBILE_NUMBER_1).subscribeEmail(YES).subscribeSms(YES).build();
        Subscription appellantOldSubscription = Subscription.builder().tya(APPEAL_NUMBER).email("oldtest@email.com")
                .mobile(MOBILE_NUMBER_2).subscribeEmail(YES).subscribeSms(YES).build();

        SscsCaseData newSscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder().appellant(Appellant.builder().build())
                        .hearingType(AppealHearingType.PAPER.name()).benefitType(BenefitType.builder().code(PIP).build()).build())
                .subscriptions(Subscriptions.builder().appellantSubscription(appellantNewSubscription).build())
                .caseReference(CASE_REFERENCE).build();

        SscsCaseData oldSscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder().appellant(Appellant.builder().build())
                        .hearingType(AppealHearingType.PAPER.name()).benefitType(BenefitType.builder().code(PIP).build()).build())
                .subscriptions(Subscriptions.builder().appellantSubscription(appellantOldSubscription).build())
                .caseReference(CASE_REFERENCE).build();

        SscsCaseDataWrapper wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(newSscsCaseData).oldSscsCaseData(oldSscsCaseData).notificationEventType(SUBSCRIPTION_UPDATED_NOTIFICATION).build();
        ccdNotificationWrapper = new CcdNotificationWrapper(wrapper);

        Notification notification = new Notification(
                Template.builder().emailTemplateId(EMAIL_TEMPLATE_ID).smsTemplateId(SMS_TEMPLATE_ID).build(),
                Destination.builder().email(NEW_TEST_EMAIL_COM).sms(MOBILE_NUMBER_2).build(), null, new Reference(), null);
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(factory.create(ccdNotificationWrapper, APPELLANT)).thenReturn(notification);
        when(notificationConfig.getTemplate(any(), any(), any(), any(), any())).thenReturn(Template.builder().emailTemplateId(EMAIL_TEMPLATE_ID).smsTemplateId(SMS_TEMPLATE_ID).build());

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
                .appeal(Appeal.builder().appellant(Appellant.builder().build())
                        .hearingType(AppealHearingType.PAPER.name()).benefitType(BenefitType.builder().code(PIP).build()).build())
                .subscriptions(Subscriptions.builder().appellantSubscription(appellantNewSubscription).build())
                .caseReference(CASE_REFERENCE).build();

        SscsCaseData oldSscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder().appellant(Appellant.builder().build())
                        .hearingType(AppealHearingType.PAPER.name()).benefitType(BenefitType.builder().code(PIP).build()).build())
                .subscriptions(Subscriptions.builder().appellantSubscription(appellantOldSubscription).build())
                .caseReference(CASE_REFERENCE).build();

        SscsCaseDataWrapper wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(newSscsCaseData)
                .oldSscsCaseData(oldSscsCaseData).notificationEventType(SUBSCRIPTION_UPDATED_NOTIFICATION).build();
        ccdNotificationWrapper = new CcdNotificationWrapper(wrapper);

        Notification notification = new Notification(
                Template.builder().emailTemplateId(EMAIL_TEMPLATE_ID).smsTemplateId(SMS_TEMPLATE_ID).build(),
                Destination.builder().email(NEW_TEST_EMAIL_COM).sms(MOBILE_NUMBER_2).build(), null, new Reference(), null);

        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(factory.create(ccdNotificationWrapper, APPELLANT)).thenReturn(notification);
        when(notificationConfig.getTemplate(any(), any(), any(), any(), any())).thenReturn(Template.builder().emailTemplateId(EMAIL_TEMPLATE_ID).smsTemplateId(SMS_TEMPLATE_ID).build());

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        verify(notificationHandler, times(1)).sendNotification(eq(ccdNotificationWrapper), any(), eq(EMAIL), any(NotificationHandler.SendNotification.class));
        verify(notificationHandler, times(1)).sendNotification(eq(ccdNotificationWrapper), any(), eq(SMS), any(NotificationHandler.SendNotification.class));

    }

    @Test
    public void shouldNotSendEmailAndSmsToOldEmailAddressIfOldEmailAddressAndSmsNotPresent() {
        Subscription appellantNewSubscription = Subscription.builder()
                .tya(APPEAL_NUMBER)
                .email(SAME_TEST_EMAIL_COM)
                .mobile(MOBILE_NUMBER_1)
                .subscribeEmail(YES)
                .subscribeSms(YES)
                .build();
        Subscription appellantOldSubscription = Subscription.builder()
                .tya(APPEAL_NUMBER)
                .build();

        SscsCaseData newSscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder()
                        .appellant(Appellant.builder().build())
                        .hearingType(AppealHearingType.PAPER.name())
                        .benefitType(BenefitType.builder()
                                .code(PIP)
                                .build())
                        .build())
                .subscriptions(Subscriptions.builder()
                        .appellantSubscription(appellantNewSubscription)
                        .build())
                .caseReference(CASE_REFERENCE).build();

        SscsCaseData oldSscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder()
                        .appellant(Appellant.builder().build())
                        .hearingType(AppealHearingType.PAPER.name())
                        .benefitType(BenefitType.builder()
                                .code(PIP)
                                .build())
                        .build())
                .subscriptions(Subscriptions.builder()
                        .appellantSubscription(appellantOldSubscription).build())
                .caseReference(CASE_REFERENCE).build();

        SscsCaseDataWrapper wrapper = SscsCaseDataWrapper.builder()
                .newSscsCaseData(newSscsCaseData)
                .oldSscsCaseData(oldSscsCaseData)
                .notificationEventType(SUBSCRIPTION_UPDATED_NOTIFICATION)
                .build();
        ccdNotificationWrapper = new CcdNotificationWrapper(wrapper);

        Notification notification = new Notification(
                Template.builder()
                        .emailTemplateId(EMAIL_TEMPLATE_ID)
                        .smsTemplateId(SMS_TEMPLATE_ID)
                        .build(),
                Destination.builder()
                        .email(NEW_TEST_EMAIL_COM)
                        .sms(MOBILE_NUMBER_2)
                        .build(),
                null,
                new Reference(),
                null);

        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(factory.create(ccdNotificationWrapper, APPELLANT)).thenReturn(notification);
        when(notificationConfig.getTemplate(any(), any(), any(), any(), any())).thenReturn(Template.builder().emailTemplateId(EMAIL_TEMPLATE_ID).smsTemplateId(SMS_TEMPLATE_ID).build());

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper);

        verify(notificationHandler, times(1)).sendNotification(eq(ccdNotificationWrapper), any(), eq(EMAIL), any(NotificationHandler.SendNotification.class));
        verify(notificationHandler, times(1)).sendNotification(eq(ccdNotificationWrapper), any(), eq(SMS), any(NotificationHandler.SendNotification.class));
    }

    @Test
    public void sendBundledLetterToGovNotifyWhenStruckOutNotification() throws IOException {
        String fileUrl = "http://dm-store:4506/documents/1e1eb3d2-5b6c-430d-8dad-ebcea1ad7ecf";

        CcdNotificationWrapper struckOutCcdNotificationWrapper = buildWrapperWithDocuments(STRUCK_OUT, fileUrl, APPELLANT_WITH_ADDRESS, null);

        Notification notification = new Notification(Template.builder().letterTemplateId(LETTER_TEMPLATE_ID_STRUCKOUT).build(), Destination.builder().build(), new HashMap<>(), new Reference(), null);

        byte[] sampleDirectionText = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdfs/direction-text.pdf"));
        byte[] sampleDirectionCoversheet = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdfs/direction-notice-coversheet-sample.pdf"));

        when(evidenceManagementService.download(URI.create(fileUrl), DM_STORE_USER_ID)).thenReturn(sampleDirectionText);
        when((notificationValidService).isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when((notificationValidService).isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);
        when(sscsGeneratePdfService.generatePdf(anyString(), any(), any(), any())).thenReturn(sampleDirectionCoversheet);

        when(factory.create(struckOutCcdNotificationWrapper, APPELLANT)).thenReturn(notification);
        notificationService.manageNotificationAndSubscription(struckOutCcdNotificationWrapper);

        verify(notificationHandler, times(1)).sendNotification(eq(struckOutCcdNotificationWrapper), eq(LETTER_TEMPLATE_ID_STRUCKOUT), eq(LETTER), any(NotificationHandler.SendNotification.class));
    }

    @Test(expected = NotificationServiceException.class)
    public void sendBundledLettersToGovNotifyWhenStruckOutNotificationFailsAtNotify() throws IOException {
        String fileUrl = "http://dm-store:4506/documents/1e1eb3d2-5b6c-430d-8dad-ebcea1ad7ecf";

        CcdNotificationWrapper struckOutCcdNotificationWrapper = buildWrapperWithDocuments(STRUCK_OUT, fileUrl, APPELLANT_WITH_ADDRESS, null);

        Notification notification = new Notification(Template.builder().letterTemplateId(LETTER_TEMPLATE_ID_STRUCKOUT).build(), Destination.builder().build(), new HashMap<>(), new Reference(), null);

        byte[] sampleDirectionText = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdfs/direction-text.pdf"));
        byte[] sampleDirectionCoversheet = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdfs/direction-notice-coversheet-sample.pdf"));

        when(evidenceManagementService.download(URI.create(fileUrl), DM_STORE_USER_ID)).thenReturn(sampleDirectionText);
        when(evidenceManagementService.download(URI.create(fileUrl), DM_STORE_USER_ID)).thenReturn(sampleDirectionText);
        when((notificationValidService).isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when((notificationValidService).isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);
        when(sscsGeneratePdfService.generatePdf(anyString(), any(), any(), any())).thenReturn(sampleDirectionCoversheet);

        doThrow(new NotificationServiceException("Forced exception", new RuntimeException())).when(notificationHandler).sendNotification(eq(struckOutCcdNotificationWrapper), eq(LETTER_TEMPLATE_ID_STRUCKOUT), eq(LETTER), any(NotificationHandler.SendNotification.class));

        when(factory.create(struckOutCcdNotificationWrapper, APPELLANT)).thenReturn(notification);
        notificationService.manageNotificationAndSubscription(struckOutCcdNotificationWrapper);
    }

    @Test
    public void doNotSendBundledLettersToGovNotifyWhenStruckOutNotificationWhenFeatureToggledOff() throws IOException {
        String fileUrl = "http://dm-store:4506/documents/1e1eb3d2-5b6c-430d-8dad-ebcea1ad7ecf";

        CcdNotificationWrapper struckOutCcdNotificationWrapper = buildWrapperWithDocuments(STRUCK_OUT, fileUrl, APPELLANT_WITH_ADDRESS, null);

        Notification notification = new Notification(Template.builder().letterTemplateId(LETTER_TEMPLATE_ID_STRUCKOUT).build(), Destination.builder().build(), new HashMap<>(), new Reference(), null);

        byte[] sampleDirectionText = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdfs/direction-text.pdf"));
        byte[] sampleDirectionCoversheet = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdfs/direction-notice-coversheet-sample.pdf"));

        when(evidenceManagementService.download(URI.create(fileUrl), DM_STORE_USER_ID)).thenReturn(sampleDirectionText);
        when((notificationValidService).isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when((notificationValidService).isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);
        when(sscsGeneratePdfService.generatePdf(anyString(), any(), any(), any())).thenReturn(sampleDirectionCoversheet);

        when(factory.create(struckOutCcdNotificationWrapper, APPELLANT)).thenReturn(notification);

        getNotificationService(false, false).manageNotificationAndSubscription(struckOutCcdNotificationWrapper);

        verify(notificationHandler, times(0)).sendNotification(eq(struckOutCcdNotificationWrapper), eq(LETTER_TEMPLATE_ID_STRUCKOUT), eq(LETTER), any(NotificationHandler.SendNotification.class));
    }

    private NotificationService getNotificationService(Boolean bundledLettersOn, Boolean lettersOn) {
        SendNotificationService sendNotificationService = new SendNotificationService(notificationSender, evidenceManagementService, sscsGeneratePdfService, notificationHandler, notificationValidService);

        final NotificationService notificationService = new NotificationService(factory, reminderService,
            notificationValidService, notificationHandler, outOfHoursCalculator, notificationConfig, sendNotificationService
        );
        ReflectionTestUtils.setField(sendNotificationService, "noncompliantcaseletterTemplate", "/templates/non_compliant_case_letter_template.html");
        ReflectionTestUtils.setField(sendNotificationService, "bundledLettersOn", bundledLettersOn);
        ReflectionTestUtils.setField(sendNotificationService, "lettersOn", lettersOn);
        return notificationService;
    }

    private CcdNotificationWrapper buildWrapperWithDocuments(NotificationEventType eventType, String fileUrl, Appellant appellant, Representative rep) {
        SscsDocumentDetails sscsDocumentDetails = SscsDocumentDetails.builder()
            .documentType("Strike Out Notice")
            .documentLink(
                DocumentLink.builder()
                    .documentUrl(fileUrl)
                    .documentFilename("direction-text.pdf")
                    .documentBinaryUrl(fileUrl + "/binary")
                    .build()
            )
            .build();

        SscsDocument sscsDocument = SscsDocument.builder().value(sscsDocumentDetails).build();

        return buildBaseWrapper(eventType, appellant, rep, sscsDocument);
    }

    static CcdNotificationWrapper buildBaseWrapper(NotificationEventType eventType, Appellant appellant, Representative rep, SscsDocument sscsDocument) {
        SscsCaseData sscsCaseDataWithDocuments = SscsCaseData.builder()
            .appeal(
                Appeal
                    .builder()
                    .hearingType(AppealHearingType.ORAL.name())
                    .hearingOptions(HearingOptions.builder().wantsToAttend(YES).build())
                    .appellant(appellant)
                    .rep(rep)
                    .build())
            .subscriptions(Subscriptions.builder().appellantSubscription(Subscription.builder()
                .tya(APPEAL_NUMBER)
                .email(EMAIL)
                .mobile(MOBILE_NUMBER_1)
                .subscribeEmail(YES)
                .subscribeSms(YES)
                .build()).build())
            .caseReference(CASE_REFERENCE)
            .ccdCaseId(CASE_ID)
            .sscsDocument(new ArrayList<>(Collections.singletonList(sscsDocument)))
            .build();

        SscsCaseDataWrapper struckOutSscsCaseDataWrapper = SscsCaseDataWrapper.builder()
            .newSscsCaseData(sscsCaseDataWithDocuments)
            .oldSscsCaseData(sscsCaseDataWithDocuments)
            .notificationEventType(eventType)
            .build();
        return new CcdNotificationWrapper(struckOutSscsCaseDataWrapper);
    }
}
