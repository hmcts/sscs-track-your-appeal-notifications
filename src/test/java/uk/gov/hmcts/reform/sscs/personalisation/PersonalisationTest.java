package uk.gov.hmcts.reform.sscs.personalisation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.ESA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.PIP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingType.*;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.ACCEPT_VIEW_BY_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.ONLINE_HEARING_LINK_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.ONLINE_HEARING_REGISTER_LINK_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.ONLINE_HEARING_SIGN_IN_LINK_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.QUESTION_ROUND_EXPIRES_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.TRIBUNAL_RESPONSE_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.*;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.AppConstants;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
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

    private String date = "2018-07-01T14:01:18.243";

    private Subscriptions subscriptions;

    private Name name;

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
        when(config.getOnlineHearingLink()).thenReturn("http://link.com");
        when(notificationDateConverterUtil.toEmailDate(LocalDate.now().plusDays(1))).thenReturn("1 January 2018");
        when(notificationDateConverterUtil.toEmailDate(LocalDate.now().plusDays(7))).thenReturn("1 February 2018");
        when(macService.generateToken("GLSCRR", PIP.name())).thenReturn("ZYX");
        when(macService.generateToken("GLSCRR", ESA.name())).thenReturn("ZYX");
        when(hearingContactDateExtractor.extract(any())).thenReturn(Optional.empty());

        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder()
                .name("LIVERPOOL").address1(ADDRESS1).address2(ADDRESS2).address3(ADDRESS3).address4(ADDRESS4).city(CITY).postcode(POSTCODE).build();

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
            NotificationEventType notificationEventType, SubscriptionType subscriptionType,  HearingType hearingType) {
        NotificationWrapper notificationWrapper = new CcdNotificationWrapper(SscsCaseDataWrapper.builder()
                .newSscsCaseData(SscsCaseData.builder()
                        .appeal(Appeal.builder()
                                .hearingType(hearingType.name())
                                .build())
                        .build())
                .notificationEventType(notificationEventType)
                .build());

        personalisation.getTemplate(notificationWrapper, PIP, subscriptionType);

        verify(config).getTemplate(eq(getExpectedTemplateName(notificationEventType, subscriptionType)),
            anyString(), anyString(), any(Benefit.class), any(AppealHearingType.class)
        );
    }

    private String getExpectedTemplateName(NotificationEventType notificationEventType,
                                           SubscriptionType subscriptionType) {
        return notificationEventType.getId() + (subscriptionType == null ? "" :
                "." + subscriptionType.name().toLowerCase());
    }

    @SuppressWarnings("Indentation")
    private Object[] generateNotificationTypeAndSubscriptionsScenarios() {
        return new Object[]{
                new Object[]{APPEAL_RECEIVED_NOTIFICATION, APPELLANT, PAPER},
                new Object[]{APPEAL_RECEIVED_NOTIFICATION, APPELLANT, REGULAR},
                new Object[]{APPEAL_RECEIVED_NOTIFICATION, APPELLANT, ONLINE},
                new Object[]{APPEAL_RECEIVED_NOTIFICATION, REPRESENTATIVE, PAPER},
                new Object[]{APPEAL_RECEIVED_NOTIFICATION, REPRESENTATIVE, REGULAR},
                new Object[]{APPEAL_RECEIVED_NOTIFICATION, REPRESENTATIVE, ONLINE},
                new Object[]{APPEAL_LAPSED_NOTIFICATION, APPELLANT, PAPER},
                new Object[]{APPEAL_LAPSED_NOTIFICATION, APPELLANT, REGULAR},
                new Object[]{APPEAL_LAPSED_NOTIFICATION, APPELLANT, ONLINE},
                new Object[]{APPEAL_LAPSED_NOTIFICATION, REPRESENTATIVE, PAPER},
                new Object[]{APPEAL_LAPSED_NOTIFICATION, REPRESENTATIVE, REGULAR},
                new Object[]{APPEAL_LAPSED_NOTIFICATION, REPRESENTATIVE, ONLINE},
                new Object[]{APPEAL_WITHDRAWN_NOTIFICATION, APPELLANT, PAPER},
                new Object[]{APPEAL_WITHDRAWN_NOTIFICATION, APPELLANT, REGULAR},
                new Object[]{APPEAL_WITHDRAWN_NOTIFICATION, APPELLANT, ONLINE},
                new Object[]{APPEAL_WITHDRAWN_NOTIFICATION, REPRESENTATIVE, PAPER},
                new Object[]{APPEAL_WITHDRAWN_NOTIFICATION, REPRESENTATIVE, REGULAR},
                new Object[]{APPEAL_WITHDRAWN_NOTIFICATION, REPRESENTATIVE, ONLINE},
                new Object[]{SYA_APPEAL_CREATED_NOTIFICATION, APPELLANT, PAPER},
                new Object[]{SYA_APPEAL_CREATED_NOTIFICATION, APPELLANT, REGULAR},
                new Object[]{SYA_APPEAL_CREATED_NOTIFICATION, APPELLANT, ONLINE},
                new Object[]{SYA_APPEAL_CREATED_NOTIFICATION, REPRESENTATIVE, PAPER},
                new Object[]{SYA_APPEAL_CREATED_NOTIFICATION, REPRESENTATIVE, REGULAR},
                new Object[]{SYA_APPEAL_CREATED_NOTIFICATION, REPRESENTATIVE, ONLINE},
                new Object[]{SYA_APPEAL_CREATED_NOTIFICATION, APPOINTEE, PAPER},
                new Object[]{SYA_APPEAL_CREATED_NOTIFICATION, APPOINTEE, REGULAR},
                new Object[]{SYA_APPEAL_CREATED_NOTIFICATION, APPOINTEE, ONLINE},
                new Object[]{APPEAL_DORMANT_NOTIFICATION, APPELLANT, PAPER},
                new Object[]{EVIDENCE_RECEIVED_NOTIFICATION, APPELLANT, PAPER},
                new Object[]{EVIDENCE_RECEIVED_NOTIFICATION, APPELLANT, REGULAR},
                new Object[]{EVIDENCE_RECEIVED_NOTIFICATION, APPELLANT, ONLINE},
                new Object[]{EVIDENCE_RECEIVED_NOTIFICATION, REPRESENTATIVE, PAPER},
                new Object[]{EVIDENCE_RECEIVED_NOTIFICATION, REPRESENTATIVE, REGULAR},
                new Object[]{EVIDENCE_RECEIVED_NOTIFICATION, REPRESENTATIVE, ONLINE},
                new Object[]{RESEND_APPEAL_CREATED_NOTIFICATION, APPELLANT, PAPER},
                new Object[]{RESEND_APPEAL_CREATED_NOTIFICATION, APPELLANT, REGULAR},
                new Object[]{RESEND_APPEAL_CREATED_NOTIFICATION, APPELLANT, ONLINE},
                new Object[]{RESEND_APPEAL_CREATED_NOTIFICATION, REPRESENTATIVE, PAPER},
                new Object[]{RESEND_APPEAL_CREATED_NOTIFICATION, REPRESENTATIVE, REGULAR},
                new Object[]{RESEND_APPEAL_CREATED_NOTIFICATION, REPRESENTATIVE, ONLINE},
                new Object[]{APPEAL_DORMANT_NOTIFICATION, REPRESENTATIVE, PAPER},
                new Object[]{APPEAL_DORMANT_NOTIFICATION, APPELLANT, ORAL},
                new Object[]{APPEAL_DORMANT_NOTIFICATION, REPRESENTATIVE, ORAL},
                new Object[]{ADJOURNED_NOTIFICATION, APPELLANT, PAPER},
                new Object[]{ADJOURNED_NOTIFICATION, APPELLANT, REGULAR},
                new Object[]{ADJOURNED_NOTIFICATION, APPELLANT, ONLINE},
                new Object[]{ADJOURNED_NOTIFICATION, REPRESENTATIVE, PAPER},
                new Object[]{ADJOURNED_NOTIFICATION, REPRESENTATIVE, REGULAR},
                new Object[]{ADJOURNED_NOTIFICATION, REPRESENTATIVE, ONLINE},
                new Object[]{POSTPONEMENT_NOTIFICATION, APPELLANT, PAPER},
                new Object[]{POSTPONEMENT_NOTIFICATION, APPELLANT, REGULAR},
                new Object[]{POSTPONEMENT_NOTIFICATION, APPELLANT, ONLINE},
                new Object[]{POSTPONEMENT_NOTIFICATION, REPRESENTATIVE, PAPER},
                new Object[]{POSTPONEMENT_NOTIFICATION, REPRESENTATIVE, REGULAR},
                new Object[]{POSTPONEMENT_NOTIFICATION, REPRESENTATIVE, ONLINE},
                new Object[]{HEARING_BOOKED_NOTIFICATION, APPELLANT, PAPER},
                new Object[]{HEARING_BOOKED_NOTIFICATION, APPELLANT, REGULAR},
                new Object[]{HEARING_BOOKED_NOTIFICATION, APPELLANT, ONLINE},
                new Object[]{HEARING_BOOKED_NOTIFICATION, REPRESENTATIVE, PAPER},
                new Object[]{HEARING_BOOKED_NOTIFICATION, REPRESENTATIVE, REGULAR},
                new Object[]{HEARING_BOOKED_NOTIFICATION, REPRESENTATIVE, ONLINE}
        };
    }

    @Test
    @Parameters({
            "PIP,judge\\, doctor and disability expert, Personal Independence Payment",
            "ESA,judge and a doctor, Employment and Support Allowance"
    })
    public void customisePersonalisation(String benefitType, String expectedPanelComposition, String
            expectedBenefitDesc) {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(date).type(APPEAL_RECEIVED.getCcdType()).build()).build());

        SscsCaseData response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code(benefitType).build())
                        .appellant(Appellant.builder().name(name).build())
                        .build())
                .subscriptions(subscriptions)
                .events(events)
                .build();

        Map<String, String> result = personalisation.create(SscsCaseDataWrapper.builder().newSscsCaseData(response).notificationEventType(APPEAL_RECEIVED_NOTIFICATION).build());

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy");
        String expectedDecisionPostedReceiveDate = dateFormatter.format(LocalDate.now().plusDays(7));
        assertEquals(expectedDecisionPostedReceiveDate, result.get("decision_posted_receive_date"));

        assertEquals(expectedPanelComposition, result.get(AppConstants.PANEL_COMPOSITION));

        assertEquals(benefitType, result.get(AppConstants.BENEFIT_NAME_ACRONYM_LITERAL));
        assertEquals(expectedBenefitDesc, result.get(AppConstants.BENEFIT_FULL_NAME_LITERAL));
        assertEquals("SC/1234/5", result.get(AppConstants.APPEAL_REF));
        assertEquals("GLSCRR", result.get(AppConstants.APPEAL_ID));
        assertEquals("Harry Kane", result.get(AppConstants.APPELLANT_NAME));
        assertEquals("01234543225", result.get(AppConstants.PHONE_NUMBER));
        assertEquals("http://link.com/manage-email-notifications/ZYX", result.get(AppConstants.MANAGE_EMAILS_LINK_LITERAL));
        assertEquals("http://tyalink.com/GLSCRR", result.get(AppConstants.TRACK_APPEAL_LINK_LITERAL));
        Assert.assertEquals(AppConstants.DWP_ACRONYM, result.get(AppConstants.FIRST_TIER_AGENCY_ACRONYM));
        Assert.assertEquals(AppConstants.DWP_FUL_NAME, result.get(AppConstants.FIRST_TIER_AGENCY_FULL_NAME));
        assertEquals("5 August 2018", result.get(AppConstants.APPEAL_RESPOND_DATE));
        assertEquals("http://link.com/GLSCRR", result.get(AppConstants.SUBMIT_EVIDENCE_LINK_LITERAL));
        assertEquals("http://link.com/progress/GLSCRR/expenses", result.get(AppConstants.CLAIMING_EXPENSES_LINK_LITERAL));
        assertEquals("http://link.com/progress/GLSCRR/abouthearing", result.get(AppConstants.HEARING_INFO_LINK_LITERAL));
        assertNull(result.get(AppConstants.EVIDENCE_RECEIVED_DATE_LITERAL));

        assertEquals(ADDRESS1, result.get(AppConstants.REGIONAL_OFFICE_NAME_LITERAL));
        assertEquals(ADDRESS2, result.get(AppConstants.SUPPORT_CENTRE_NAME_LITERAL));
        assertEquals(ADDRESS3, result.get(AppConstants.ADDRESS_LINE_LITERAL));
        assertEquals(ADDRESS4, result.get(AppConstants.TOWN_LITERAL));
        assertEquals(CITY, result.get(AppConstants.COUNTY_LITERAL));
        assertEquals(POSTCODE, result.get(AppConstants.POSTCODE_LITERAL));
        assertEquals("1 February 2018", result.get(TRIBUNAL_RESPONSE_DATE_LITERAL));
        assertEquals("1 February 2018", result.get(ACCEPT_VIEW_BY_DATE_LITERAL));
        assertEquals("1 January 2018", result.get(QUESTION_ROUND_EXPIRES_DATE_LITERAL));
    }


    @Test
    public void givenEvidenceReceivedNotification_customisePersonalisation() {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(date).type(APPEAL_RECEIVED.getCcdType()).build()).build());

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

        Map<String, String> result = personalisation.create(SscsCaseDataWrapper.builder().newSscsCaseData(response).notificationEventType(EVIDENCE_RECEIVED_NOTIFICATION).build());

        assertEquals("1 July 2018", result.get(AppConstants.EVIDENCE_RECEIVED_DATE_LITERAL));
    }

    @Test
    public void setAppealReceivedEventData() {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(date).type(APPEAL_RECEIVED.getCcdType()).build()).build());

        SscsCaseData response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
                .events(events)
                .build();

        Map<String, String> result = personalisation.setEventData(new HashMap<>(), response, APPEAL_RECEIVED_NOTIFICATION);

        assertEquals("5 August 2018", result.get(AppConstants.APPEAL_RESPOND_DATE));
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

        assertEquals("1 July 2018", result.get(AppConstants.EVIDENCE_RECEIVED_DATE_LITERAL));
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

        assertEquals("", result.get(AppConstants.EVIDENCE_RECEIVED_DATE_LITERAL));
    }

    @Test
    public void givenHearingData_correctlySetTheHearingDetails() {
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

        Map<String, String> result = personalisation.create(SscsCaseDataWrapper.builder().newSscsCaseData(response).notificationEventType(HEARING_BOOKED_NOTIFICATION).build());

        assertEquals(hearingDate.format(DateTimeFormatter.ofPattern(AppConstants.RESPONSE_DATE_FORMAT)), result.get(AppConstants.HEARING_DATE));
        assertEquals("12:00 PM", result.get(AppConstants.HEARING_TIME));
        assertEquals("The venue, 12 The Road Avenue, Village, Aberdeen, Aberdeenshire, AB12 0HN", result.get(AppConstants.VENUE_ADDRESS_LITERAL));
        assertEquals("http://www.googlemaps.com/aberdeenvenue", result.get(AppConstants.VENUE_MAP_LINK_LITERAL));
        assertEquals("in 7 days", result.get(AppConstants.DAYS_TO_HEARING_LITERAL));
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

        Map<String, String> result = personalisation.create(SscsCaseDataWrapper.builder().newSscsCaseData(response).notificationEventType(HEARING_BOOKED_NOTIFICATION).build());

        assertEquals("tomorrow", result.get(AppConstants.DAYS_TO_HEARING_LITERAL));
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

        assertEquals(ADDRESS1, result.get(AppConstants.REGIONAL_OFFICE_NAME_LITERAL));
        assertEquals(ADDRESS2, result.get(AppConstants.SUPPORT_CENTRE_NAME_LITERAL));
        assertEquals(ADDRESS3, result.get(AppConstants.ADDRESS_LINE_LITERAL));
        assertEquals(ADDRESS4, result.get(AppConstants.TOWN_LITERAL));
        assertEquals(CITY, result.get(AppConstants.COUNTY_LITERAL));
        assertEquals(POSTCODE, result.get(AppConstants.POSTCODE_LITERAL));
    }

    @Test
    public void shouldPopulateHearingContactDateFromCcdCaseIfPresent() {

        SscsCaseDataWrapper wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(SscsCaseData.builder().build()).build();

        ZonedDateTime now = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1528907807), ZoneId.of("UTC"));
        when(hearingContactDateExtractor.extract(wrapper)).thenReturn(Optional.of(now));

        Map<String, String> values = new HashMap<>();
        personalisation.setHearingContactDate(values, wrapper);

        assertEquals("13 June 2018", values.get(AppConstants.HEARING_CONTACT_DATE));
    }

    @Test
    public void shouldNotPopulateHearingContactDateFromCcdCaseIfNotPresent() {

        SscsCaseDataWrapper wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(SscsCaseData.builder().build()).build();

        when(hearingContactDateExtractor.extract(wrapper)).thenReturn(Optional.empty());

        Map<String, String> values = new HashMap<>();
        personalisation.setHearingContactDate(values, wrapper);

        assertFalse(values.containsKey(AppConstants.HEARING_CONTACT_DATE));
    }

    @Test
    public void shouldSetOnlineHearingLink() {
        LocalDate hearingDate = LocalDate.now().plusDays(1);

        Hearing hearing = createHearing(hearingDate);

        List<Hearing> hearingList = new ArrayList<>();
        hearingList.add(hearing);

        SscsCaseData response = createResponse(hearingList);

        Map result = personalisation.create(SscsCaseDataWrapper.builder()
                .newSscsCaseData(response)
                .notificationEventType(NotificationEventType.QUESTION_ROUND_ISSUED_NOTIFICATION)
                .build());

        assertEquals("http://link.com/onlineHearing?email=test%40email.com", result.get(ONLINE_HEARING_LINK_LITERAL));
        assertEquals("http://link.com/register", result.get(ONLINE_HEARING_REGISTER_LINK_LITERAL));
        assertEquals("http://link.com/sign-in", result.get(ONLINE_HEARING_SIGN_IN_LINK_LITERAL));
    }

    @Test
    public void shouldNotSetOnlineHearingLinkIfEmailAddressDoesNotExist() {
        LocalDate hearingDate = LocalDate.now().plusDays(1);

        Hearing hearing = createHearing(hearingDate);

        List<Hearing> hearingList = new ArrayList<>();
        hearingList.add(hearing);

        SscsCaseData response = createResponse(hearingList);
        Subscription subscriptionsWithoutEmail = response.getSubscriptions().getAppellantSubscription().toBuilder()
                .email(null).build();
        Subscriptions subscriptions = response.getSubscriptions().toBuilder().appellantSubscription(subscriptionsWithoutEmail).build();
        response.setSubscriptions(subscriptions);

        Map result = personalisation.create(SscsCaseDataWrapper.builder()
                .newSscsCaseData(response)
                .notificationEventType(NotificationEventType.QUESTION_ROUND_ISSUED_NOTIFICATION)
                .build());

        assertNull(result.get(ONLINE_HEARING_LINK_LITERAL));
    }

    @Test
    public void shouldPopulateAppointeeSubscriptionPersonalisation() {
        final String tyaNumber = "tya";
        when(macService.generateToken(tyaNumber, PIP.name())).thenReturn("ZYX");
        final SscsCaseData sscsCaseData = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code(PIP.name()).build())
                        .appellant(Appellant.builder().name(name)
                            .appointee(Appointee.builder().name(Name.builder().build()).build())
                            .build())
                        .build())
                .subscriptions(Subscriptions.builder().appointeeSubscription(Subscription.builder()
                        .tya(tyaNumber)
                        .subscribeEmail("Yes")
                        .email("appointee@example.com")
                        .build()).build())
                .build();

        Map result = personalisation.create(SscsCaseDataWrapper.builder()
                .newSscsCaseData(sscsCaseData)
                .notificationEventType(SUBSCRIPTION_CREATED_NOTIFICATION)
                .build());

        assertEquals(tyaNumber, result.get(AppConstants.APPEAL_ID));
        assertEquals("http://link.com/manage-email-notifications/ZYX", result.get(AppConstants.MANAGE_EMAILS_LINK_LITERAL));
        assertEquals("http://tyalink.com/" + tyaNumber, result.get(AppConstants.TRACK_APPEAL_LINK_LITERAL));

    }

    private Hearing createHearing(LocalDate hearingDate) {
        return Hearing.builder().value(HearingDetails.builder()
                .hearingDate(hearingDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .time("12:00")
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

    private SscsCaseData createResponse(List<Hearing> hearingList) {
        return SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                        .appellant(Appellant.builder().name(name).build())
                        .build())
                .subscriptions(subscriptions)
                .hearings(hearingList)
                .build();
    }
}
