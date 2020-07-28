package uk.gov.hmcts.reform.sscs.personalisation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.PIP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.config.AppealHearingType.ONLINE;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_RECEIVED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.QUESTION_ROUND_ISSUED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.getSubscription;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Event;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscriptions;
import uk.gov.hmcts.reform.sscs.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.domain.notify.Link;
import uk.gov.hmcts.reform.sscs.domain.notify.Template;
import uk.gov.hmcts.reform.sscs.extractor.HearingContactDateExtractor;
import uk.gov.hmcts.reform.sscs.factory.CohNotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.MessageAuthenticationServiceImpl;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.service.coh.QuestionRounds;
import uk.gov.hmcts.reform.sscs.service.coh.QuestionService;

public class CohPersonalisationTest {

    private static final String CASE_ID = "54321";

    @Mock
    private NotificationConfig config;

    @Mock
    private MessageAuthenticationServiceImpl macService;

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Mock
    private HearingContactDateExtractor hearingContactDateExtractor;

    @Mock
    private QuestionService questionService;

    @Mock
    private NotificationDateConverterUtil notificationDateConverterUtil;

    @InjectMocks
    private CohPersonalisation cohPersonalisation;

    @Before
    public void setup() {
        initMocks(this);
        when(config.getManageEmailsLink()).thenReturn(Link.builder().linkUrl("http://manageemails.com/mac").build());
        when(config.getTrackAppealLink()).thenReturn(Link.builder().linkUrl("http://tyalink.com/appeal_id").build());
        when(config.getEvidenceSubmissionInfoLink()).thenReturn(Link.builder().linkUrl("http://link.com/appeal_id").build());
        when(config.getManageEmailsLink()).thenReturn(Link.builder().linkUrl("http://link.com/manage-email-notifications/mac").build());
        when(config.getClaimingExpensesLink()).thenReturn(Link.builder().linkUrl("http://link.com/progress/appeal_id/expenses").build());
        when(config.getHearingInfoLink()).thenReturn(Link.builder().linkUrl("http://link.com/progress/appeal_id/abouthearing").build());
        when(config.getOnlineHearingLinkWithEmail()).thenReturn(Link.builder().linkUrl("http://link.com/onlineHearing?email={email}").build());
        when(macService.generateToken("GLSCRR", PIP.name())).thenReturn("ZYX");

        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder().build();
        when(regionalProcessingCenterService.getByScReferenceCode("SC/1234/5")).thenReturn(rpc);
    }

    @Test
    public void addsQuestionEndDate() {
        Subscription appellantSubscription = Subscription.builder()
                .tya("GLSCRR")
                .email("test@email.com")
                .mobile("07983495065")
                .subscribeEmail("Yes")
                .subscribeSms("No")
                .build();
        Name name = Name.builder().firstName("Harry").lastName("Kane").title("Mr").build();
        Subscriptions subscriptions = Subscriptions.builder().appellantSubscription(appellantSubscription).build();

        String date = "2018-07-01T14:01:18.243";


        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(date).type(APPEAL_RECEIVED.getCcdType()).build()).build());

        SscsCaseData response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                        .appellant(Appellant.builder().name(name).build())
                        .build())
                .subscriptions(subscriptions)
                .events(events)
                .build();

        SscsCaseDataWrapper sscsCaseDataWrapper = SscsCaseDataWrapper.builder().newSscsCaseData(response).notificationEventType(APPEAL_RECEIVED_NOTIFICATION).build();

        String someHearingId = "someHearingId";
        String cohDate = "cohDate";
        String expectedRequiredByDate = "expectedRequiredByDate";

        when(questionService.getQuestionRequiredByDate(someHearingId)).thenReturn(cohDate);
        when(notificationDateConverterUtil.toEmailDate(cohDate)).thenReturn(expectedRequiredByDate);

        Map<String, String> placeholders = cohPersonalisation.create(new CohNotificationWrapper(someHearingId, sscsCaseDataWrapper), getSubscriptionWithType(sscsCaseDataWrapper));

        assertThat(placeholders, hasEntry("questions_end_date", expectedRequiredByDate));
    }

    @Test
    public void setsCorrectTemplatesForFirstQuestionRound() {
        String someHearingId = "someHearingId";
        QuestionRounds questionRounds = mock(QuestionRounds.class);
        when(questionService.getQuestionRounds(someHearingId)).thenReturn(questionRounds);
        when(questionRounds.getCurrentQuestionRound()).thenReturn(1);
        CohNotificationWrapper cohNotificationWrapper = new CohNotificationWrapper(
                someHearingId,
                SscsCaseDataWrapper.builder()
                        .newSscsCaseData(SscsCaseData.builder()
                                .appeal(Appeal.builder().hearingType("cor").build())
                                .languagePreferenceWelsh("No")
                                .build())
                        .notificationEventType(QUESTION_ROUND_ISSUED_NOTIFICATION)
                        .build());
        Template expectedTemplate = Template.builder().build();
        when(config.getTemplate(
                QUESTION_ROUND_ISSUED_NOTIFICATION.getId(),
                QUESTION_ROUND_ISSUED_NOTIFICATION.getId(),
                QUESTION_ROUND_ISSUED_NOTIFICATION.getId(),
                QUESTION_ROUND_ISSUED_NOTIFICATION.getId(),
                Benefit.PIP, ONLINE, null,
                LanguagePreference.ENGLISH))
                .thenReturn(expectedTemplate);

        Template template = cohPersonalisation.getTemplate(cohNotificationWrapper, Benefit.PIP, APPELLANT);
        assertThat(template, is(expectedTemplate));
    }

    @Test
    public void setsCorrectTemplatesForSecondQuestionRound() {
        String someHearingId = "someHearingId";
        QuestionRounds questionRounds = mock(QuestionRounds.class);
        when(questionService.getQuestionRounds(someHearingId)).thenReturn(questionRounds);
        when(questionRounds.getCurrentQuestionRound()).thenReturn(2);
        CohNotificationWrapper cohNotificationWrapper = new CohNotificationWrapper(someHearingId,
                SscsCaseDataWrapper.builder()
                        .newSscsCaseData(
                                SscsCaseData.builder()
                                        .appeal(Appeal.builder().hearingType("cor").build())
                                        .languagePreferenceWelsh("No")
                                        .build()
                        )
                        .build());
        Template expectedTemplate = Template.builder().build();
        when(config.getTemplate("follow_up_question_round_issued", "follow_up_question_round_issued", "follow_up_question_round_issued", "follow_up_question_round_issued", Benefit.PIP, ONLINE, null, LanguagePreference.ENGLISH)).thenReturn(expectedTemplate);

        Template template = cohPersonalisation.getTemplate(cohNotificationWrapper, Benefit.PIP, APPELLANT);
        assertThat(template, is(expectedTemplate));
    }

    private SubscriptionWithType getSubscriptionWithType(SscsCaseDataWrapper sscsCaseDataWrapper) {
        return new SubscriptionWithType(getSubscription(sscsCaseDataWrapper.getNewSscsCaseData(), APPELLANT), APPELLANT);
    }
}