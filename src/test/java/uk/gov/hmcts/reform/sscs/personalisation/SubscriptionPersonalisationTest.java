package uk.gov.hmcts.reform.sscs.personalisation;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.PIP;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.getSubscription;

import java.time.LocalDate;
import java.util.*;
import javax.annotation.Resource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.AppConstants;
import uk.gov.hmcts.reform.sscs.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.domain.notify.Link;
import uk.gov.hmcts.reform.sscs.extractor.HearingContactDateExtractor;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.MessageAuthenticationServiceImpl;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;

public class SubscriptionPersonalisationTest {

    private SscsCaseDataWrapper wrapper;

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

    private static final Subscription NEW_SUBSCRIPTION = Subscription.builder()
            .tya("GLSCRR").email("test@email.com")
            .mobile("07983495065").subscribeEmail("Yes").subscribeSms("Yes").build();

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
        buildNewAndOldCaseData(NEW_SUBSCRIPTION, buildSubscriptionWithNothingSubscribed());
        Map<String, String> result = personalisation.create(wrapper, getSubscriptionWithType(new CcdNotificationWrapper(wrapper)));

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
    public void customisePersonalisationShouldLeaveNotificationTypeAsSubscriptionUpdatedWhenEmailHasChanged() {
        Subscription newAppellantSubscription = Subscription.builder()
                .tya("GLSCRR").email("changed@test.com")
                .mobile("07983495065").subscribeEmail("Yes").subscribeSms("Yes").build();

        Subscription oldSubscription = Subscription.builder()
                .tya("GLSCRR").email("test@email.com")
                .mobile("07983495065").subscribeEmail("Yes").subscribeSms("No").build();

        buildNewAndOldCaseData(newAppellantSubscription, oldSubscription);

        personalisation.create(wrapper, getSubscriptionWithType(new CcdNotificationWrapper(wrapper)));

        assertEquals(SUBSCRIPTION_UPDATED_NOTIFICATION, wrapper.getNotificationEventType());
    }

    @Test
    public void checkSubscriptionCreatedNotificationTypeWhenSmsSubscribedIsFirstSet() {
        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(NEW_SUBSCRIPTION, buildSubscriptionWithNothingSubscribed());

        assertTrue(result);
    }

    @Test
    public void checkSubscriptionCreatedNotificationTypeNotChangedWhenSmsSubscribedIsAlreadySet() {
        Subscription oldSubscription = NEW_SUBSCRIPTION.toBuilder()
                .subscribeEmail("No").subscribeSms("Yes").build();

        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(NEW_SUBSCRIPTION, oldSubscription);

        assertFalse(result);
    }

    @Test
    public void checkSubscriptionCreatedNotificationTypeNotChangedWhenSmsSubscribedIsNotSet() {

        Subscription newSubscription = NEW_SUBSCRIPTION.toBuilder().subscribeSms("No").build();
        Subscription oldSubscription = newSubscription.toBuilder()
               .subscribeEmail("No").subscribeSms("No").build();

        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(newSubscription, oldSubscription);

        assertFalse(result);
    }

    @Test
    public void emptyOldAppellantSubscriptionReturnsFalseForSubscriptionCreatedNotificationType() {
        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(null, buildSubscriptionWithNothingSubscribed());
        assertFalse(result);
    }

    @Test
    public void emptyNewAppellantSubscriptionReturnsFalseForSubscriptionCreatedNotificationType() {

        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(null, buildSubscriptionWithNothingSubscribed());

        assertFalse(result);
    }

    @Test
    public void willUnsetMobileAndSmsIfSubscriptionIsUnchanged() {
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(NEW_SUBSCRIPTION, APPELLANT);
        personalisation.unsetMobileAndEmailIfUnchanged(subscriptionWithType, NEW_SUBSCRIPTION);
        assertEquals(NEW_SUBSCRIPTION.toBuilder().mobile(null).email(null).build(),
                subscriptionWithType.getSubscription());
    }

    @Test
    public void willUnsetEmailIfSubscriptionIfSmsIsSubscribed() {
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(NEW_SUBSCRIPTION, APPELLANT);
        Subscription oldSubscription = NEW_SUBSCRIPTION.toBuilder().subscribeSms("No").build();
        personalisation.unsetMobileAndEmailIfUnchanged(subscriptionWithType, oldSubscription);
        assertEquals(NEW_SUBSCRIPTION.toBuilder().email(null).build(), subscriptionWithType.getSubscription());
    }

    @Test
    public void willUnsetMobileIfSubscriptionIfEmailIsSubscribed() {
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(NEW_SUBSCRIPTION, APPELLANT);
        Subscription oldSubscription = NEW_SUBSCRIPTION.toBuilder().subscribeEmail("No").build();
        personalisation.unsetMobileAndEmailIfUnchanged(subscriptionWithType, oldSubscription);
        assertEquals(NEW_SUBSCRIPTION.toBuilder().mobile(null).build(), subscriptionWithType.getSubscription());
    }

    @Test
    public void willUnsetMobileIfSubscriptionIfEmailIsChanged() {
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(NEW_SUBSCRIPTION, APPELLANT);
        Subscription oldSubscription = NEW_SUBSCRIPTION.toBuilder().email("test2@email.com").build();
        personalisation.unsetMobileAndEmailIfUnchanged(subscriptionWithType, oldSubscription);
        assertEquals(NEW_SUBSCRIPTION.toBuilder().mobile(null).build(), subscriptionWithType.getSubscription());
    }

    @Test
    public void willUnsetEmailIfSubscriptionIfMobileIsChanged() {
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(NEW_SUBSCRIPTION, APPELLANT);
        Subscription oldSubscription = NEW_SUBSCRIPTION.toBuilder().mobile("07983495060").build();
        personalisation.unsetMobileAndEmailIfUnchanged(subscriptionWithType, oldSubscription);
        assertEquals(NEW_SUBSCRIPTION.toBuilder().email(null).build(), subscriptionWithType.getSubscription());
    }


    private Subscription buildSubscriptionWithNothingSubscribed() {
        return NEW_SUBSCRIPTION.toBuilder().subscribeEmail("No").subscribeSms("No").build();
    }

    private SubscriptionWithType getSubscriptionWithType(CcdNotificationWrapper ccdNotificationWrapper) {
        return new SubscriptionWithType(getSubscription(ccdNotificationWrapper.getNewSscsCaseData(), APPELLANT), APPELLANT);
    }

    private void buildNewAndOldCaseData(Subscription newAppellantSubscription, Subscription oldSubscription) {
        SscsCaseData newSscsCaseData = SscsCaseData.builder().ccdCaseId("54321")
                .appeal(Appeal.builder()
                        .benefitType(BenefitType.builder().code("PIP").build())
                        .appellant(Appellant.builder().name(Name.builder().firstName("Harry").lastName("Kane").title("Mr").build()).build()).build())
                .caseReference("1234")
                .subscriptions(Subscriptions.builder().appellantSubscription(newAppellantSubscription).build()).build();

        SscsCaseData oldSscsCaseData = SscsCaseData.builder().ccdCaseId("54321")
                .appeal(Appeal.builder()
                        .benefitType(BenefitType.builder().code("PIP").build())
                        .appellant(Appellant.builder().name(Name.builder().firstName("Harry").lastName("Kane").title("Mr").build()).build()).build())
                .caseReference("5432")
                .subscriptions(Subscriptions.builder().appellantSubscription(oldSubscription).build()).build();

        wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(newSscsCaseData).oldSscsCaseData(oldSscsCaseData).notificationEventType(SUBSCRIPTION_UPDATED_NOTIFICATION).build();
    }
}
