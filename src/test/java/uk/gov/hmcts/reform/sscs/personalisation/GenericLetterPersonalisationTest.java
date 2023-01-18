package uk.gov.hmcts.reform.sscs.personalisation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.PIP;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationMappingConstants.IS_OTHER_PARTY;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationMappingConstants.JOINT;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationMappingConstants.NAME;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationMappingConstants.REPRESENTATIVE_NAME;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.OTHER_PARTY;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_RECEIVED;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.JointParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscriptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.config.PersonalisationMappingConstants;
import uk.gov.hmcts.reform.sscs.config.properties.EvidenceProperties;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.domain.notify.Link;
import uk.gov.hmcts.reform.sscs.extractor.HearingContactDateExtractor;
import uk.gov.hmcts.reform.sscs.service.MessageAuthenticationServiceImpl;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;


@RunWith(JUnitParamsRunner.class)
public class GenericLetterPersonalisationTest {

    SscsCaseData data;

    @InjectMocks
    GenericLetterPersonalisation genericLetterPersonalisation;

    private Subscriptions subscriptions;

    @Mock
    private NotificationConfig config;

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Mock
    private EvidenceProperties evidenceProperties;

    @Mock
    private HearingContactDateExtractor hearingContactDateExtractor;

    @Mock
    private NotificationDateConverterUtil notificationDateConverterUtil;

    @Mock
    Personalisation personalisation;

    @Mock
    MessageAuthenticationServiceImpl macService;

    @Before
    public void setup() {
        openMocks(this);

        when(config.getTrackAppealLink()).thenReturn(Link.builder().linkUrl("http://tyalink.com/appeal_id").build());
        when(config.getClaimingExpensesLink()).thenReturn(Link.builder().linkUrl("http://link.com/progress/appeal_id/expenses").build());
        when(config.getEvidenceSubmissionInfoLink()).thenReturn(Link.builder().linkUrl("http://link.com/appeal_id").build());
        when(config.getHearingInfoLink()).thenReturn(Link.builder().linkUrl("http://link.com/progress/appeal_id/abouthearing").build());
        when(config.getOnlineHearingLinkWithEmail()).thenReturn(Link.builder().linkUrl("http://link.com/onlineHearing?email={email}").build());
        when(config.getManageEmailsLink()).thenReturn(Link.builder().linkUrl("http://link.com/manage-email-notifications/mac").build());
        when(regionalProcessingCenterService.getByScReferenceCode("SC/1234/5")).thenReturn(null);
        when(evidenceProperties.getAddress()).thenReturn(new EvidenceProperties.EvidenceAddress());
        when(hearingContactDateExtractor.extract(any())).thenReturn(Optional.empty());

        when(notificationDateConverterUtil.toEmailDate(LocalDate.now().plusDays(1))).thenReturn("1 January 2018");
        when(notificationDateConverterUtil.toEmailDate(LocalDate.now().plusDays(7))).thenReturn("1 February 2018");
        when(notificationDateConverterUtil.toEmailDate(LocalDate.now().plusDays(56))).thenReturn("1 February 2019");

        when(macService.generateToken("GLSCRR", PIP.name())).thenReturn("ZYX");

        Subscription subscription = Subscription.builder()
                .tya("GLSCRR")
                .email("test@email.com")
                .mobile("07983495065")
                .subscribeEmail("No")
                .subscribeSms("No")
                .build();

        subscriptions = Subscriptions.builder().appellantSubscription(subscription)
                .jointPartySubscription(subscription)
                .representativeSubscription(subscription)
                .build();

        data = SscsCaseData.builder()
                .appeal(Appeal.builder()
                        .appellant(Appellant.builder()
                                .name(Name.builder().firstName("Appellant").lastName("Name").title("Mr").build())
                                .address(Address.builder().build())
                                .appointee(Appointee.builder().name(Name.builder().build()).build())
                                .build())
                        .rep(Representative.builder()
                                .name(Name.builder().firstName("Representative").lastName("Name").title("Mr").build())
                                .build())
                        .benefitType(BenefitType.builder().code(PIP.name()).build())
                        .build())
                .otherParties(List.of(new CcdValue<>(OtherParty
                        .builder()
                        .name(Name.builder().firstName("Other").lastName("Party").title("Mr").build())
                        .build())))
                .subscriptions(subscriptions)
                .build();
    }

    @Test
    public void givenSubscriberIsAppellant_shouldReturnCorrectAppellantFields() {
        var wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(data).notificationEventType(APPEAL_RECEIVED).build();

        var subscriptionWithType = new SubscriptionWithType(subscriptions.getAppellantSubscription(),
                APPELLANT,
                data.getAppeal().getAppellant(),
                data.getAppeal().getAppellant().getAppointee());

        var result = genericLetterPersonalisation.create(wrapper, subscriptionWithType);

        Assert.assertEquals("Appellant Name", result.get(NAME));
        Assert.assertEquals("", result.get(JOINT));
    }

    @Test
    public void givenThereIsAJointParty_shouldReturnCorrectJointField() {
        data.setJointParty(JointParty.builder()
                .hasJointParty(YesNo.YES)
                .name(Name.builder().firstName("Joint").lastName("Party").title("Mr").build())
                .build());

        var wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(data).notificationEventType(APPEAL_RECEIVED).build();

        var subscriptionWithType = new SubscriptionWithType(subscriptions.getAppellantSubscription(),
                APPELLANT,
                data.getAppeal().getAppellant(),
                data.getAppeal().getAppellant().getAppointee());

        var result = genericLetterPersonalisation.create(wrapper, subscriptionWithType);

        Assert.assertEquals(JOINT, result.get(JOINT));
    }

    @Test
    public void givenSubscriberIsRepresentative_shouldReturnCorrectRepresentativeFields() {
        var wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(data).notificationEventType(APPEAL_RECEIVED).build();

        var subscriptionWithType = new SubscriptionWithType(subscriptions.getRepresentativeSubscription(),
                REPRESENTATIVE,
                data.getAppeal().getAppellant(),
                data.getAppeal().getRep());

        var result = genericLetterPersonalisation.create(wrapper, subscriptionWithType);

        Assert.assertEquals("Representative Name", result.get(REPRESENTATIVE_NAME));
        Assert.assertEquals("Yes", result.get(PersonalisationMappingConstants.REPRESENTATIVE));
    }

    @Test
    public void givenSubscriberIsOtherParrty_shouldReturnCorrectOtherPartyFields() {
        var wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(data).notificationEventType(APPEAL_RECEIVED).build();

        var subscriptionWithType = new SubscriptionWithType(subscriptions.getRepresentativeSubscription(),
                OTHER_PARTY,
                data.getOtherParties().get(0).getValue(),
                data.getOtherParties().get(0).getValue());

        var result = genericLetterPersonalisation.create(wrapper, subscriptionWithType);

        Assert.assertEquals("Other Party", result.get(REPRESENTATIVE_NAME));
        Assert.assertEquals("Yes", result.get(PersonalisationMappingConstants.REPRESENTATIVE));
        Assert.assertEquals("Yes", result.get(IS_OTHER_PARTY));
    }


}
