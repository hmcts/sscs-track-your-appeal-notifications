package uk.gov.hmcts.reform.sscs.factory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.PIP;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Resource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.Link;
import uk.gov.hmcts.reform.sscs.domain.notify.Notification;
import uk.gov.hmcts.reform.sscs.domain.notify.Template;
import uk.gov.hmcts.reform.sscs.extractor.HearingContactDateExtractor;
import uk.gov.hmcts.reform.sscs.personalisation.Personalisation;
import uk.gov.hmcts.reform.sscs.personalisation.SubscriptionPersonalisation;
import uk.gov.hmcts.reform.sscs.service.MessageAuthenticationServiceImpl;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;

public class NotificationFactoryTest {

    private static final String CASE_ID = "54321";

    private NotificationFactory factory;

    private SscsCaseDataWrapper wrapper;

    private SscsCaseData ccdResponse;

    @Mock
    private PersonalisationFactory personalisationFactory;

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Mock
    private HearingContactDateExtractor hearingContactDateExtractor;

    @Mock
    private NotificationConfig config;

    @InjectMocks
    @Resource
    private Personalisation personalisation;

    @InjectMocks
    @Resource
    private SubscriptionPersonalisation subscriptionPersonalisation;

    @Mock
    private MessageAuthenticationServiceImpl macService;

    private Subscription subscription;

    private String date = "2018-01-01T14:01:18.243";

    @Before
    public void setup() {
        initMocks(this);
        factory = new NotificationFactory(personalisationFactory);

        subscription = Subscription.builder()
                .tya("ABC").email("test@testing.com")
                .mobile("07985858594").subscribeEmail("Yes").subscribeSms("No").build();

        ccdResponse = SscsCaseData.builder().caseId(CASE_ID).caseReference("SC/1234/5").appeal(Appeal.builder()
                .appellant(Appellant.builder().name(Name.builder().firstName("Ronnie").lastName("Scott").title("Mr").build()).build())
                .benefitType(BenefitType.builder().code("PIP").build()).build())
                .subscriptions(Subscriptions.builder().appellantSubscription(subscription).build()).build();

        wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(ccdResponse).notificationEventType(APPEAL_RECEIVED_NOTIFICATION).build();

        when(config.getHmctsPhoneNumber()).thenReturn("01234543225");
        when(config.getManageEmailsLink()).thenReturn(Link.builder().linkUrl("http://manageemails.com/mac").build());
        when(config.getTrackAppealLink()).thenReturn(Link.builder().linkUrl("http://tyalink.com/appeal_id").build());
        when(config.getEvidenceSubmissionInfoLink()).thenReturn(Link.builder().linkUrl("http://link.com/appeal_id").build());
        when(config.getManageEmailsLink()).thenReturn(Link.builder().linkUrl("http://link.com/manage-email-notifications/mac").build());
        when(config.getClaimingExpensesLink()).thenReturn(Link.builder().linkUrl("http://link.com/progress/appeal_id/expenses").build());
        when(config.getHearingInfoLink()).thenReturn(Link.builder().linkUrl("http://link.com/progress/appeal_id/abouthearing").build());
        when(macService.generateToken("ABC", PIP.name())).thenReturn("ZYX");

        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder()
                .name("Venue").address1("HMCTS").address2("The Road").address3("Town").address4("City").city("Birmingham").postcode("B23 1EH").build();
        when(regionalProcessingCenterService.getByScReferenceCode("SC/1234/5")).thenReturn(rpc);
        when(hearingContactDateExtractor.extract(any())).thenReturn(Optional.empty());
    }

    @Test
    public void buildNotificationFromSscsCaseData() {
        when(personalisationFactory.apply(APPEAL_RECEIVED_NOTIFICATION)).thenReturn(personalisation);
        when(config.getTemplate(APPEAL_RECEIVED_NOTIFICATION.getId(), APPEAL_RECEIVED_NOTIFICATION.getId(), PIP)).thenReturn(Template.builder().emailTemplateId("123").smsTemplateId(null).build());
        Notification result = factory.create(new CcdNotificationWrapper(wrapper));

        assertEquals("123", result.getEmailTemplate());
        assertEquals("test@testing.com", result.getEmail());
        assertEquals("ABC", result.getAppealNumber());
    }

