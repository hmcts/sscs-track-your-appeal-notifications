package uk.gov.hmcts.reform.sscs.personalisation;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.PIP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.HEARING_BOOKED;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.time.LocalDate;
import java.util.*;
import javax.annotation.Resource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.AppConstants;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.Link;
import uk.gov.hmcts.reform.sscs.extractor.HearingContactDateExtractor;
import uk.gov.hmcts.reform.sscs.service.MessageAuthenticationServiceImpl;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;

public class SubscriptionPersonalisationTest {

    private static final String DATE = "2018-01-01T14:01:18.243";

    SscsCaseDataWrapper wrapper;

    SscsCaseData newSscsCaseData;

    SscsCaseData oldSscsCaseData;

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Mock
    private HearingContactDateExtractor hearingContactDateExtractor;

    @Mock
    private NotificationConfig config;

    @Mock
    private MessageAuthenticationServiceImpl macService;

    @Mock
    private NotificationDateConverterUtil notificationDateConverterUtil;

    @InjectMocks
    @Resource
    SubscriptionPersonalisation personalisation;

    @Before
    public void setup() {
        initMocks(this);
        when(config.getHmctsPhoneNumber()).thenReturn("01234543225");
        when(config.getManageEmailsLink()).thenReturn(Link.builder().linkUrl("http://manageemails.com/mac").build());
        when(config.getTrackAppealLink()).thenReturn(Link.builder().linkUrl("http://tyalink.com/appeal_id").build());
        when(config.getEvidenceSubmissionInfoLink()).thenReturn(Link.builder().linkUrl("http://link.com/appeal_id").build());
        when(config.getManageEmailsLink()).thenReturn(Link.builder().linkUrl("http://link.com/manage-email-notifications/mac").build());
        when(config.getClaimingExpensesLink()).thenReturn(Link.builder().linkUrl("http://link.com/progress/appeal_id/expenses").build());
        when(config.getHearingInfoLink()).thenReturn(Link.builder().linkUrl("http://link.com/progress/appeal_id/abouthearing").build());
        when(config.getOnlineHearingLinkWithEmail()).thenReturn(Link.builder().linkUrl("http://link.com/onlineHearing?email={email}").build());
        when(notificationDateConverterUtil.toEmailDate(any(LocalDate.class))).thenReturn("1 January 2018");
        when(macService.generateToken("GLSCRR", PIP.name())).thenReturn("ZYX");
        when(hearingContactDateExtractor.extract(any())).thenReturn(Optional.empty());

        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder()
                .name("Venue").address1("HMCTS").address2("The Road").address3("Town").address4("City").city("Birmingham").postcode("B23 1EH").build();

        when(regionalProcessingCenterService.getByScReferenceCode("1234")).thenReturn(rpc);

    }

    @Test
    public void customisePersonalisation() {
        buildNewAndOldCaseData(buildDefaultNewAppeallantSubscription(), buildDefaultOldAppeallantSubscription());
        Map<String, String> result = personalisation.create(wrapper, APPELLANT);

        assertEquals("PIP", result.get(AppConstants.BENEFIT_NAME_ACRONYM_LITERAL));
        assertEquals("Personal Independence Payment", result.get(AppConstants.BENEFIT_FULL_NAME_LITERAL));
        assertEquals("1234", result.get(AppConstants.APPEAL_REF));
        assertEquals("GLSCRR", result.get(AppConstants.APPEAL_ID));
        assertEquals("Harry Kane", result.get(AppConstants.NAME));
        assertEquals("01234543225", result.get(AppConstants.PHONE_NUMBER));
        assertEquals("http://link.com/manage-email-notifications/ZYX", result.get(AppConstants.MANAGE_EMAILS_LINK_LITERAL));
        assertEquals("http://tyalink.com/GLSCRR", result.get(AppConstants.TRACK_APPEAL_LINK_LITERAL));
        assertEquals(AppConstants.DWP_ACRONYM, result.get(AppConstants.FIRST_TIER_AGENCY_ACRONYM));
        assertEquals(AppConstants.DWP_FUL_NAME, result.get(AppConstants.FIRST_TIER_AGENCY_FULL_NAME));
        assertEquals("http://link.com/GLSCRR", result.get(AppConstants.SUBMIT_EVIDENCE_LINK_LITERAL));
    }

