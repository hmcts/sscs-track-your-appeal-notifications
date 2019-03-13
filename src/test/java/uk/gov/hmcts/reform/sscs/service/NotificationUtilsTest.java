package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.SscsCaseDataUtils.CASE_ID;
import static uk.gov.hmcts.reform.sscs.SscsCaseDataUtils.addHearing;
import static uk.gov.hmcts.reform.sscs.SscsCaseDataUtils.addHearingOptions;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.YES;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.service.NotificationServiceTest.*;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.*;
import static uk.gov.hmcts.reform.sscs.service.SendNotificationServiceTest.APPELLANT_WITH_ADDRESS_AND_APPOINTEE;

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
import uk.gov.hmcts.reform.sscs.domain.notify.*;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;

@RunWith(JUnitParamsRunner.class)
public class NotificationUtilsTest {
    @Mock
    private NotificationValidService notificationValidService;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void trueWhenHasPopulatedAppointee() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
            null,
            null
        );

        assertTrue(hasAppointee(wrapper.getSscsCaseDataWrapper()));
    }

    @Test
    public void falseWhenHasNullPopulatedAppointee() {
        Appellant appellant = Appellant.builder()
            .name(Name.builder().firstName("Ap").lastName("pellant").build())
            .address(Address.builder().line1("Appellant Line 1").town("Appellant Town").county("Appellant County").postcode("AP9 3LL").build())
            .appointee(Appointee.builder().build())
            .build();

        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            appellant,
            null,
            null
        );

        assertFalse(hasAppointee(wrapper.getSscsCaseDataWrapper()));
    }

    @Test
    public void falseWhenHasNullAppointee() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS,
            null,
            null
        );

        assertFalse(hasAppointee(wrapper.getSscsCaseDataWrapper()));
    }

    @Test
    public void falseWhenNoFirstName() {
        assertFalse(hasAppointee(Appointee.builder().name(Name.builder().lastName("Last").build()).build()));
    }

    @Test
    public void falseWhenNoLastName() {
        assertFalse(hasAppointee(Appointee.builder().name(Name.builder().firstName("First").build()).build()));
    }

    @Test
    public void trueWhenHasFirstAndLastName() {
        assertTrue(hasAppointee(Appointee.builder().name(Name.builder().firstName("First").lastName("Last").build()).build()));
    }

    @Test
    public void trueWhenHasPopulatedRep() {
        Representative rep = Representative.builder()
            .hasRepresentative("Yes")
            .name(Name.builder().firstName("Joe").lastName("Bloggs").build())
            .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 7SE").build())
            .build();

        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS,
            rep,
            null
        );

        assertTrue(hasRepresentative(wrapper.getSscsCaseDataWrapper()));
    }

    @Test
    public void falseWhenHasPopulatedRepButHasRepSetToNo() {
        Representative rep = Representative.builder()
            .hasRepresentative("No")
            .name(Name.builder().firstName("Joe").lastName("Bloggs").build())
            .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 7SE").build())
            .build();

        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS,
            rep,
            null
        );

        assertFalse(hasRepresentative(wrapper.getSscsCaseDataWrapper()));
    }

    @Test
    public void falseWhenHasPopulatedRepButHasRepNotSet() {
        Representative rep = Representative.builder()
            .name(Name.builder().firstName("Joe").lastName("Bloggs").build())
            .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 7SE").build())
            .build();

        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS,
            rep,
            null
        );

        assertFalse(hasRepresentative(wrapper.getSscsCaseDataWrapper()));
    }

    @Test
    public void falseWhenHasNullPopulatedRep() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS,
            Representative.builder().build(),
            null
        );

        assertFalse(hasRepresentative(wrapper.getSscsCaseDataWrapper()));
    }

    @Test
    public void falseWhenHasNullRep() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS,
            null,
            null
        );

        assertFalse(hasRepresentative(wrapper.getSscsCaseDataWrapper()));
    }

    @Test
    public void shouldBeOkToSendNotificationForValidFutureNotification() {
        NotificationEventType eventType = HEARING_BOOKED_NOTIFICATION;
        NotificationWrapper wrapper = buildNotificationWrapper(eventType);

        Subscription subscription = Subscription.builder().subscribeSms("Yes").subscribeEmail("Yes").build();

        when(notificationValidService.isNotificationStillValidToSend(wrapper.getNewSscsCaseData().getHearings(), eventType)).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(wrapper.getNewSscsCaseData(), eventType)).thenReturn(true);

        assertTrue(isOkToSendNotification(wrapper, eventType, subscription, notificationValidService));
    }

    @Test
    public void shouldNotBeOkToSendNotificationValidPastNotification() {
        NotificationEventType eventType = HEARING_BOOKED_NOTIFICATION;
        NotificationWrapper wrapper = buildNotificationWrapper(eventType);

        Subscription subscription = Subscription.builder().subscribeSms("Yes").subscribeEmail("Yes").build();

        when(notificationValidService.isNotificationStillValidToSend(wrapper.getNewSscsCaseData().getHearings(), eventType)).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(wrapper.getNewSscsCaseData(), eventType)).thenReturn(false);

        assertFalse(isOkToSendNotification(wrapper, eventType, subscription, notificationValidService));
    }

    @Test
    public void shouldNotBeOkToSendNotificationInvalidNotification() {
        NotificationEventType eventType = HEARING_BOOKED_NOTIFICATION;
        NotificationWrapper wrapper = buildNotificationWrapper(eventType);

        Subscription subscription = Subscription.builder().subscribeSms("Yes").subscribeEmail("Yes").build();

        when(notificationValidService.isNotificationStillValidToSend(wrapper.getNewSscsCaseData().getHearings(), eventType)).thenReturn(false);
        when(notificationValidService.isHearingTypeValidToSendNotification(wrapper.getNewSscsCaseData(), eventType)).thenReturn(true);

        assertFalse(isOkToSendNotification(wrapper, eventType, subscription, notificationValidService));
    }

    private NotificationWrapper buildNotificationWrapper(NotificationEventType eventType) {
        SscsCaseData sscsCaseData = getSscsCaseDataBuilder(
            APPELLANT_WITH_ADDRESS,
            null,
            null
        ).build();
        addHearing(sscsCaseData, 1);
        addHearingOptions(sscsCaseData, "Yes");

        SscsCaseDataWrapper caseDataWrapper = SscsCaseDataWrapper.builder()
            .newSscsCaseData(sscsCaseData)
            .oldSscsCaseData(sscsCaseData)
            .notificationEventType(eventType)
            .build();
        return new CcdNotificationWrapper(caseDataWrapper);
    }

    @Test
    @Parameters(method = "mandatoryNotificationTypes")
    public void isMandatoryLetter(NotificationEventType eventType) {
        assertTrue(isMandatoryLetterEventType(buildNotificationWrapper(eventType)));
    }

    @Test
    @Parameters(method = "nonMandatoryNotificationTypes")
    public void isNotMandatoryLetter(NotificationEventType eventType) {
        assertFalse(isMandatoryLetterEventType(buildNotificationWrapper(eventType)));
    }

    @Test
    public void itIsOkToSendNotification() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS,
            null,
            null
        );

        Subscription subscription = Subscription.builder().subscribeEmail("test@test.com").subscribeSms("07800000000").build();

        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        assertTrue(NotificationUtils.isOkToSendNotification(wrapper, HEARING_BOOKED_NOTIFICATION, subscription, notificationValidService));
    }

    @Test
    @Parameters(method = "isNotOkToSendNotificationResponses")
    public void isNotOkToSendNotification(boolean isNotificationStillValidToSendResponse, boolean isHearingTypeValidToSendNotificationResponse) {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS,
            null,
            null
        );

        Subscription subscription = Subscription.builder().subscribeEmail("test@test.com").subscribeSms("07800000000").build();

        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(isNotificationStillValidToSendResponse);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(isHearingTypeValidToSendNotificationResponse);

        assertFalse(NotificationUtils.isOkToSendNotification(wrapper, HEARING_BOOKED_NOTIFICATION, subscription, notificationValidService));
    }

    @Test
    @Parameters(method = "fallbackLetterRequiredScenarios")
    public void fallbackLetterIsRequired(SubscriptionWithType subscriptionWithType, boolean isFallbackLetterRequiredForSubscriptionTypeResponse, NotificationEventType eventType) {
        when(notificationValidService.isFallbackLetterRequiredForSubscriptionType(any(), any(), any())).thenReturn(isFallbackLetterRequiredForSubscriptionTypeResponse);

        CcdNotificationWrapper wrapper = buildBaseWrapper(subscriptionWithType.getSubscription(), eventType);

        assertTrue(isFallbackLetterRequired(wrapper, subscriptionWithType, subscriptionWithType.getSubscription(), eventType, notificationValidService));
    }

    @Test
    @Parameters(method = "fallbackLetterNotRequiredScenarios")
    public void fallbackLetterIsNotRequired(SubscriptionWithType subscriptionWithType, Subscription subscription, boolean isFallbackLetterRequiredForSubscriptionTypeResponse, NotificationEventType eventType) {
        when(notificationValidService.isFallbackLetterRequiredForSubscriptionType(any(), any(), any())).thenReturn(isFallbackLetterRequiredForSubscriptionTypeResponse);

        CcdNotificationWrapper wrapper = buildBaseWrapper(subscription, eventType);

        assertFalse(isFallbackLetterRequired(wrapper, subscriptionWithType, subscription, eventType, notificationValidService));
    }

    @Test
    public void okToSendSmsNotificationisValid() {
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        CcdNotificationWrapper wrapper = buildBaseWrapper(null, null);

        Subscription subscription = Subscription.builder().subscribeSms("Yes").build();
        Notification notification = Notification.builder()
            .reference(new Reference("someref"))
            .destination(Destination.builder().sms("07800123456").build())
            .template(Template.builder().smsTemplateId("some.template").build())
            .build();

        assertTrue(isOkToSendSmsNotification(wrapper, subscription, notification, VIEW_ISSUED, notificationValidService));
    }

    @Test
    @Parameters(method = "isNotOkToSendSmsNotificationScenarios")
    public void okToSendSmsNotificationisNotValid(Subscription subscription, Notification notification) {
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        CcdNotificationWrapper wrapper = buildBaseWrapper(null, null);

        assertFalse(isOkToSendSmsNotification(wrapper, subscription, notification, VIEW_ISSUED, notificationValidService));
    }

    @Test
    public void okToSendEmailNotificationisValid() {
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        CcdNotificationWrapper wrapper = buildBaseWrapper(null, null);

        Subscription subscription = Subscription.builder().subscribeEmail("Yes").build();
        Notification notification = Notification.builder()
            .reference(new Reference("someref"))
            .destination(Destination.builder().email("test@test.com").build())
            .template(Template.builder().emailTemplateId("some.template").build())
            .build();

        assertTrue(isOkToSendEmailNotification(wrapper, subscription, notification, notificationValidService));
    }

    @Test
    @Parameters(method = "isNotOkToSendEmailNotificationScenarios")
    public void okToSendEmailNotificationisNotValid(Subscription subscription, Notification notification) {
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        CcdNotificationWrapper wrapper = buildBaseWrapper(null, null);

        assertFalse(isOkToSendEmailNotification(wrapper, subscription, notification, notificationValidService));
    }

    private Object[] mandatoryNotificationTypes() {
        return new Object[]{
            STRUCK_OUT,
            HEARING_BOOKED_NOTIFICATION
        };
    }

    private Object[] nonMandatoryNotificationTypes() {
        return new Object[]{
            ADJOURNED_NOTIFICATION,
            SYA_APPEAL_CREATED_NOTIFICATION,
            RESEND_APPEAL_CREATED_NOTIFICATION,
            APPEAL_LAPSED_NOTIFICATION,
            APPEAL_RECEIVED_NOTIFICATION,
            APPEAL_WITHDRAWN_NOTIFICATION,
            APPEAL_DORMANT_NOTIFICATION,
            EVIDENCE_RECEIVED_NOTIFICATION,
            DWP_RESPONSE_RECEIVED_NOTIFICATION,
            POSTPONEMENT_NOTIFICATION,
            SUBSCRIPTION_CREATED_NOTIFICATION,
            SUBSCRIPTION_UPDATED_NOTIFICATION,
            SUBSCRIPTION_OLD_NOTIFICATION,
            EVIDENCE_REMINDER_NOTIFICATION,
            FIRST_HEARING_HOLDING_REMINDER_NOTIFICATION,
            SECOND_HEARING_HOLDING_REMINDER_NOTIFICATION,
            THIRD_HEARING_HOLDING_REMINDER_NOTIFICATION,
            FINAL_HEARING_HOLDING_REMINDER_NOTIFICATION,
            HEARING_REMINDER_NOTIFICATION,
            DWP_RESPONSE_LATE_REMINDER_NOTIFICATION,
            QUESTION_ROUND_ISSUED_NOTIFICATION,
            QUESTION_DEADLINE_ELAPSED_NOTIFICATION,
            QUESTION_DEADLINE_REMINDER_NOTIFICATION,
            HEARING_REQUIRED_NOTIFICATION,
            VIEW_ISSUED,
            DECISION_ISSUED_2,
            CASE_UPDATED,
            DO_NOT_SEND
        };
    }

    private Object[] isNotOkToSendNotificationResponses() {
        return new Object[]{
            new Object[] {
                false, false
            },
            new Object[] {
                false, true
            },
            new Object[] {
                true, false
            }
        };
    }

    private static CcdNotificationWrapper buildBaseWrapper(Subscription subscription, NotificationEventType eventType) {
        Subscriptions subscriptions = null;
        if (null != subscription) {
            subscriptions = Subscriptions.builder().appellantSubscription(subscription).build();
        }

        SscsCaseData sscsCaseDataWithDocuments = SscsCaseData.builder()
            .appeal(
                Appeal
                    .builder()
                    .hearingType(AppealHearingType.ORAL.name())
                    .hearingOptions(HearingOptions.builder().wantsToAttend(YES).build())
                    .build())
            .subscriptions(subscriptions)
            .ccdCaseId(CASE_ID)
            .build();

        SscsCaseDataWrapper sscsCaseDataWrapper = SscsCaseDataWrapper.builder()
            .newSscsCaseData(sscsCaseDataWithDocuments)
            .oldSscsCaseData(sscsCaseDataWithDocuments)
            .notificationEventType(eventType)
            .build();
        return new CcdNotificationWrapper(sscsCaseDataWrapper);
    }

    private Object[] fallbackLetterRequiredScenarios() {
        return new Object[]{
            new Object[] {
                new SubscriptionWithType(Subscription.builder().subscribeEmail(YES).build(), SubscriptionType.APPELLANT),
                true,
                DO_NOT_SEND
            },
            new Object[] {
                new SubscriptionWithType(Subscription.builder().build(), SubscriptionType.APPELLANT),
                true,
                DO_NOT_SEND
            },
            new Object[] {
                new SubscriptionWithType(null, SubscriptionType.APPELLANT),
                true,
                DO_NOT_SEND
            }
        };
    }

    private Object[] fallbackLetterNotRequiredScenarios() {
        return new Object[]{
            new Object[] {
                new SubscriptionWithType(Subscription.builder().build(), SubscriptionType.APPELLANT),
                null,
                false,
                DO_NOT_SEND
            },
            new Object[] {
                new SubscriptionWithType(Subscription.builder().build(), SubscriptionType.APPELLANT),
                Subscription.builder().build(),
                false,
                DO_NOT_SEND
            }
        };
    }

    public Object[] isNotOkToSendSmsNotificationScenarios() {
        return new Object[] {
            new Object[] {
                null,
                null
            },
            new Object[] {
                null,
                Notification.builder()
                    .reference(new Reference("someref"))
                    .destination(Destination.builder().sms("07800123456").build())
                    .template(Template.builder().smsTemplateId("some.template").build())
                    .build()
            },
            new Object[] {
                Subscription.builder().subscribeSms("Yes").build(),
                Notification.builder()
                    .reference(new Reference("someref"))
                    .destination(Destination.builder().build())
                    .template(Template.builder().smsTemplateId("some.template").build())
                    .build()
            },
            new Object[] {
                Subscription.builder().subscribeSms("Yes").build(),
                Notification.builder()
                    .reference(new Reference("someref"))
                    .destination(Destination.builder().sms("07800123456").build())
                    .template(Template.builder().build())
                    .build()
            }
        };
    }

    public Object[] isNotOkToSendEmailNotificationScenarios() {
        return new Object[] {
            new Object[] {
                null,
                null
            },
            new Object[] {
                null,
                Notification.builder()
                    .reference(new Reference("someref"))
                    .destination(Destination.builder().email("test@test.com").build())
                    .template(Template.builder().emailTemplateId("some.template").build())
                    .build()
            },
            new Object[] {
                Subscription.builder().subscribeSms("Yes").build(),
                Notification.builder()
                    .reference(new Reference("someref"))
                    .destination(Destination.builder().build())
                    .template(Template.builder().emailTemplateId("some.template").build())
                    .build()
            },
            new Object[] {
                Subscription.builder().subscribeSms("Yes").build(),
                Notification.builder()
                    .reference(new Reference("someref"))
                    .destination(Destination.builder().email("test@test.com").build())
                    .template(Template.builder().build())
                    .build()
            }
        };
    }
}