    @Test
    public void buildSubscriptionCreatedSmsNotificationFromSscsCaseDataWithSubscriptionUpdatedNotificationAndSmsFirstSubscribed() {
        when(personalisationFactory.apply(SUBSCRIPTION_UPDATED_NOTIFICATION)).thenReturn(subscriptionPersonalisation);
        when(config.getTemplate(SUBSCRIPTION_UPDATED_NOTIFICATION.getId(), SUBSCRIPTION_CREATED_NOTIFICATION.getId(), PIP)).thenReturn(Template.builder().emailTemplateId(null).smsTemplateId("123").build());

        wrapper = SscsCaseDataWrapper.builder()
                .newSscsCaseData(
                    ccdResponse.toBuilder()
                        .subscriptions(Subscriptions.builder().appellantSubscription(subscription.toBuilder().subscribeSms("Yes").subscribeEmail("No").build()).build())
                    .build())
                .oldSscsCaseData(
                    ccdResponse.toBuilder()
                        .subscriptions(Subscriptions.builder().appellantSubscription(subscription.toBuilder().subscribeSms("No").subscribeEmail("No").build()).build())
                    .build())
                .notificationEventType(SUBSCRIPTION_UPDATED_NOTIFICATION)
                .build();

        Notification result = factory.create(new CcdNotificationWrapper(wrapper));

        assertEquals("123", result.getSmsTemplate());
    }

    @Test
    public void buildSubscriptionUpdatedSmsNotificationFromSscsCaseDataWithSubscriptionUpdatedNotificationAndSmsAlreadySubscribed() {
        when(personalisationFactory.apply(SUBSCRIPTION_UPDATED_NOTIFICATION)).thenReturn(subscriptionPersonalisation);
        when(config.getTemplate(SUBSCRIPTION_UPDATED_NOTIFICATION.getId(), SUBSCRIPTION_UPDATED_NOTIFICATION.getId(), PIP)).thenReturn(Template.builder().emailTemplateId(null).smsTemplateId("123").build());

        wrapper = SscsCaseDataWrapper.builder()
                .newSscsCaseData(
                    ccdResponse.toBuilder()
                        .subscriptions(Subscriptions.builder().appellantSubscription(subscription.toBuilder().subscribeSms("Yes").subscribeEmail("No").build()).build())
                    .build())
                .oldSscsCaseData(
                    ccdResponse.toBuilder()
                        .subscriptions(Subscriptions.builder().appellantSubscription(subscription.toBuilder().subscribeSms("Yes").subscribeEmail("Yes").build()).build())
                    .build())
                .notificationEventType(SUBSCRIPTION_UPDATED_NOTIFICATION)
                .build();

        Notification result = factory.create(new CcdNotificationWrapper(wrapper));

        assertEquals("123", result.getSmsTemplate());
    }