    @Test
    public void customisePersonalisationSetsNotificationTypeToMostRecentWhenNewSubscription() {
        buildNewAndOldCaseData(buildDefaultNewAppeallantSubscription(), buildDefaultOldAppeallantSubscription());
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(APPEAL_RECEIVED.getCcdType()).build()).build());
        newSscsCaseData.setEvents(events);

        personalisation.create(wrapper, APPELLANT);

        assertEquals(APPEAL_RECEIVED_NOTIFICATION, wrapper.getNotificationEventType());
    }

    @Test
    public void customisePersonalisationSetsNotificationTypeToDoNotSendWhenEmailHasNotChanged() {
        Subscription newAppellantSubscription = Subscription.builder()
                .tya("GLSCRR").email("test@email.com")
                .mobile("07983495065").subscribeEmail("Yes").subscribeSms("Yes").build();

        Subscription oldAppellantSubscription = Subscription.builder()
                .tya("GLSCRR").email("test@email.com")
                .mobile("07983495065").subscribeEmail("Yes").subscribeSms("No").build();

        buildNewAndOldCaseData(newAppellantSubscription, oldAppellantSubscription);

        personalisation.create(wrapper, APPELLANT);

        assertEquals(DO_NOT_SEND, wrapper.getNotificationEventType());
    }

    @Test
    public void customisePersonalisationShouldLeaveNotificationTypeAsSubscriptionUpdatedWhenEmailHasChanged() {
        Subscription newAppellantSubscription = Subscription.builder()
                .tya("GLSCRR").email("changed@test.com")
                .mobile("07983495065").subscribeEmail("Yes").subscribeSms("Yes").build();

        Subscription oldAppellantSubscription = Subscription.builder()
                .tya("GLSCRR").email("test@email.com")
                .mobile("07983495065").subscribeEmail("Yes").subscribeSms("No").build();

        buildNewAndOldCaseData(newAppellantSubscription, oldAppellantSubscription);

        personalisation.create(wrapper, APPELLANT);

        assertEquals(SUBSCRIPTION_UPDATED_NOTIFICATION, wrapper.getNotificationEventType());
    }

    @Test
    public void checkSubscriptionCreatedNotificationTypeWhenSmsSubscribedIsFirstSet() {
        buildNewAndOldCaseData(buildDefaultNewAppeallantSubscription(), buildDefaultOldAppeallantSubscription());

        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(newSscsCaseData, oldSscsCaseData);

        assertTrue(result);
    }

    @Test
    public void checkSubscriptionCreatedNotificationTypeNotChangedWhenSmsSubscribedIsAlreadySet() {
        Subscription newAppellantSubscription = Subscription.builder()
                .tya("GLSCRR").email("test@email.com")
                .mobile("07983495065").subscribeEmail("Yes").subscribeSms("Yes").build();

        Subscription oldAppellantSubscription = Subscription.builder()
                .tya("GLSCRR").email("test@email.com")
                .mobile("07983495065").subscribeEmail("No").subscribeSms("Yes").build();

        buildNewAndOldCaseData(newAppellantSubscription, oldAppellantSubscription);

        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(newSscsCaseData, oldSscsCaseData);

        assertFalse(result);
    }

    @Test
    public void checkSubscriptionCreatedNotificationTypeNotChangedWhenSmsSubscribedIsNotSet() {
        Subscription newAppellantSubscription = Subscription.builder()
                .tya("GLSCRR").email("test@email.com")
                .mobile("07983495065").subscribeEmail("Yes").subscribeSms("No").build();

        Subscription oldAppellantSubscription = Subscription.builder()
                .tya("GLSCRR").email("test@email.com")
                .mobile("07983495065").subscribeEmail("No").subscribeSms("No").build();

        buildNewAndOldCaseData(newAppellantSubscription, oldAppellantSubscription);

        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(newSscsCaseData, oldSscsCaseData);

        assertFalse(result);
    }

    @Test
    public void emptyOldAppellantSubscriptionReturnsFalseForSubscriptionCreatedNotificationType() {
        buildNewAndOldCaseData(buildDefaultNewAppeallantSubscription(), buildDefaultOldAppeallantSubscription());

        oldSscsCaseData.setSubscriptions(Subscriptions.builder().appellantSubscription(null).build());

        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(newSscsCaseData, oldSscsCaseData);

        assertFalse(result);
    }

    @Test
    public void emptyNewAppellantSubscriptionReturnsFalseForSubscriptionCreatedNotificationType() {
        buildNewAndOldCaseData(buildDefaultNewAppeallantSubscription(), buildDefaultOldAppeallantSubscription());

        newSscsCaseData.setSubscriptions(Subscriptions.builder().appellantSubscription(null).build());

        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(newSscsCaseData, oldSscsCaseData);

        assertFalse(result);
    }

    @Test
    public void setMostRecentNotificationEventTypeNotificationWhenEmailSubscribedIsFirstSet() {
        buildNewAndOldCaseData(buildDefaultNewAppeallantSubscription(), buildDefaultOldAppeallantSubscription());

        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(APPEAL_RECEIVED.getCcdType()).build()).build());
        newSscsCaseData.setEvents(events);

        assertEquals(APPEAL_RECEIVED_NOTIFICATION, personalisation.getNotificationEventTypeNotification(wrapper));
    }

    @Test
    public void doNotSendNotificationEventTypeNotificationWhenEmailSubscribedIsAlreadySet() {
        Subscription newAppellantSubscription = Subscription.builder()
                .tya("GLSCRR").email("test@email.com")
                .mobile("07983495065").subscribeEmail("Yes").subscribeSms("Yes").build();

        Subscription oldAppellantSubscription = Subscription.builder()
                .tya("GLSCRR").email("test@email.com")
                .mobile("07983495065").subscribeEmail("Yes").subscribeSms("No").build();

        buildNewAndOldCaseData(newAppellantSubscription, oldAppellantSubscription);

        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(APPEAL_RECEIVED.getCcdType()).build()).build());
        newSscsCaseData.setEvents(events);

        assertEquals(DO_NOT_SEND, personalisation.getNotificationEventTypeNotification(wrapper));
    }

    @Test
    public void doNotUpdateMostRecentNotificationEventTypeNotificationWhenNotificationEventTypeIsNotKnown() {
        buildNewAndOldCaseData(buildDefaultNewAppeallantSubscription(), buildDefaultOldAppeallantSubscription());

        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(null).build()).build());
        newSscsCaseData.setEvents(events);

        assertEquals(SUBSCRIPTION_UPDATED_NOTIFICATION, personalisation.getNotificationEventTypeNotification(wrapper));
    }

    @Test
    public void emptyOldAppellantSubscriptionDoesNotUpdateNotificationType() {
        buildNewAndOldCaseData(buildDefaultNewAppeallantSubscription(), buildDefaultOldAppeallantSubscription());

        oldSscsCaseData.setSubscriptions(Subscriptions.builder().appellantSubscription(null).build());

        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(APPEAL_RECEIVED.getCcdType()).build()).build());
        newSscsCaseData.setEvents(events);

        assertEquals(SUBSCRIPTION_UPDATED_NOTIFICATION, personalisation.getNotificationEventTypeNotification(wrapper));
    }

    @Test
    public void emptyNewAppellantSubscriptionDoesNotUpdateNotificationType() {
        buildNewAndOldCaseData(buildDefaultNewAppeallantSubscription(), buildDefaultOldAppeallantSubscription());

        newSscsCaseData.setSubscriptions(Subscriptions.builder().appellantSubscription(null).build());

        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(APPEAL_RECEIVED.getCcdType()).build()).build());
        newSscsCaseData.setEvents(events);

        assertEquals(SUBSCRIPTION_UPDATED_NOTIFICATION, personalisation.getNotificationEventTypeNotification(wrapper));
    }

    @Test
    public void emptyEventsDoesNotUpdateNotificationType() {
        buildNewAndOldCaseData(buildDefaultNewAppeallantSubscription(), buildDefaultOldAppeallantSubscription());

        newSscsCaseData.setEvents(new ArrayList<>());

        assertEquals(SUBSCRIPTION_UPDATED_NOTIFICATION, personalisation.getNotificationEventTypeNotification(wrapper));
    }

    @Test
    public void isPaperCase() {
        assertTrue(personalisation.isPaperCase(AppealHearingType.PAPER.name()));
    }

    @Test
    public void isNotPaperCase() {
        Arrays.stream(AppealHearingType.values())
            .filter(aht -> {
                return aht != AppealHearingType.PAPER;
            })
            .forEach(aht -> assertFalse(personalisation.isPaperCase(aht.name())));
    }

    @Test
    public void doNotSendMostRecentNotificationEventTypeIfPaperCase() {
        buildNewAndOldCaseData(buildDefaultNewAppeallantSubscription(), buildDefaultOldAppeallantSubscription());

        wrapper.getNewSscsCaseData().getAppeal().setHearingType(AppealHearingType.PAPER.name());
        wrapper.getOldSscsCaseData().getAppeal().setHearingType(AppealHearingType.PAPER.name());

        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(HEARING_BOOKED.getCcdType()).build()).build());
        newSscsCaseData.setEvents(events);

        assertEquals(SUBSCRIPTION_UPDATED_NOTIFICATION, personalisation.getNotificationEventTypeNotification(wrapper));
    }

    @Test
    public void sendMostRecentNotificationEventTypeIfOralCase() {
        buildNewAndOldCaseData(buildDefaultNewAppeallantSubscription(), buildDefaultOldAppeallantSubscription());

        wrapper.getNewSscsCaseData().getAppeal().setHearingType(AppealHearingType.ORAL.name());
        wrapper.getOldSscsCaseData().getAppeal().setHearingType(AppealHearingType.ORAL.name());

        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(HEARING_BOOKED.getCcdType()).build()).build());
        newSscsCaseData.setEvents(events);

        assertEquals(HEARING_BOOKED_NOTIFICATION, personalisation.getNotificationEventTypeNotification(wrapper));
    }

    @Test
    public void givenEmailNotChangedAndSubscriptionWasAndStillIsSubscribed_thenReturnTrue() {
        Subscription newAppellantSubscription = Subscription.builder()
                .tya("GLSCRR").email("test@email.com")
                .mobile("07983495065").subscribeEmail("Yes").subscribeSms("Yes").build();

        Subscription oldAppellantSubscription = Subscription.builder()
                .tya("GLSCRR").email("test@email.com")
                .mobile("07983495065").subscribeEmail("Yes").subscribeSms("No").build();

        buildNewAndOldCaseData(newAppellantSubscription, oldAppellantSubscription);

        assertEquals(true, personalisation.doNotSendEmailUpdatedNotificationWhenEmailNotChanged(newSscsCaseData, oldSscsCaseData));
    }

    @Test
    public void givenEmailChangedAndSubscriptionWasAndStillIsSubscribed_thenReturnFalse() {
        Subscription newAppellantSubscription = Subscription.builder()
                .tya("GLSCRR").email("changed@test.com")
                .mobile("07983495065").subscribeEmail("Yes").subscribeSms("Yes").build();

        Subscription oldAppellantSubscription = Subscription.builder()
                .tya("GLSCRR").email("test@email.com")
                .mobile("07983495065").subscribeEmail("Yes").subscribeSms("No").build();

        buildNewAndOldCaseData(newAppellantSubscription, oldAppellantSubscription);

        assertEquals(false, personalisation.doNotSendEmailUpdatedNotificationWhenEmailNotChanged(newSscsCaseData, oldSscsCaseData));
    }

    @Test
    public void givenEmailChangedAndNowSubscribed_thenReturnFalse() {
        Subscription newAppellantSubscription = Subscription.builder()
                .tya("GLSCRR").email("changed@test.com")
                .mobile("07983495065").subscribeEmail("Yes").subscribeSms("Yes").build();

        Subscription oldAppellantSubscription = Subscription.builder()
                .tya("GLSCRR").email("test@email.com")
                .mobile("07983495065").subscribeEmail("No").subscribeSms("No").build();

        buildNewAndOldCaseData(newAppellantSubscription, oldAppellantSubscription);

        assertEquals(false, personalisation.doNotSendEmailUpdatedNotificationWhenEmailNotChanged(newSscsCaseData, oldSscsCaseData));
    }

    private Subscription buildDefaultNewAppeallantSubscription() {
        return Subscription.builder()
                .tya("GLSCRR").email("test@email.com")
                .mobile("07983495065").subscribeEmail("Yes").subscribeSms("Yes").build();
    }

    private Subscription buildDefaultOldAppeallantSubscription() {
        return Subscription.builder()
                .tya("GLSCRR").email("test@email.com")
                .mobile("07983495065").subscribeEmail("No").subscribeSms("No").build();
    }

    private void buildNewAndOldCaseData(Subscription newAppellantSubscription, Subscription oldAppellantSubscription) {
        newSscsCaseData = SscsCaseData.builder().ccdCaseId("54321")
                .appeal(Appeal.builder()
                        .benefitType(BenefitType.builder().code("PIP").build())
                        .appellant(Appellant.builder().name(Name.builder().firstName("Harry").lastName("Kane").title("Mr").build()).build()).build())
                .caseReference("1234")
                .subscriptions(Subscriptions.builder().appellantSubscription(newAppellantSubscription).build()).build();

        oldSscsCaseData = SscsCaseData.builder().ccdCaseId("54321")
                .appeal(Appeal.builder()
                        .benefitType(BenefitType.builder().code("PIP").build())
                        .appellant(Appellant.builder().name(Name.builder().firstName("Harry").lastName("Kane").title("Mr").build()).build()).build())
                .caseReference("5432")
                .subscriptions(Subscriptions.builder().appellantSubscription(oldAppellantSubscription).build()).build();

        wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(newSscsCaseData).oldSscsCaseData(oldSscsCaseData).notificationEventType(SUBSCRIPTION_UPDATED_NOTIFICATION).build();
    }
}
