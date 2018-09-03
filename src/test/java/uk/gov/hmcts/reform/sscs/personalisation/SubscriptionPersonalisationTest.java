package uk.gov.hmcts.reform.sscs.personalisation;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.PIP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.AppConstants;
import uk.gov.hmcts.reform.sscs.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.Link;
import uk.gov.hmcts.reform.sscs.extractor.HearingContactDateExtractor;
import uk.gov.hmcts.reform.sscs.service.MessageAuthenticationServiceImpl;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;

public class SubscriptionPersonalisationTest {

    Subscription newAppellantSubscription;

    Subscription oldAppellantSubscription;

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

    @InjectMocks
    @Resource
    SubscriptionPersonalisation personalisation;

    private String date = "2018-01-01T14:01:18.243";

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
        when(macService.generateToken("GLSCRR", PIP.name())).thenReturn("ZYX");
        when(hearingContactDateExtractor.extract(any())).thenReturn(Optional.empty());

        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder()
                .name("Venue").address1("HMCTS").address2("The Road").address3("Town").address4("City").city("Birmingham").postcode("B23 1EH").build();

        when(regionalProcessingCenterService.getByScReferenceCode("1234")).thenReturn(rpc);

        newAppellantSubscription = Subscription.builder()
            .tya("GLSCRR").email("test@email.com")
            .mobile("07983495065").subscribeEmail("Yes").subscribeSms("Yes").build();

        oldAppellantSubscription = Subscription.builder()
            .tya("GLSCRR").email("test@email.com")
            .mobile("07983495065").subscribeEmail("No").subscribeSms("No").build();

