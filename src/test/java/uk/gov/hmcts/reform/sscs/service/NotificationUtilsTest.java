package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.SscsCaseDataUtils.CASE_ID;
import static uk.gov.hmcts.reform.sscs.SscsCaseDataUtils.addHearing;
import static uk.gov.hmcts.reform.sscs.SscsCaseDataUtils.addHearingOptions;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.YES;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.service.NotificationServiceTest.*;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.*;
import static uk.gov.hmcts.reform.sscs.service.SendNotificationServiceTest.APPELLANT_WITH_ADDRESS_AND_APPOINTEE;

import java.util.Arrays;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.*;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;

@RunWith(JUnitParamsRunner.class)
public class NotificationUtilsTest {
    @Mock
    private NotificationValidService notificationValidService;

    @Before
    public void setup() {
        openMocks(this);
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
        assertFalse(hasAppointee(Appointee.builder().name(Name.builder().lastName("Last").build()).build(), "Yes"));
    }

    @Test
    public void falseWhenNoLastName() {
        assertFalse(hasAppointee(Appointee.builder().name(Name.builder().firstName("First").build()).build(), "Yes"));
    }

    @Test
    public void trueWhenHasFirstAndLastName() {
        assertTrue(hasAppointee(Appointee.builder().name(Name.builder().firstName("First").lastName("Last").build()).build(), "Yes"));
    }

    @Test
    public void falseWhenIsAppointeeIsNo() {
        assertFalse(hasAppointee(Appointee.builder().name(Name.builder().firstName("First").lastName("Last").build()).build(), "No"));
    }

