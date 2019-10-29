package uk.gov.hmcts.reform.sscs.personalisation;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.PIP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingType.*;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.*;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.*;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.domain.notify.Link;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.extractor.HearingContactDateExtractor;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.MessageAuthenticationServiceImpl;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;

@RunWith(JUnitParamsRunner.class)
public class PersonalisationTest {

    private static final String CASE_ID = "54321";
    private static final String ADDRESS1 = "HM Courts & Tribunals Service";
    private static final String ADDRESS2 = "Social Security & Child Support Appeals";
    private static final String ADDRESS3 = "Prudential Buildings";
    private static final String ADDRESS4 = "36 Dale Street";
    private static final String CITY = "LIVERPOOL";
    private static final String POSTCODE = "L2 5UZ";
    private static final String PHONE = "0300 999 8888";
    private static final String DATE = "2018-07-01T14:01:18.243";

    @Mock
    private NotificationConfig config;

    @Mock
    private HearingContactDateExtractor hearingContactDateExtractor;

    @Mock
    private MessageAuthenticationServiceImpl macService;

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Mock
    private NotificationDateConverterUtil notificationDateConverterUtil;

    @InjectMocks
    public Personalisation personalisation;

    private Subscriptions subscriptions;

    private Name name;

    private  RegionalProcessingCenter rpc;

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
        when(config.getOnlineHearingLink()).thenReturn("http://link.com");
        when(notificationDateConverterUtil.toEmailDate(LocalDate.now().plusDays(1))).thenReturn("1 January 2018");
        when(notificationDateConverterUtil.toEmailDate(LocalDate.now().plusDays(7))).thenReturn("1 February 2018");
        when(notificationDateConverterUtil.toEmailDate(LocalDate.now().plusDays(56))).thenReturn("1 February 2019");
        when(macService.generateToken(eq("GLSCRR"), any())).thenReturn("ZYX");
        when(hearingContactDateExtractor.extract(any())).thenReturn(Optional.empty());

        rpc = RegionalProcessingCenter.builder()
                .name("LIVERPOOL").address1(ADDRESS1).address2(ADDRESS2).address3(ADDRESS3).address4(ADDRESS4).city(CITY).postcode(POSTCODE).phoneNumber(PHONE).build();

        when(regionalProcessingCenterService.getByScReferenceCode("SC/1234/5")).thenReturn(rpc);

        Subscription appellantSubscription = Subscription.builder()
                .tya("GLSCRR")
                .email("test@email.com")
                .mobile("07983495065")
                .subscribeEmail("Yes")
                .subscribeSms("No")
                .build();