        newSscsCaseData = SscsCaseData.builder().caseId("54321")
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(Name.builder().firstName("Harry").lastName("Kane").title("Mr").build()).build()).build())
            .caseReference("1234")
            .subscriptions(Subscriptions.builder().appellantSubscription(newAppellantSubscription).build()).notificationType(SUBSCRIPTION_UPDATED).build();

        oldSscsCaseData = SscsCaseData.builder().caseId("54321")
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(Name.builder().firstName("Harry").lastName("Kane").title("Mr").build()).build()).build())
            .caseReference("5432")
            .subscriptions(Subscriptions.builder().appellantSubscription(oldAppellantSubscription).build()).notificationType(SUBSCRIPTION_UPDATED).build();
    }

    @Test
    public void customisePersonalisation() {
        Map<String, String> result = personalisation.create(SscsCaseDataWrapper.builder().newSscsCaseData(newSscsCaseData).oldSscsCaseData(oldSscsCaseData).build());

        assertEquals("PIP", result.get(AppConstants.BENEFIT_NAME_ACRONYM_LITERAL));
        assertEquals("Personal Independence Payment", result.get(AppConstants.BENEFIT_FULL_NAME_LITERAL));
        assertEquals("1234", result.get(AppConstants.APPEAL_REF));
        assertEquals("GLSCRR", result.get(AppConstants.APPEAL_ID));
        assertEquals("Harry Kane", result.get(AppConstants.APPELLANT_NAME));
        assertEquals("01234543225", result.get(AppConstants.PHONE_NUMBER));
        assertEquals("http://link.com/manage-email-notifications/ZYX", result.get(AppConstants.MANAGE_EMAILS_LINK_LITERAL));
        assertEquals("http://tyalink.com/GLSCRR", result.get(AppConstants.TRACK_APPEAL_LINK_LITERAL));
        Assert.assertEquals(AppConstants.DWP_ACRONYM, result.get(AppConstants.FIRST_TIER_AGENCY_ACRONYM));
        Assert.assertEquals(AppConstants.DWP_FUL_NAME, result.get(AppConstants.FIRST_TIER_AGENCY_FULL_NAME));
        assertEquals("http://link.com/GLSCRR", result.get(AppConstants.SUBMIT_EVIDENCE_LINK_LITERAL));
    }

    @Test
    public void customisePersonalisationSetsNotificationTypeToMostRecentWhenNewSubscription() {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(date).type(APPEAL_RECEIVED.getCcdType()).build()).build());
        newSscsCaseData.setEvents(events);

        personalisation.create(SscsCaseDataWrapper.builder().newSscsCaseData(newSscsCaseData).oldSscsCaseData(oldSscsCaseData).build());

        assertEquals(APPEAL_RECEIVED, newSscsCaseData.getNotificationType());
    }

    @Test
    public void customisePersonalisationSetsNotificationTypeToDoNotSendWhenEmailHasNotChanged() {
        newAppellantSubscription.setSubscribeEmail("Yes");
        oldAppellantSubscription.setSubscribeEmail("Yes");

        personalisation.create(SscsCaseDataWrapper.builder().newSscsCaseData(newSscsCaseData).oldSscsCaseData(oldSscsCaseData).build());

        assertEquals(DO_NOT_SEND, newSscsCaseData.getNotificationType());
    }

    @Test
    public void customisePersonalisationShouldLeaveNotificationTypeAsSubscriptionUpdatedWhenEmailHasChanged() {
        newAppellantSubscription.setSubscribeEmail("Yes");
        newAppellantSubscription.setEmail("changed@test.com");
        oldAppellantSubscription.setSubscribeEmail("Yes");

        personalisation.create(SscsCaseDataWrapper.builder().newSscsCaseData(newSscsCaseData).oldSscsCaseData(oldSscsCaseData).build());

        assertEquals(SUBSCRIPTION_UPDATED, newSscsCaseData.getNotificationType());
    }

    @Test
    public void checkSubscriptionCreatedNotificationTypeWhenSmsSubscribedIsFirstSet() {
        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(newSscsCaseData, oldSscsCaseData);

        assertTrue(result);
    }

    @Test
    public void checkSubscriptionCreatedNotificationTypeNotChangedWhenSmsSubscribedIsAlreadySet() {
        oldAppellantSubscription.setSubscribeSms("Yes");

        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(newSscsCaseData, oldSscsCaseData);

        assertFalse(result);
    }

    @Test
    public void checkSubscriptionCreatedNotificationTypeNotChangedWhenSmsSubscribedIsNotSet() {
        newAppellantSubscription.setSubscribeSms("No");

        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(newSscsCaseData, oldSscsCaseData);

        assertFalse(result);
    }

    @Test
    public void emptyOldAppellantSubscriptionReturnsFalseForSubscriptionCreatedNotificationType() {
        oldSscsCaseData.setSubscriptions(Subscriptions.builder().appellantSubscription(null).build());

        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(newSscsCaseData, oldSscsCaseData);

        assertFalse(result);
    }

    @Test
    public void emptyNewAppellantSubscriptionReturnsFalseForSubscriptionCreatedNotificationType() {
        newSscsCaseData.setSubscriptions(Subscriptions.builder().appellantSubscription(null).build());

        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(newSscsCaseData, oldSscsCaseData);

        assertFalse(result);
    }

    @Test
    public void setMostRecentEventTypeNotificationWhenEmailSubscribedIsFirstSet() {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(date).type(APPEAL_RECEIVED.getCcdType()).build()).build());
        newSscsCaseData.setEvents(events);

        assertEquals(APPEAL_RECEIVED, personalisation.setEventTypeNotification(newSscsCaseData, oldSscsCaseData));
    }

    @Test
    public void doNotSendEventTypeNotificationWhenEmailSubscribedIsAlreadySet() {
        oldAppellantSubscription.setSubscribeEmail("Yes");

        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(date).type(APPEAL_RECEIVED.getCcdType()).build()).build());
        newSscsCaseData.setEvents(events);

        assertEquals(DO_NOT_SEND, personalisation.setEventTypeNotification(newSscsCaseData, oldSscsCaseData));
    }

    @Test
    public void doNotUpdateMostRecentEventTypeNotificationWhenEventTypeIsNotKnown() {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(date).type(null).build()).build());
        newSscsCaseData.setEvents(events);

        assertEquals(SUBSCRIPTION_UPDATED, personalisation.setEventTypeNotification(newSscsCaseData, oldSscsCaseData));
    }

    @Test
    public void emptyOldAppellantSubscriptionDoesNotUpdateNotificationType() {
        oldSscsCaseData.setSubscriptions(Subscriptions.builder().appellantSubscription(null).build());

        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(date).type(APPEAL_RECEIVED.getCcdType()).build()).build());
        newSscsCaseData.setEvents(events);

        assertEquals(SUBSCRIPTION_UPDATED, personalisation.setEventTypeNotification(newSscsCaseData, oldSscsCaseData));
    }

    @Test
    public void emptyNewAppellantSubscriptionDoesNotUpdateNotificationType() {
        newSscsCaseData.setSubscriptions(Subscriptions.builder().appellantSubscription(null).build());

        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(date).type(APPEAL_RECEIVED.getCcdType()).build()).build());
        newSscsCaseData.setEvents(events);

        assertEquals(SUBSCRIPTION_UPDATED, personalisation.setEventTypeNotification(newSscsCaseData, oldSscsCaseData));
    }

    @Test
    public void emptyEventsDoesNotUpdateNotificationType() {
        newSscsCaseData.setEvents(new ArrayList<>());

        assertEquals(SUBSCRIPTION_UPDATED, personalisation.setEventTypeNotification(newSscsCaseData, oldSscsCaseData));
    }

    @Test
    public void givenEmailNotChangedAndSubscriptionWasAndStillIsSubscribed_thenReturnTrue() {
        newAppellantSubscription.setSubscribeEmail("Yes");
        oldAppellantSubscription.setSubscribeEmail("Yes");

        assertEquals(true, personalisation.doNotSendEmailUpdatedNotificationWhenEmailNotChanged(newSscsCaseData, oldSscsCaseData));
    }

    @Test
    public void givenEmailChangedAndSubscriptionWasAndStillIsSubscribed_thenReturnFalse() {
        newAppellantSubscription.setSubscribeEmail("Yes");
        newAppellantSubscription.setEmail("changed@test.com");
        oldAppellantSubscription.setSubscribeEmail("Yes");

        assertEquals(false, personalisation.doNotSendEmailUpdatedNotificationWhenEmailNotChanged(newSscsCaseData, oldSscsCaseData));
    }

    @Test
    public void givenEmailChangedAndNowSubscribed_thenReturnFalse() {
        newAppellantSubscription.setSubscribeEmail("Yes");
        newAppellantSubscription.setEmail("changed@test.com");
        oldAppellantSubscription.setSubscribeEmail("No");

        assertEquals(false, personalisation.doNotSendEmailUpdatedNotificationWhenEmailNotChanged(newSscsCaseData, oldSscsCaseData));
    }
}