    @Test
    @Parameters({"Yes", "", "null"})
    public void trueWhenIsAppointeeIs(@Nullable String value) {
        assertTrue(hasAppointee(Appointee.builder().name(Name.builder().firstName("First").lastName("Last").build()).build(), value));
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
    public void okToSendSmsNotificationisValid() {
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        CcdNotificationWrapper wrapper = buildBaseWrapper(null, null);

        Subscription subscription = Subscription.builder().subscribeSms("Yes").wantSmsNotifications("Yes").build();
        Notification notification = Notification.builder()
            .reference(new Reference("someref"))
            .destination(Destination.builder().sms("07800123456").build())
            .template(Template.builder().smsTemplateId(Arrays.asList("some.template")).build())
            .build();

        assertTrue(isOkToSendSmsNotification(wrapper, subscription, notification, STRUCK_OUT, notificationValidService));
    }

    @Test
    @Parameters(method = "isNotOkToSendSmsNotificationScenarios")
    public void okToSendSmsNotificationisNotValid(Subscription subscription, Notification notification) {
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        CcdNotificationWrapper wrapper = buildBaseWrapper(null, null);

        assertFalse(isOkToSendSmsNotification(wrapper, subscription, notification, STRUCK_OUT, notificationValidService));
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

    @Test
    public void hasNoAppointeeSubscriptionsIfAppointeeIsNotSubscribed() {
        Subscription subscription = Subscription.builder().wantSmsNotifications("No").subscribeSms("No").subscribeEmail("No").build();
        CcdNotificationWrapper wrapper = buildBaseWrapper(subscription, null);
        wrapper.getSscsCaseDataWrapper().getNewSscsCaseData().setSubscriptions(wrapper.getSscsCaseDataWrapper().getNewSscsCaseData().getSubscriptions().toBuilder().appointeeSubscription(subscription).build());
        assertFalse(hasAppointeeSubscriptionOrIsMandatoryAppointeeLetter(wrapper.getSscsCaseDataWrapper()));
    }

    @Test
    public void hasNoRepresentativeSubscriptionsIfRepresentativeIsNotSubscribed() {
        Subscription subscription = Subscription.builder().wantSmsNotifications("No").subscribeSms("No").subscribeEmail("No").build();
        CcdNotificationWrapper wrapper = buildBaseWrapper(subscription, null);
        wrapper.getSscsCaseDataWrapper().getNewSscsCaseData().setSubscriptions(wrapper.getSscsCaseDataWrapper().getNewSscsCaseData().getSubscriptions().toBuilder().appointeeSubscription(subscription).build());
        assertFalse(hasRepSubscriptionOrIsMandatoryRepLetter(wrapper.getSscsCaseDataWrapper()));
    }

    @Test
    public void shouldReturnFalseWhenThereIsNoJointParty() {
        assertFalse(hasJointParty(buildBaseWrapper(null, null).getNewSscsCaseData()));
    }

    @Test
    public void shouldReturnTrueWhenThereIsAJointParty() {
        assertTrue(hasJointParty(buildJointPartyWrapper(null, null, YES).getNewSscsCaseData()));
    }

    @Test
    public void shouldReturnTrueWhenThereIsAJointPartySubscriptionAndJointPartyIsYes() {
        Subscription subscription = Subscription.builder().subscribeSms(YES).subscribeEmail(YES).build();
        assertTrue(hasJointPartySubscription(buildJointPartyWrapper(subscription, null, YES).getSscsCaseDataWrapper()));
    }

    @Test
    public void shouldReturnFalseWhenThereIsAJointPartySubscriptionAndJointPartyIsNo() {
        Subscription subscription = Subscription.builder().subscribeSms(YES).subscribeEmail(YES).build();
        assertFalse(hasJointPartySubscription(buildJointPartyWrapper(subscription, null, "No").getSscsCaseDataWrapper()));
    }

    @Test
    public void shouldReturnFalseWhenThereIsANoJointPartySubscription() {
        assertFalse(hasJointPartySubscription(buildJointPartyWrapper(null, null, YES).getSscsCaseDataWrapper()));
    }

    @Test
    public void shouldReturntrueWhenThereIsANoJointPartySubscriptionButALetterIsSent() {
        assertTrue(hasJointPartySubscription(buildJointPartyWrapper(null, ISSUE_FINAL_DECISION, YES).getSscsCaseDataWrapper()));
    }

    private Object[] mandatoryNotificationTypes() {
        return new Object[]{
            STRUCK_OUT,
            HEARING_BOOKED_NOTIFICATION,
            DWP_UPLOAD_RESPONSE_NOTIFICATION
        };
    }

    private Object[] nonMandatoryNotificationTypes() {
        return new Object[]{
            ADJOURNED_NOTIFICATION,
            SYA_APPEAL_CREATED_NOTIFICATION,
            RESEND_APPEAL_CREATED_NOTIFICATION,
            APPEAL_DORMANT_NOTIFICATION,
            EVIDENCE_RECEIVED_NOTIFICATION,
            DWP_RESPONSE_RECEIVED_NOTIFICATION,
            POSTPONEMENT_NOTIFICATION,
            SUBSCRIPTION_CREATED_NOTIFICATION,
            SUBSCRIPTION_UPDATED_NOTIFICATION,
            SUBSCRIPTION_OLD_NOTIFICATION,
            EVIDENCE_REMINDER_NOTIFICATION,
            HEARING_REMINDER_NOTIFICATION,
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

    private static CcdNotificationWrapper buildJointPartyWrapper(Subscription subscription, NotificationEventType eventType, String jointParty) {
        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapper(subscription, eventType);
        final SscsCaseData sscsCaseData = ccdNotificationWrapper.getNewSscsCaseData().toBuilder()
                .jointParty(jointParty)
                .jointPartyAddressSameAsAppellant(YES)
                .jointPartyName(JointPartyName.builder().firstName("Joint").lastName("Party").build())
                .subscriptions(Subscriptions.builder().appellantSubscription(subscription).jointPartySubscription(subscription).build())
                .build();
        return new CcdNotificationWrapper(SscsCaseDataWrapper.builder()
                .newSscsCaseData(sscsCaseData)
                .oldSscsCaseData(sscsCaseData)
                .notificationEventType(eventType)
                .build());
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
                    .appellant(Appellant.builder().build())
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
                    .template(Template.builder().smsTemplateId(Arrays.asList("some.template")).build())
                    .build()
            },
            new Object[] {
                Subscription.builder().subscribeSms("Yes").build(),
                Notification.builder()
                    .reference(new Reference("someref"))
                    .destination(Destination.builder().build())
                    .template(Template.builder().smsTemplateId(Arrays.asList("some.template")).build())
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

    @Test
    @Parameters(method = "getLatestHearingScenarios")
    public void getLatestHearingTest(
            SscsCaseData sscsCaseData, String expectedHearingId, String expectedDate, String exptectedTime) {
        Hearing hearing = NotificationUtils.getLatestHearing(sscsCaseData);
        assertEquals(expectedHearingId, hearing.getValue().getHearingId());
        assertEquals(expectedDate, hearing.getValue().getHearingDate());
        assertEquals(exptectedTime, hearing.getValue().getTime());
    }

    @Test
    public void whenGettingLatestHearing_shouldReturnNullIfNoHearings() {
        SscsCaseData sscsCaseData = SscsCaseData.builder().build();
        Hearing hearing = NotificationUtils.getLatestHearing(sscsCaseData);
        assertNull(hearing);
    }

    private Hearing createHearing(String hearingId, String hearingDate, String hearingTime) {
        return Hearing.builder().value(HearingDetails.builder()
                .hearingDate(hearingDate)
                .hearingId(hearingId)
                .time(hearingTime)
                .build()).build();
    }

    public Object[] getLatestHearingScenarios() {
        return new Object[] {
            new Object[] {
                SscsCaseData.builder()
                .hearings(Arrays.asList(
                        createHearing("1", "2019-06-01", "14:00")))
                .build(),
                "1","2019-06-01","14:00"
            },
            new Object[] {
                SscsCaseData.builder()
                .hearings(Arrays.asList(
                        createHearing("1", "2019-06-01", "14:00"),
                        createHearing("1", "2019-06-01", "14:01"),
                        createHearing("2", "2019-06-01", "10:00")))
                .build(),
                "2","2019-06-01","10:00"
            },
            new Object[] {
                SscsCaseData.builder()
                .hearings(Arrays.asList(
                        createHearing("1", "2019-06-01", "10:00"),
                        createHearing("1", "2019-06-01", "14:01"),
                        createHearing("2", "2019-06-02", "14:00")))
                .build(),
                "2","2019-06-02","14:00"
            },
            new Object[] {
                SscsCaseData.builder()
                .hearings(Arrays.asList(
                        createHearing("3", "2019-06-01", "14:00"),
                        createHearing("1", "2019-06-02", "14:01"),
                        createHearing("2", "2019-06-01", "10:00")))
                .build(),
                "3","2019-06-01","14:00"
            },
            new Object[] {
                SscsCaseData.builder()
                .hearings(Arrays.asList(
                        createHearing("1", "2019-06-01", "14:00"),
                        createHearing("4", "2019-06-01", "14:01"),
                        createHearing("1", "2019-06-01", "10:00")))
                .build(),
                "4","2019-06-01","14:01"
            },
            new Object[] {
                SscsCaseData.builder()
                .hearings(Arrays.asList(
                        createHearing("1", "2019-06-01", "14:00"),
                        createHearing("4", "2019-06-01", "14:01"),
                        createHearing("4", "2019-06-01", "13:00"),
                        createHearing("1", "2019-06-01", "10:00")))
                .build(),
                "4","2019-06-01","14:01"
            },
            new Object[]{
                SscsCaseData.builder()
                .hearings(Arrays.asList(
                        createHearing("1", "2019-06-01", "14:00"),
                        createHearing("4", "2019-06-01", "13:00"),
                        createHearing("4", "2019-06-01", "14:01"),
                        createHearing("1", "2019-06-01", "10:00")))
                .build(),
                "4", "2019-06-01", "14:01"
            },
            new Object[]{
                SscsCaseData.builder()
                        .hearings(Arrays.asList(
                                createHearing("1", "2019-05-28", "14:01"),
                                createHearing(null, "2018-05-28", "14:01")))
                        .build(),
                "1", "2019-05-28", "14:01"
            },
            new Object[]{
                SscsCaseData.builder()
                        .hearings(Arrays.asList(
                                createHearing(null, "2019-06-01", "14:00"),
                                createHearing(null, "2019-06-01", "13:00"),
                                createHearing(null, "2019-06-01", "14:01"),
                                createHearing(null, "2019-06-01", "10:00")))
                        .build(),
                null, "2019-06-01", "14:01"
            }
        };
    }
}