        subscriptions = Subscriptions.builder().appellantSubscription(appellantSubscription).build();
        name = Name.builder().firstName("Harry").lastName("Kane").title("Mr").build();
    }

    @Test
    @Parameters(method = "generateNotificationTypeAndSubscriptionsScenarios")
    public void givenSubscriptionType_shouldGenerateEmailAndSmsTemplateNamesPerSubscription(
            NotificationEventType notificationEventType, SubscriptionType subscriptionType, HearingType hearingType,
            boolean hasEmailTemplate, boolean hasSmsTemplate, boolean hasLetterTemplate, boolean hasDocmosisTemplate) {
        NotificationWrapper notificationWrapper = new CcdNotificationWrapper(SscsCaseDataWrapper.builder()
                .newSscsCaseData(SscsCaseData.builder()
                        .appeal(Appeal.builder()
                                .hearingType(hearingType.name())
                                .build())
                        .build())
                .notificationEventType(notificationEventType)
                .build());

        personalisation.getTemplate(notificationWrapper, PIP, subscriptionType);

        verify(config).getTemplate(eq(hasEmailTemplate ? getExpectedTemplateName(notificationEventType, subscriptionType) : notificationEventType.getId()),
                eq(hasSmsTemplate ? getExpectedTemplateName(notificationEventType, subscriptionType) : notificationEventType.getId()),
                eq(hasLetterTemplate ? getExpectedTemplateName(notificationEventType, subscriptionType) : notificationEventType.getId()),
                eq(hasDocmosisTemplate ? getExpectedTemplateName(notificationEventType, subscriptionType) : notificationEventType.getId()),
                any(Benefit.class), any(AppealHearingType.class), eq(null)
        );
    }

    private String getExpectedTemplateName(NotificationEventType notificationEventType,
                                           SubscriptionType subscriptionType) {
        return notificationEventType.getId() + (subscriptionType == null ? "" :
                "." + StringUtils.lowerCase(subscriptionType.name()));
    }

    @SuppressWarnings({"Indentation", "unused"})
    private Object[] generateNotificationTypeAndSubscriptionsScenarios() {
        return new Object[]{
                new Object[]{APPEAL_RECEIVED_NOTIFICATION, APPELLANT, PAPER, true, true, true, true},
                new Object[]{APPEAL_RECEIVED_NOTIFICATION, APPELLANT, REGULAR, true, true, true, true},
                new Object[]{APPEAL_RECEIVED_NOTIFICATION, APPELLANT, ONLINE, true, true, true, true},
                new Object[]{APPEAL_RECEIVED_NOTIFICATION, REPRESENTATIVE, PAPER, true, true, true, true},
                new Object[]{APPEAL_RECEIVED_NOTIFICATION, REPRESENTATIVE, REGULAR, true, true, true, true},
                new Object[]{APPEAL_RECEIVED_NOTIFICATION, REPRESENTATIVE, ONLINE, true, true, true, true},
                new Object[]{APPEAL_RECEIVED_NOTIFICATION, APPOINTEE, PAPER, true, true, true, true},
                new Object[]{APPEAL_RECEIVED_NOTIFICATION, APPOINTEE, REGULAR, true, true, true, true},
                new Object[]{APPEAL_RECEIVED_NOTIFICATION, APPOINTEE, ONLINE, true, true, true, true},
                new Object[]{APPEAL_LAPSED_NOTIFICATION, APPELLANT, PAPER, true, true, true, false},
                new Object[]{APPEAL_LAPSED_NOTIFICATION, APPELLANT, REGULAR, true, true, true, false},
                new Object[]{APPEAL_LAPSED_NOTIFICATION, APPELLANT, ONLINE, true, true, true, false},
                new Object[]{APPEAL_LAPSED_NOTIFICATION, APPOINTEE, PAPER, true, true, true, false},
                new Object[]{APPEAL_LAPSED_NOTIFICATION, APPOINTEE, REGULAR, true, true, true, false},
                new Object[]{APPEAL_LAPSED_NOTIFICATION, APPOINTEE, ONLINE, true, true, true, false},
                new Object[]{APPEAL_WITHDRAWN_NOTIFICATION, APPELLANT, PAPER, true, true, true, false},
                new Object[]{APPEAL_WITHDRAWN_NOTIFICATION, APPELLANT, REGULAR, true, true, true, false},
                new Object[]{APPEAL_WITHDRAWN_NOTIFICATION, APPELLANT, ONLINE, true, true, true, false},
                new Object[]{APPEAL_WITHDRAWN_NOTIFICATION, REPRESENTATIVE, PAPER, true, true, true, false},
                new Object[]{APPEAL_WITHDRAWN_NOTIFICATION, REPRESENTATIVE, REGULAR, true, true, true, false},
                new Object[]{APPEAL_WITHDRAWN_NOTIFICATION, REPRESENTATIVE, ONLINE, true, true, true, false},
                new Object[]{SYA_APPEAL_CREATED_NOTIFICATION, APPELLANT, PAPER, true, true, true, false},
                new Object[]{SYA_APPEAL_CREATED_NOTIFICATION, APPELLANT, REGULAR, true, true, true, false},
                new Object[]{SYA_APPEAL_CREATED_NOTIFICATION, APPELLANT, ONLINE, true, true, true, false},
                new Object[]{SYA_APPEAL_CREATED_NOTIFICATION, REPRESENTATIVE, PAPER, true, true, true, false},
                new Object[]{SYA_APPEAL_CREATED_NOTIFICATION, REPRESENTATIVE, REGULAR, true, true, true, false},
                new Object[]{SYA_APPEAL_CREATED_NOTIFICATION, REPRESENTATIVE, ONLINE, true, true, true, false},
                new Object[]{SYA_APPEAL_CREATED_NOTIFICATION, APPOINTEE, PAPER, true, true, true, false},
                new Object[]{SYA_APPEAL_CREATED_NOTIFICATION, APPOINTEE, REGULAR, true, true, true, false},
                new Object[]{SYA_APPEAL_CREATED_NOTIFICATION, APPOINTEE, ONLINE, true, true, true, false},
                new Object[]{DWP_RESPONSE_RECEIVED_NOTIFICATION, APPELLANT, ONLINE, true, true, true, false},
                new Object[]{DWP_RESPONSE_RECEIVED_NOTIFICATION, REPRESENTATIVE, ONLINE, true, true, true, false},
                new Object[]{DWP_RESPONSE_RECEIVED_NOTIFICATION, APPOINTEE, ONLINE, true, true, true, false},
                new Object[]{DWP_RESPONSE_RECEIVED_NOTIFICATION, APPELLANT, PAPER, true, true, true, false},
                new Object[]{DWP_RESPONSE_RECEIVED_NOTIFICATION, REPRESENTATIVE, PAPER, true, true, true, false},
                new Object[]{DWP_RESPONSE_RECEIVED_NOTIFICATION, APPOINTEE, PAPER, true, true, true, false},
                new Object[]{DWP_UPLOAD_RESPONSE_NOTIFICATION, APPELLANT, ONLINE, true, true, true, false},
                new Object[]{DWP_UPLOAD_RESPONSE_NOTIFICATION, REPRESENTATIVE, ONLINE, true, true, true, false},
                new Object[]{DWP_UPLOAD_RESPONSE_NOTIFICATION, APPOINTEE, ONLINE, true, true, true, false},
                new Object[]{DWP_UPLOAD_RESPONSE_NOTIFICATION, APPELLANT, PAPER, true, true, true, false},
                new Object[]{DWP_UPLOAD_RESPONSE_NOTIFICATION, REPRESENTATIVE, PAPER, true, true, true, false},
                new Object[]{DWP_UPLOAD_RESPONSE_NOTIFICATION, APPOINTEE, PAPER, true, true, true, false},
                new Object[]{APPEAL_DORMANT_NOTIFICATION, APPELLANT, PAPER, true, true, false, false},
                new Object[]{EVIDENCE_RECEIVED_NOTIFICATION, APPELLANT, PAPER, true, true, true, false},
                new Object[]{EVIDENCE_RECEIVED_NOTIFICATION, APPELLANT, REGULAR, true, true, true, false},
                new Object[]{EVIDENCE_RECEIVED_NOTIFICATION, APPELLANT, ONLINE, true, true, true, false},
                new Object[]{EVIDENCE_RECEIVED_NOTIFICATION, REPRESENTATIVE, PAPER, true, true, true, false},
                new Object[]{EVIDENCE_RECEIVED_NOTIFICATION, REPRESENTATIVE, REGULAR, true, true, true, false},
                new Object[]{EVIDENCE_RECEIVED_NOTIFICATION, REPRESENTATIVE, ONLINE, true, true, true, false},
                new Object[]{RESEND_APPEAL_CREATED_NOTIFICATION, APPELLANT, PAPER, true, true, false, false},
                new Object[]{RESEND_APPEAL_CREATED_NOTIFICATION, APPELLANT, REGULAR, true, true, false, false},
                new Object[]{RESEND_APPEAL_CREATED_NOTIFICATION, APPELLANT, ONLINE, true, true, false, false},
                new Object[]{RESEND_APPEAL_CREATED_NOTIFICATION, REPRESENTATIVE, PAPER, true, true, false, false},
                new Object[]{RESEND_APPEAL_CREATED_NOTIFICATION, REPRESENTATIVE, REGULAR, true, true, false, false},
                new Object[]{RESEND_APPEAL_CREATED_NOTIFICATION, REPRESENTATIVE, ONLINE, true, true, false, false},
                new Object[]{APPEAL_DORMANT_NOTIFICATION, REPRESENTATIVE, PAPER, true, true, false, false},
                new Object[]{APPEAL_DORMANT_NOTIFICATION, APPELLANT, ORAL, true, true, false, false},
                new Object[]{APPEAL_DORMANT_NOTIFICATION, REPRESENTATIVE, ORAL, true, true, false, false},
                new Object[]{ADJOURNED_NOTIFICATION, APPELLANT, PAPER, true, true, false, false},
                new Object[]{ADJOURNED_NOTIFICATION, APPELLANT, REGULAR, true, true, false, false},
                new Object[]{ADJOURNED_NOTIFICATION, APPELLANT, ONLINE, true, true, false, false},
                new Object[]{ADJOURNED_NOTIFICATION, REPRESENTATIVE, PAPER, true, true, false, false},
                new Object[]{ADJOURNED_NOTIFICATION, REPRESENTATIVE, REGULAR, true, true, false, false},
                new Object[]{ADJOURNED_NOTIFICATION, REPRESENTATIVE, ONLINE, true, true, false, false},
                new Object[]{POSTPONEMENT_NOTIFICATION, APPELLANT, PAPER, true, true, false, false},
                new Object[]{POSTPONEMENT_NOTIFICATION, APPELLANT, REGULAR, true, true, false, false},
                new Object[]{POSTPONEMENT_NOTIFICATION, APPELLANT, ONLINE, true, true, false, false},
                new Object[]{POSTPONEMENT_NOTIFICATION, REPRESENTATIVE, PAPER, true, true, false, false},
                new Object[]{POSTPONEMENT_NOTIFICATION, REPRESENTATIVE, REGULAR, true, true, false, false},
                new Object[]{POSTPONEMENT_NOTIFICATION, REPRESENTATIVE, ONLINE, true, true, false, false},
                new Object[]{HEARING_BOOKED_NOTIFICATION, APPELLANT, PAPER, true, true, true, false},
                new Object[]{HEARING_BOOKED_NOTIFICATION, APPELLANT, REGULAR, true, true, true, false},
                new Object[]{HEARING_BOOKED_NOTIFICATION, APPELLANT, ONLINE, true, true, true, false},
                new Object[]{HEARING_BOOKED_NOTIFICATION, REPRESENTATIVE, PAPER, true, true, true, false},
                new Object[]{HEARING_BOOKED_NOTIFICATION, REPRESENTATIVE, REGULAR, true, true, true, false},
                new Object[]{HEARING_BOOKED_NOTIFICATION, REPRESENTATIVE, ONLINE, true, true, true, false},
                new Object[]{DIRECTION_ISSUED, APPELLANT, ONLINE, false, false, false, true},
                new Object[]{DIRECTION_ISSUED, APPOINTEE, ONLINE, false, false, false, true},
                new Object[]{DIRECTION_ISSUED, REPRESENTATIVE, ONLINE, false, false, false, true},
                new Object[]{DECISION_ISSUED, APPELLANT, ONLINE, false, false, false, true},
                new Object[]{DECISION_ISSUED, APPOINTEE, ONLINE, false, false, false, true},
                new Object[]{DECISION_ISSUED, REPRESENTATIVE, ONLINE, false, false, false, true},
                new Object[]{VALID_APPEAL_CREATED, APPELLANT, PAPER, true, true, true, false},
                new Object[]{VALID_APPEAL_CREATED, APPELLANT, REGULAR, true, true, true, false},
                new Object[]{VALID_APPEAL_CREATED, APPELLANT, ONLINE, true, true, true, false},
                new Object[]{VALID_APPEAL_CREATED, REPRESENTATIVE, PAPER, true, true, true, false},
                new Object[]{VALID_APPEAL_CREATED, REPRESENTATIVE, REGULAR, true, true, true, false},
                new Object[]{VALID_APPEAL_CREATED, REPRESENTATIVE, ONLINE, true, true, true, false},
                new Object[]{VALID_APPEAL_CREATED, APPOINTEE, PAPER, true, true, true, false},
                new Object[]{VALID_APPEAL_CREATED, APPOINTEE, REGULAR, true, true, true, false},
                new Object[]{VALID_APPEAL_CREATED, APPOINTEE, ONLINE, true, true, true, false}
        };
    }

    @Test
    @Parameters({
            "PIP,judge\\, doctor and disability expert, Personal Independence Payment",
            "ESA,judge and a doctor, Employment and Support Allowance",
            "UC,judge\\, doctor and disability expert (if applicable), Universal Credit"
    })
    public void customisePersonalisation(String benefitType, String expectedPanelComposition, String
            expectedBenefitDesc) {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(APPEAL_RECEIVED.getCcdType()).build()).build());

        SscsCaseData response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .regionalProcessingCenter(rpc)
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code(benefitType).build())
                        .appellant(Appellant.builder().name(name).build())
                        .build())
                .subscriptions(subscriptions)
                .events(events)
                .build();

        Map<String, String> result = personalisation.create(SscsCaseDataWrapper.builder().newSscsCaseData(response)
                .notificationEventType(APPEAL_RECEIVED_NOTIFICATION).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT));

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy");
        String expectedDecisionPostedReceiveDate = dateFormatter.format(LocalDate.now().plusDays(7));
        assertEquals(expectedDecisionPostedReceiveDate, result.get("decision_posted_receive_date"));

        assertEquals(expectedPanelComposition, result.get(PANEL_COMPOSITION));

        assertEquals(benefitType, result.get(BENEFIT_NAME_ACRONYM_LITERAL));
        assertEquals(expectedBenefitDesc, result.get(BENEFIT_FULL_NAME_LITERAL));
        assertEquals("SC/1234/5", result.get(APPEAL_REF));
        assertEquals("SC/1234/5", result.get(CASE_REFERENCE_ID));
        assertEquals("GLSCRR", result.get(APPEAL_ID));
        assertEquals("Harry Kane", result.get(NAME));
        assertEquals("Harry Kane", result.get(APPELLANT_NAME));
        assertEquals("0300 999 8888", result.get(PHONE_NUMBER));
        assertEquals("http://link.com/manage-email-notifications/ZYX", result.get(MANAGE_EMAILS_LINK_LITERAL));
        assertEquals("http://tyalink.com/GLSCRR", result.get(TRACK_APPEAL_LINK_LITERAL));
        assertEquals(DWP_ACRONYM, result.get(FIRST_TIER_AGENCY_ACRONYM));
        assertEquals(DWP_FUL_NAME, result.get(FIRST_TIER_AGENCY_FULL_NAME));
        assertEquals("5 August 2018", result.get(APPEAL_RESPOND_DATE));
        assertEquals("http://link.com/GLSCRR", result.get(SUBMIT_EVIDENCE_LINK_LITERAL));
        assertEquals("http://link.com/progress/GLSCRR/expenses", result.get(CLAIMING_EXPENSES_LINK_LITERAL));
        assertEquals("http://link.com/progress/GLSCRR/abouthearing", result.get(HEARING_INFO_LINK_LITERAL));
        assertNull(result.get(EVIDENCE_RECEIVED_DATE_LITERAL));

        assertEquals(ADDRESS1, result.get(REGIONAL_OFFICE_NAME_LITERAL));
        assertEquals(ADDRESS2, result.get(SUPPORT_CENTRE_NAME_LITERAL));
        assertEquals(ADDRESS3, result.get(ADDRESS_LINE_LITERAL));
        assertEquals(ADDRESS4, result.get(TOWN_LITERAL));
        assertEquals(CITY, result.get(COUNTY_LITERAL));
        assertEquals(POSTCODE, result.get(POSTCODE_LITERAL));
        assertEquals(CASE_ID, result.get(CCD_ID));
        assertEquals("1 February 2019", result.get(TRIBUNAL_RESPONSE_DATE_LITERAL));
        assertEquals("1 February 2018", result.get(ACCEPT_VIEW_BY_DATE_LITERAL));
        assertEquals("1 January 2018", result.get(QUESTION_ROUND_EXPIRES_DATE_LITERAL));
        assertEquals("", result.get(APPOINTEE_DESCRIPTION));
    }

    @Test
    @Parameters({
        "judge\\, doctor and disability expert (if applicable)"
    })
    public void givenNoBenefitType_customisePersonalisation(String expectedPanelComposition) {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(APPEAL_RECEIVED.getCcdType()).build()).build());

        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .regionalProcessingCenter(rpc)
            .appeal(Appeal.builder()
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .events(events)
            .build();

        Map<String, String> result = personalisation.create(SscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(APPEAL_RECEIVED_NOTIFICATION).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT));

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy");
        String expectedDecisionPostedReceiveDate = dateFormatter.format(LocalDate.now().plusDays(7));
        assertEquals(expectedDecisionPostedReceiveDate, result.get("decision_posted_receive_date"));

        assertEquals(expectedPanelComposition, result.get(PANEL_COMPOSITION));

        assertNull(result.get(BENEFIT_NAME_ACRONYM_LITERAL));
        assertNull(result.get(BENEFIT_FULL_NAME_LITERAL));
        assertEquals("SC/1234/5", result.get(APPEAL_REF));
        assertEquals("SC/1234/5", result.get(CASE_REFERENCE_ID));
        assertEquals("GLSCRR", result.get(APPEAL_ID));
        assertEquals("Harry Kane", result.get(NAME));
        assertEquals("Harry Kane", result.get(APPELLANT_NAME));
        assertEquals("0300 999 8888", result.get(PHONE_NUMBER));
        assertNull(result.get(MANAGE_EMAILS_LINK_LITERAL));
        assertEquals("http://tyalink.com/GLSCRR", result.get(TRACK_APPEAL_LINK_LITERAL));
        assertEquals(DWP_ACRONYM, result.get(FIRST_TIER_AGENCY_ACRONYM));
        assertEquals(DWP_FUL_NAME, result.get(FIRST_TIER_AGENCY_FULL_NAME));
        assertEquals("5 August 2018", result.get(APPEAL_RESPOND_DATE));
        assertEquals("http://link.com/GLSCRR", result.get(SUBMIT_EVIDENCE_LINK_LITERAL));
        assertEquals("http://link.com/progress/GLSCRR/expenses", result.get(CLAIMING_EXPENSES_LINK_LITERAL));
        assertEquals("http://link.com/progress/GLSCRR/abouthearing", result.get(HEARING_INFO_LINK_LITERAL));
        assertNull(result.get(EVIDENCE_RECEIVED_DATE_LITERAL));

        assertEquals(ADDRESS1, result.get(REGIONAL_OFFICE_NAME_LITERAL));
        assertEquals(ADDRESS2, result.get(SUPPORT_CENTRE_NAME_LITERAL));
        assertEquals(ADDRESS3, result.get(ADDRESS_LINE_LITERAL));
        assertEquals(ADDRESS4, result.get(TOWN_LITERAL));
        assertEquals(CITY, result.get(COUNTY_LITERAL));
        assertEquals(POSTCODE, result.get(POSTCODE_LITERAL));
        assertEquals(CASE_ID, result.get(CCD_ID));
        assertEquals("1 February 2019", result.get(TRIBUNAL_RESPONSE_DATE_LITERAL));
        assertEquals("1 February 2018", result.get(ACCEPT_VIEW_BY_DATE_LITERAL));
        assertEquals("1 January 2018", result.get(QUESTION_ROUND_EXPIRES_DATE_LITERAL));
        assertEquals("", result.get(APPOINTEE_DESCRIPTION));
    }

    @Test
    public void givenNoRpc_thenGivePhoneNumberBasedOnSc() {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(APPEAL_RECEIVED.getCcdType()).build()).build());

        SscsCaseData response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .regionalProcessingCenter(null)
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                        .appellant(Appellant.builder().name(name).build())
                        .build())
                .subscriptions(subscriptions)
                .events(events)
                .build();

        Map<String, String> result = personalisation.create(SscsCaseDataWrapper.builder().newSscsCaseData(response)
                .notificationEventType(APPEAL_RECEIVED_NOTIFICATION).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT));

        assertEquals("0300 999 8888", result.get(PHONE_NUMBER));
    }

    @Test
    public void appealRefWillReturnCcdCaseIdWhenCaseReferenceIsNotSet() {
        RegionalProcessingCenter rpc = regionalProcessingCenterService.getByScReferenceCode("SC/1234/5");
        SscsCaseData response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference(null)
                .regionalProcessingCenter(rpc)
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                        .appellant(Appellant.builder().name(name).build())
                        .build())
                .subscriptions(subscriptions)
                .build();

        Map result = personalisation.create(SscsCaseDataWrapper.builder().newSscsCaseData(response)
                .notificationEventType(APPEAL_RECEIVED_NOTIFICATION).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT));

        assertEquals(CASE_ID, result.get(APPEAL_REF));
        assertEquals(CASE_ID, result.get(CASE_REFERENCE_ID));
    }

    @Test
    public void appealRefWillReturnCcdCaseIdWhenCreatedInGapsFromReadyToList() {
        RegionalProcessingCenter rpc = regionalProcessingCenterService.getByScReferenceCode("SC/1234/5");
        SscsCaseData response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .regionalProcessingCenter(rpc)
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                        .appellant(Appellant.builder().name(name).build())
                        .build())
                .subscriptions(subscriptions)
                .createdInGapsFrom("readyToList")
                .build();

        Map result = personalisation.create(SscsCaseDataWrapper.builder().newSscsCaseData(response)
                .notificationEventType(APPEAL_RECEIVED_NOTIFICATION).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT));

        assertEquals(CASE_ID, result.get(APPEAL_REF));
        assertEquals(CASE_ID, result.get(CASE_REFERENCE_ID));
    }

    @Test
    public void appealRefWillReturnCaseReferenceWhenCreatedInGapsFromValidAppeal() {
        RegionalProcessingCenter rpc = regionalProcessingCenterService.getByScReferenceCode("SC/1234/5");
        SscsCaseData response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .regionalProcessingCenter(rpc)
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                        .appellant(Appellant.builder().name(name).build())
                        .build())
                .subscriptions(subscriptions)
                .createdInGapsFrom("validAppeal")
                .build();

        Map result = personalisation.create(SscsCaseDataWrapper.builder().newSscsCaseData(response)
                .notificationEventType(APPEAL_RECEIVED_NOTIFICATION).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT));

        assertEquals("SC/1234/5", result.get(APPEAL_REF));
        assertEquals("SC/1234/5", result.get(CASE_REFERENCE_ID));
    }

    @Test
    public void givenEvidenceReceivedNotification_customisePersonalisation() {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(APPEAL_RECEIVED.getCcdType()).build()).build());

        List<Document> documents = new ArrayList<>();

        Document doc = Document.builder().value(DocumentDetails.builder()
                .dateReceived("2018-07-01")
                .evidenceType("Medical")
                .evidenceProvidedBy("Caseworker").build()).build();

        documents.add(doc);

        Evidence evidence = Evidence.builder().documents(documents).build();

        Subscription appellantSubscription = Subscription.builder()
                .tya("GLSCRR")
                .email("test@email.com")
                .mobile("07983495065")
                .subscribeEmail("Yes")
                .subscribeSms("No")
                .build();

        Subscriptions subscriptions = Subscriptions.builder().appellantSubscription(appellantSubscription).build();

        SscsCaseData response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                        .appellant(Appellant.builder().name(name).build())
                        .build())
                .subscriptions(subscriptions)
                .events(events)
                .evidence(evidence)
                .build();

        Map<String, String> result = personalisation.create(SscsCaseDataWrapper.builder()
                .newSscsCaseData(response).notificationEventType(EVIDENCE_RECEIVED_NOTIFICATION).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT));

        assertEquals("1 July 2018", result.get(EVIDENCE_RECEIVED_DATE_LITERAL));
    }

    @Test
    public void setAppealReceivedEventData() {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(APPEAL_RECEIVED.getCcdType()).build()).build());

        SscsCaseData response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
                .events(events)
                .build();

        Map<String, String> result = personalisation.setEventData(new HashMap<>(), response, APPEAL_RECEIVED_NOTIFICATION);

        assertEquals("5 August 2018", result.get(APPEAL_RESPOND_DATE));
    }

    @Test
    public void givenDigitalCaseWithDateSentToDwp_thenUseCaseSentToDwpDateForAppealRespondDate() {
        SscsCaseData response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
                .createdInGapsFrom("readyToList")
                .dateSentToDwp("2018-07-01")
                .build();

        Map<String, String> result = personalisation.setEventData(new HashMap<>(), response, APPEAL_RECEIVED_NOTIFICATION);

        assertEquals("5 August 2018", result.get(APPEAL_RESPOND_DATE));
    }

    @Test
    public void givenDigitalCaseWithNoDateSentToDwp_thenUseTodaysDateForAppealRespondDate() {
        SscsCaseData response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
                .createdInGapsFrom("readyToList")
                .build();

        Map<String, String> result = personalisation.setEventData(new HashMap<>(), response, APPEAL_RECEIVED_NOTIFICATION);

        assertEquals(LocalDate.now().plusDays(MAX_DWP_RESPONSE_DAYS).format(DateTimeFormatter.ofPattern(RESPONSE_DATE_FORMAT)), result.get(APPEAL_RESPOND_DATE));
    }

    @Test
    public void setJudgeDecisionAppealToProceedEventData() {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(JUDGE_DECISION_APPEAL_TO_PROCEED.getId()).build()).build());

        SscsCaseData response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
                .events(events)
                .build();

        Map<String, String> result = personalisation.setEventData(new HashMap<>(), response, JUDGE_DECISION_APPEAL_TO_PROCEED);

        assertEquals("5 August 2018", result.get(APPEAL_RESPOND_DATE));
    }

    @Test
    public void setTcwDecisionAppealToProceedEventData() {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(TCW_DECISION_APPEAL_TO_PROCEED.getId()).build()).build());

        SscsCaseData response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
                .events(events)
                .build();

        Map<String, String> result = personalisation.setEventData(new HashMap<>(), response, TCW_DECISION_APPEAL_TO_PROCEED);

        assertEquals("5 August 2018", result.get(APPEAL_RESPOND_DATE));
    }

    @Test
    public void setEvidenceReceivedEventData() {
        List<Document> documents = new ArrayList<>();

        Document doc = Document.builder().value(DocumentDetails.builder()
                .dateReceived("2018-07-01")
                .evidenceType("Medical")
                .evidenceProvidedBy("Caseworker").build()).build();

        documents.add(doc);

        Evidence evidence = Evidence.builder().documents(documents).build();

        SscsCaseData response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
                .evidence(evidence)
                .build();

        Map<String, String> result = personalisation.setEvidenceReceivedNotificationData(new HashMap<>(), response, EVIDENCE_RECEIVED_NOTIFICATION);

        assertEquals("1 July 2018", result.get(EVIDENCE_RECEIVED_DATE_LITERAL));
    }

    @Test
    public void setEvidenceReceivedEventDataWhenEvidenceIsEmpty() {
        List<Document> documents = null;

        Evidence evidence = Evidence.builder().documents(documents).build();

        SscsCaseData response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
                .evidence(evidence)
                .build();

        Map<String, String> result = personalisation.setEvidenceReceivedNotificationData(new HashMap<>(), response, EVIDENCE_RECEIVED_NOTIFICATION);

        assertEquals("", result.get(EVIDENCE_RECEIVED_DATE_LITERAL));
    }

    @Test
    @Parameters(method = "generateHearingNotificationTypeAndSubscriptionsScenarios")
    public void givenHearingData_correctlySetTheHearingDetails(NotificationEventType hearingNotificationEventType,
                                                               SubscriptionType subscriptionType) {
        LocalDate hearingDate = LocalDate.now().plusDays(7);

        Hearing hearing = createHearing(hearingDate);

        List<Hearing> hearingList = new ArrayList<>();
        hearingList.add(hearing);

        SscsCaseData response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                        .appellant(Appellant.builder().name(name).build())
                        .build())
                .subscriptions(subscriptions)
                .hearings(hearingList)
                .build();

        Map<String, String> result = personalisation.create(SscsCaseDataWrapper.builder()
                .newSscsCaseData(response).notificationEventType(hearingNotificationEventType).build(),
                new SubscriptionWithType(subscriptions.getAppellantSubscription(), subscriptionType));

        assertEquals(hearingDate.format(DateTimeFormatter.ofPattern(RESPONSE_DATE_FORMAT)), result.get(HEARING_DATE));
        assertEquals("12:00 PM", result.get(HEARING_TIME));
        assertEquals("The venue, 12 The Road Avenue, Village, Aberdeen, Aberdeenshire, AB12 0HN", result.get(VENUE_ADDRESS_LITERAL));
        assertEquals("http://www.googlemaps.com/aberdeenvenue", result.get(VENUE_MAP_LINK_LITERAL));
        assertEquals("in 7 days", result.get(DAYS_TO_HEARING_LITERAL));
    }

    @SuppressWarnings({"Indentation", "unused"})
    private Object[] generateHearingNotificationTypeAndSubscriptionsScenarios() {
        return new Object[]{
                new Object[]{HEARING_BOOKED_NOTIFICATION, APPELLANT},
                new Object[]{HEARING_BOOKED_NOTIFICATION, APPOINTEE},

                new Object[]{HEARING_REMINDER_NOTIFICATION, APPELLANT},
                new Object[]{HEARING_REMINDER_NOTIFICATION, APPOINTEE},
        };
    }

    @Test
    public void givenOnlyOneDayUntilHearing_correctlySetTheDaysToHearingText() {
        LocalDate hearingDate = LocalDate.now().plusDays(1);

        Hearing hearing = createHearing(hearingDate);

        List<Hearing> hearingList = new ArrayList<>();
        hearingList.add(hearing);

        SscsCaseData response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                        .appellant(Appellant.builder().name(name).build())
                        .build())
                .subscriptions(subscriptions)
                .hearings(hearingList)
                .build();

        Map<String, String> result = personalisation.create(SscsCaseDataWrapper.builder().newSscsCaseData(response)
                .notificationEventType(HEARING_BOOKED_NOTIFICATION).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT));

        assertEquals("tomorrow", result.get(DAYS_TO_HEARING_LITERAL));
    }

    @Test
    public void handleNullEventWhenPopulatingEventData() {
        SscsCaseData response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
                .build();

        Map<String, String> result = personalisation.setEventData(new HashMap<>(), response, POSTPONEMENT_NOTIFICATION);

        assertEquals(new HashMap<>(), result);
    }

    @Test
    public void handleEmptyEventsWhenPopulatingEventData() {
        SscsCaseData response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
                .events(new ArrayList())
                .build();

        Map<String, String> result = personalisation.setEventData(new HashMap<>(), response, POSTPONEMENT_NOTIFICATION);

        assertEquals(new HashMap<>(), result);
    }

    @Test
    public void shouldPopulateRegionalProcessingCenterFromCcdCaseIfItsPresent() {
        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder()
                .name("LIVERPOOL").address1(ADDRESS1).address2(ADDRESS2).address3(ADDRESS3).address4(ADDRESS4).city(CITY).postcode(POSTCODE).build();

        SscsCaseData response = SscsCaseData.builder().regionalProcessingCenter(rpc).build();

        Map<String, String> result = personalisation.setEvidenceProcessingAddress(new HashMap<>(), response);

        verify(regionalProcessingCenterService, never()).getByScReferenceCode(anyString());

        assertEquals(ADDRESS1, result.get(REGIONAL_OFFICE_NAME_LITERAL));
        assertEquals(ADDRESS2, result.get(SUPPORT_CENTRE_NAME_LITERAL));
        assertEquals(ADDRESS3, result.get(ADDRESS_LINE_LITERAL));
        assertEquals(ADDRESS4, result.get(TOWN_LITERAL));
        assertEquals(CITY, result.get(COUNTY_LITERAL));
        assertEquals(POSTCODE, result.get(POSTCODE_LITERAL));
    }

    @Test
    public void shouldNotPopulateRegionalProcessingCenterIfRpcCannotBeFound() {

        SscsCaseData response = SscsCaseData.builder().regionalProcessingCenter(null).build();

        when(regionalProcessingCenterService.getByScReferenceCode("SC/1234/5")).thenReturn(null);

        Map<String, String> result = personalisation.setEvidenceProcessingAddress(new HashMap<>(), response);

        verify(regionalProcessingCenterService, never()).getByScReferenceCode(anyString());

        assertNull(result.get(REGIONAL_OFFICE_NAME_LITERAL));
        assertNull(result.get(SUPPORT_CENTRE_NAME_LITERAL));
        assertNull(result.get(ADDRESS_LINE_LITERAL));
        assertNull(result.get(TOWN_LITERAL));
        assertNull(result.get(COUNTY_LITERAL));
        assertNull(result.get(POSTCODE_LITERAL));
    }

    @Test
    public void shouldPopulateHearingContactDateFromCcdCaseIfPresent() {

        SscsCaseDataWrapper wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(SscsCaseData.builder().build()).build();

        ZonedDateTime now = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1528907807), ZoneId.of("UTC"));
        when(hearingContactDateExtractor.extract(wrapper)).thenReturn(Optional.of(now));

        Map<String, String> values = new HashMap<>();
        personalisation.setHearingContactDate(values, wrapper);

        assertEquals("13 June 2018", values.get(HEARING_CONTACT_DATE));
    }

    @Test
    public void shouldNotPopulateHearingContactDateFromCcdCaseIfNotPresent() {

        SscsCaseDataWrapper wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(SscsCaseData.builder().build()).build();

        when(hearingContactDateExtractor.extract(wrapper)).thenReturn(Optional.empty());

        Map<String, String> values = new HashMap<>();
        personalisation.setHearingContactDate(values, wrapper);

        assertFalse(values.containsKey(HEARING_CONTACT_DATE));
    }

    @Test
    public void shouldSetOnlineHearingLink() {
        LocalDate hearingDate = LocalDate.now().plusDays(1);

        Hearing hearing = createHearing(hearingDate);

        List<Hearing> hearingList = new ArrayList<>();
        hearingList.add(hearing);

        SscsCaseData response = createResponseWithHearings(hearingList);

        Map result = personalisation.create(SscsCaseDataWrapper.builder()
                .newSscsCaseData(response)
                .notificationEventType(QUESTION_ROUND_ISSUED_NOTIFICATION)
                .build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT));

        assertEquals("http://link.com/onlineHearing?email=test%40email.com", result.get(ONLINE_HEARING_LINK_LITERAL));
        assertEquals("http://link.com/register?tya=GLSCRR", result.get(ONLINE_HEARING_REGISTER_LINK_LITERAL));
        assertEquals("http://link.com/sign-in", result.get(ONLINE_HEARING_SIGN_IN_LINK_LITERAL));
    }

    @Test
    public void shouldNotSetOnlineHearingLinkIfEmailAddressDoesNotExist() {
        LocalDate hearingDate = LocalDate.now().plusDays(1);

        Hearing hearing = createHearing(hearingDate);

        List<Hearing> hearingList = new ArrayList<>();
        hearingList.add(hearing);

        SscsCaseData response = createResponseWithHearings(hearingList);
        Subscription subscriptionsWithoutEmail = response.getSubscriptions().getAppellantSubscription().toBuilder()
                .email(null).build();
        Subscriptions subscriptions = response.getSubscriptions().toBuilder().appellantSubscription(subscriptionsWithoutEmail).build();
        response.setSubscriptions(subscriptions);

        Map result = personalisation.create(SscsCaseDataWrapper.builder()
                .newSscsCaseData(response)
                .notificationEventType(QUESTION_ROUND_ISSUED_NOTIFICATION)
                .build(), new SubscriptionWithType(subscriptionsWithoutEmail, APPELLANT));

        assertNull(result.get(ONLINE_HEARING_LINK_LITERAL));
    }

    @Test
    public void shouldPopulateAppointeeSubscriptionPersonalisation() {
        final String tyaNumber = "tya";
        Name appointeeName = Name.builder().title("MR").firstName("George").lastName("Appointee").build();
        when(macService.generateToken(tyaNumber, PIP.name())).thenReturn("ZYX");

        final SscsCaseData sscsCaseData = SscsCaseData.builder()
                .ccdCaseId(CASE_ID)
                .caseReference("SC/1234/5")
                .appeal(Appeal.builder()
                        .benefitType(BenefitType.builder()
                                .code(PIP.name())
                                .build())
                        .appellant(Appellant.builder()
                                .name(name)
                                .appointee(Appointee.builder()
                                        .name(appointeeName)
                                        .build())
                                .build())
                        .build())
                .subscriptions(Subscriptions.builder()
                        .appointeeSubscription(Subscription.builder()
                                .tya(tyaNumber)
                                .subscribeEmail("Yes")
                                .email("appointee@example.com")
                                .build())
                        .representativeSubscription(Subscription.builder()
                                .tya("repTya")
                                .subscribeEmail("Yes")
                                .email("rep@example.com")
                                .build())
                        .build())
                .build();

        Map result = personalisation.create(SscsCaseDataWrapper.builder()
                .newSscsCaseData(sscsCaseData)
                .notificationEventType(SUBSCRIPTION_CREATED_NOTIFICATION)
                .build(), new SubscriptionWithType(sscsCaseData.getSubscriptions().getAppointeeSubscription(), APPOINTEE));

        assertNotNull(result);
        assertEquals(CASE_ID, result.get(CCD_ID));
        assertEquals(appointeeName.getFullNameNoTitle(), result.get(NAME));
        assertEquals(name.getFullNameNoTitle(), result.get(APPELLANT_NAME));
        assertEquals(tyaNumber, result.get(APPEAL_ID));
        assertEquals("http://link.com/manage-email-notifications/ZYX", result.get(MANAGE_EMAILS_LINK_LITERAL));
        assertEquals("http://tyalink.com/" + tyaNumber, result.get(TRACK_APPEAL_LINK_LITERAL));
        assertEquals("You are receiving this update as the appointee for Harry Kane.\r\n\r\n", result.get(APPOINTEE_DESCRIPTION));
    }

    @Test
    public void shouldPopulateRepSubscriptionPersonalisation() {
        final String tyaNumber = "tya";
        final String repTyaNumber = "repTya";
        when(macService.generateToken(repTyaNumber, PIP.name())).thenReturn("ZYX");

        final SscsCaseData sscsCaseData = SscsCaseData.builder()
                .ccdCaseId(CASE_ID)
                .caseReference("SC/1234/5")
                .appeal(Appeal.builder()
                        .benefitType(BenefitType.builder()
                                .code(PIP.name())
                                .build())
                        .appellant(Appellant.builder()
                                .name(name)
                                .build())
                        .rep(Representative.builder()
                                .name(name)
                                .build())
                        .build())
                .subscriptions(Subscriptions.builder()
                        .appellantSubscription(Subscription.builder()
                                .tya(tyaNumber)
                                .subscribeEmail("Yes")
                                .email("appointee@example.com")
                                .build())
                        .representativeSubscription(Subscription.builder()
                                .tya(repTyaNumber)
                                .subscribeEmail("Yes")
                                .email("rep@example.com")
                                .build())
                        .build())
                .build();

        Map result = personalisation.create(SscsCaseDataWrapper.builder()
                .newSscsCaseData(sscsCaseData)
                .notificationEventType(SUBSCRIPTION_CREATED_NOTIFICATION)
                .build(),
                new SubscriptionWithType(sscsCaseData.getSubscriptions()
                        .getRepresentativeSubscription(), REPRESENTATIVE));

        assertNotNull(result);
        assertEquals(repTyaNumber, result.get(APPEAL_ID));
        assertEquals("http://link.com/manage-email-notifications/ZYX", result.get(MANAGE_EMAILS_LINK_LITERAL));
        assertEquals("http://tyalink.com/" + repTyaNumber, result.get(TRACK_APPEAL_LINK_LITERAL));
        assertEquals("http://link.com/" + repTyaNumber, result.get(SUBMIT_EVIDENCE_LINK_LITERAL));
        assertEquals("http://link.com/" + repTyaNumber, result.get(SUBMIT_EVIDENCE_INFO_LINK_LITERAL));
    }

    @Test
    public void shouldHandleNoSubscription() {
        when(macService.generateToken(StringUtils.EMPTY, PIP.name())).thenReturn("ZYX");
        SscsCaseData response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference(null)
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                        .appellant(Appellant.builder().name(name).build())
                        .build())
                .build();

        Map result = personalisation.create(SscsCaseDataWrapper.builder().newSscsCaseData(response)
                .notificationEventType(APPEAL_RECEIVED_NOTIFICATION).build(), new SubscriptionWithType(null, APPELLANT));

        assertEquals(CASE_ID, result.get(APPEAL_REF));
    }

    @Test
    public void getLatestInfoRequestDetailWhenNoneProvided() {
        assertNull(Personalisation.getLatestInfoRequestDetail(createResponseWithInfoRequests(null)));
    }

    @Test
    public void getLatestInfoRequestDetailWhenOneProvided() {
        String expected = "Request for information";

        List<AppellantInfoRequest> requests = Collections.singletonList(
            AppellantInfoRequest.builder()
                .id("456")
                .appellantInfo(
                    AppellantInfo.builder()
                        .requestDate("2019-01-09").paragraph(expected)
                        .build()
                )
                .build()
        );

        InfoRequests infoRequests = InfoRequests.builder()
            .appellantInfoRequest(requests)
            .build();
        String latestInfoRequest = Personalisation.getLatestInfoRequestDetail(createResponseWithInfoRequests(infoRequests));
        assertNotNull(latestInfoRequest);
        assertEquals(expected, latestInfoRequest);
    }

    @Test
    public void getLatestInfoRequestDetailWhenMultipleProvided() {
        String expected = "Final request for information";

        List<AppellantInfoRequest> requests = Arrays.asList(
            AppellantInfoRequest.builder()
                .id("123")
                .appellantInfo(
                    AppellantInfo.builder()
                        .requestDate("2019-01-10").paragraph("Please provide the information requested")
                        .build()
                )
                .build(),
            AppellantInfoRequest.builder()
                .id("789")
                .appellantInfo(
                    AppellantInfo.builder()
                        .requestDate("2019-01-11").paragraph(expected)
                        .build()
                )
                .build(),
            AppellantInfoRequest.builder()
                .id("456")
                .appellantInfo(
                    AppellantInfo.builder()
                        .requestDate("2019-01-09").paragraph("Request for information")
                        .build()
                )
                .build()
        );

        InfoRequests infoRequests = InfoRequests.builder()
            .appellantInfoRequest(requests)
            .build();
        String latestInfoRequest = Personalisation.getLatestInfoRequestDetail(createResponseWithInfoRequests(infoRequests));
        assertNotNull(latestInfoRequest);
        assertEquals(expected, latestInfoRequest);
    }

    private Hearing createHearing(LocalDate hearingDate) {
        return Hearing.builder().value(HearingDetails.builder()
                .hearingDate(hearingDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .time("12:00")
                .hearingId("1")
                .venue(Venue.builder()
                        .name("The venue")
                        .address(Address.builder()
                                .line1("12 The Road Avenue")
                                .line2("Village")
                                .town("Aberdeen")
                                .county("Aberdeenshire")
                                .postcode("AB12 0HN").build())
                        .googleMapLink("http://www.googlemaps.com/aberdeenvenue")
                        .build()).build()).build();
    }

    private SscsCaseData createResponseWithHearings(List<Hearing> hearingList) {
        return SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                        .appellant(Appellant.builder().name(name).build())
                        .build())
                .subscriptions(subscriptions)
                .hearings(hearingList)
                .build();
    }

    private SscsCaseData createResponseWithInfoRequests(InfoRequests infoRequests) {
        return SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .infoRequests(infoRequests)
            .build();
    }
}