    @Test
    public void buildLastNotificationFromSscsCaseDataEventWhenEmailFirstSubscribed() {
        when(personalisationFactory.apply(SUBSCRIPTION_UPDATED_NOTIFICATION)).thenReturn(subscriptionPersonalisation);
        when(config.getTemplate(APPEAL_RECEIVED_NOTIFICATION.getId(), APPEAL_RECEIVED_NOTIFICATION.getId(), PIP)).thenReturn(Template.builder().emailTemplateId("123").smsTemplateId(null).build());

        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(date).type(APPEAL_RECEIVED_NOTIFICATION.getId()).build()).build());

        wrapper = SscsCaseDataWrapper.builder()
                .newSscsCaseData(
                    ccdResponse.toBuilder()
                        .subscriptions(Subscriptions.builder().appellantSubscription(subscription.toBuilder().subscribeSms("Yes").subscribeEmail("Yes").build()).build())
                        .events(events)
                        .build())
                .oldSscsCaseData(
                    ccdResponse.toBuilder()
                        .subscriptions(Subscriptions.builder().appellantSubscription(subscription.toBuilder().subscribeSms("Yes").subscribeEmail("No").build()).build())
                        .build())
                .notificationEventType(SUBSCRIPTION_UPDATED_NOTIFICATION)
                .build();

        Notification result = factory.create(new CcdNotificationWrapper(wrapper));

        assertEquals("123", result.getEmailTemplate());
    }

    @Test
    public void buildLastNotificationFromSscsCaseDataEventWhenSmsFirstSubscribed() {
        when(personalisationFactory.apply(SUBSCRIPTION_UPDATED_NOTIFICATION)).thenReturn(subscriptionPersonalisation);
        when(config.getTemplate(DO_NOT_SEND.getId(), SUBSCRIPTION_CREATED_NOTIFICATION.getId(), PIP)).thenReturn(Template.builder().emailTemplateId(null).smsTemplateId("123").build());

        List<Event> event = new ArrayList<>();
        event.add(Event.builder().value(EventDetails.builder().date(date).type(APPEAL_RECEIVED_NOTIFICATION.getId()).build()).build());

        wrapper = SscsCaseDataWrapper.builder()
                .newSscsCaseData(
                        ccdResponse.toBuilder()
                                .subscriptions(Subscriptions.builder().appellantSubscription(subscription.toBuilder().subscribeSms("Yes").subscribeEmail("Yes").build()).build())
                                .events(event)
                                .build())
                .oldSscsCaseData(
                        ccdResponse.toBuilder()
                                .subscriptions(Subscriptions.builder().appellantSubscription(subscription.toBuilder().subscribeSms("No").subscribeEmail("Yes").build()).build())
                                .build())
                .notificationEventType(SUBSCRIPTION_UPDATED_NOTIFICATION)
                .build();

        Notification result = factory.create(new CcdNotificationWrapper(wrapper));

        assertEquals("123", result.getSmsTemplate());
    }

    @Test
    public void buildNoNotificationFromSscsCaseDataWhenSubscriptionUpdateReceivedWithNoChangeInEmailAddress() {
        when(personalisationFactory.apply(SUBSCRIPTION_UPDATED_NOTIFICATION)).thenReturn(subscriptionPersonalisation);
        when(config.getTemplate(DO_NOT_SEND.getId(), SUBSCRIPTION_CREATED_NOTIFICATION.getId(), PIP)).thenReturn(Template.builder().emailTemplateId(null).smsTemplateId(null).build());

        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(date).type(APPEAL_RECEIVED_NOTIFICATION.getId()).build()).build());

        wrapper = SscsCaseDataWrapper.builder()
            .newSscsCaseData(
                ccdResponse.toBuilder()
                    .subscriptions(Subscriptions.builder().appellantSubscription(subscription.toBuilder().subscribeSms("Yes").subscribeEmail("Yes").build()).build())
                    .events(events)
                    .build())
            .oldSscsCaseData(
                ccdResponse.toBuilder()
                    .subscriptions(Subscriptions.builder().appellantSubscription(subscription.toBuilder().subscribeSms("No").subscribeEmail("Yes").build()).build())
                    .build())
            .notificationEventType(SUBSCRIPTION_UPDATED_NOTIFICATION)
            .build();

        Notification result = factory.create(new CcdNotificationWrapper(wrapper));

        assertNull(result.getEmailTemplate());
    }

    @Test
    public void buildSubscriptionUpdatedNotificationFromSscsCaseDataWhenEmailIsChanged() {
        when(personalisationFactory.apply(SUBSCRIPTION_UPDATED_NOTIFICATION)).thenReturn(subscriptionPersonalisation);
        when(config.getTemplate(SUBSCRIPTION_UPDATED_NOTIFICATION.getId(), SUBSCRIPTION_UPDATED_NOTIFICATION.getId(), PIP)).thenReturn(Template.builder().emailTemplateId("123").smsTemplateId(null).build());

        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(date).type(APPEAL_RECEIVED_NOTIFICATION.getId()).build()).build());

        wrapper = SscsCaseDataWrapper.builder()
            .newSscsCaseData(
                ccdResponse.toBuilder()
                    .subscriptions(Subscriptions.builder().appellantSubscription(subscription.toBuilder().email("changed@testing.com").subscribeSms("Yes").subscribeEmail("Yes").build()).build())
                    .events(events)
                    .build())
            .oldSscsCaseData(
                ccdResponse.toBuilder()
                    .subscriptions(Subscriptions.builder().appellantSubscription(subscription.toBuilder().subscribeSms("Yes").subscribeEmail("Yes").build()).build())
                    .build())
            .notificationEventType(SUBSCRIPTION_UPDATED_NOTIFICATION)
            .build();

        Notification result = factory.create(new CcdNotificationWrapper(wrapper));

        assertEquals("123", result.getEmailTemplate());
    }

    @Test
    public void returnNullIfPersonalisationNotFound() {
        when(personalisationFactory.apply(APPEAL_RECEIVED_NOTIFICATION)).thenReturn(null);
        Notification result = factory.create(new CcdNotificationWrapper(wrapper));

        assertNull(result);
    }
}
