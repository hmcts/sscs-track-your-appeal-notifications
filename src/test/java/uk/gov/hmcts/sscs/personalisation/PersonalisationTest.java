package uk.gov.hmcts.sscs.personalisation;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.sscs.config.AppConstants.*;
import static uk.gov.hmcts.sscs.domain.Benefit.PIP;
import static uk.gov.hmcts.sscs.domain.notify.EventType.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.annotation.Resource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.sscs.config.NotificationConfig;
import uk.gov.hmcts.sscs.domain.*;
import uk.gov.hmcts.sscs.domain.notify.Event;
import uk.gov.hmcts.sscs.domain.notify.Link;
import uk.gov.hmcts.sscs.extractor.HearingContactDateExtractor;
import uk.gov.hmcts.sscs.service.MessageAuthenticationServiceImpl;
import uk.gov.hmcts.sscs.service.RegionalProcessingCenterService;

public class PersonalisationTest {

    private static final String CASE_ID = "54321";
    public static final String ADDRESS1 = "HM Courts & Tribunals Service";
    public static final String ADDRESS2 = "Social Security & Child Support Appeals";
    public static final String ADDRESS3 = "Prudential Buildings";
    public static final String ADDRESS4 = "36 Dale Street";
    public static final String CITY = "LIVERPOOL";
    public static final String POSTCODE = "L2 5UZ";

    @Mock
    private NotificationConfig config;

    @Mock
    private HearingContactDateExtractor hearingContactDateExtractor;

    @Mock
    private MessageAuthenticationServiceImpl macService;

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @InjectMocks
    @Resource
    public Personalisation personalisation;

    String date = "2018-07-01T14:01:18.243";

    Subscriptions subscriptions;

    Name name;

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

        RegionalProcessingCenter rpc = new RegionalProcessingCenter();
        rpc.createRegionalProcessingCenter("LIVERPOOL", "HM Courts & Tribunals Service", ADDRESS2,
            ADDRESS3, "36 Dale Street", POSTCODE, "LIVERPOOL");

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
    public void customisePersonalisation() {
        List<Events> events = new ArrayList<>();
        events.add(Events.builder().value(Event.builder().date(date).type(APPEAL_RECEIVED.getId()).build()).build());

        CcdResponse response = CcdResponse.builder()
            .caseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .notificationType(APPEAL_RECEIVED)
            .events(events)
            .build();

        Map<String, String> result = personalisation.create(CcdResponseWrapper.builder().newCcdResponse(response).build());

        assertEquals("PIP", result.get(BENEFIT_NAME_ACRONYM_LITERAL));
        assertEquals("Personal Independence Payment", result.get(BENEFIT_FULL_NAME_LITERAL));
        assertEquals("SC/1234/5", result.get(APPEAL_REF));
        assertEquals("GLSCRR", result.get(APPEAL_ID));
        assertEquals("Harry Kane", result.get(APPELLANT_NAME));
        assertEquals("01234543225", result.get(PHONE_NUMBER));
        assertEquals("http://link.com/manage-email-notifications/ZYX", result.get(MANAGE_EMAILS_LINK_LITERAL));
        assertEquals("http://tyalink.com/GLSCRR", result.get(TRACK_APPEAL_LINK_LITERAL));
        assertEquals(DWP_ACRONYM, result.get(FIRST_TIER_AGENCY_ACRONYM));
        assertEquals(DWP_FUL_NAME, result.get(FIRST_TIER_AGENCY_FULL_NAME));
        assertEquals("05 August 2018", result.get(APPEAL_RESPOND_DATE));
        assertEquals("http://link.com/GLSCRR", result.get(SUBMIT_EVIDENCE_LINK_LITERAL));
        assertEquals("http://link.com/progress/GLSCRR/expenses", result.get(CLAIMING_EXPENSES_LINK_LITERAL));
        assertEquals("http://link.com/progress/GLSCRR/abouthearing", result.get(HEARING_INFO_LINK_LITERAL));
        assertNull(result.get(EVIDENCE_RECEIVED_DATE_LITERAL));

        assertEquals(ADDRESS1, result.get(REGIONAL_OFFICE_NAME_LITERAL));
        assertEquals(DEPARTMENT_NAME_STRING, result.get(DEPARTMENT_NAME_LITERAL));
        assertEquals(ADDRESS2, result.get(SUPPORT_CENTRE_NAME_LITERAL));
        assertEquals(ADDRESS3, result.get(ADDRESS_LINE_LITERAL));
        assertEquals(ADDRESS4, result.get(TOWN_LITERAL));
        assertEquals(CITY, result.get(COUNTY_LITERAL));
        assertEquals(POSTCODE, result.get(POSTCODE_LITERAL));
    }

    @Test
    public void givenEvidenceReceivedNotification_customisePersonalisation() {
        List<Events> events = new ArrayList<>();
        events.add(Events.builder().value(Event.builder().date(date).type(APPEAL_RECEIVED.getId()).build()).build());

        List<Documents> documents = new ArrayList<>();

        Documents doc = Documents.builder().value(Doc.builder()
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

        CcdResponse response = CcdResponse.builder()
            .caseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .notificationType(EVIDENCE_RECEIVED)
            .events(events)
            .evidence(evidence)
            .build();

        Map<String, String> result = personalisation.create(CcdResponseWrapper.builder().newCcdResponse(response).build());

        assertEquals("01 July 2018", result.get(EVIDENCE_RECEIVED_DATE_LITERAL));
    }

    @Test
    public void setAppealReceivedEventData() {
        List<Events> events = new ArrayList<>();
        events.add(Events.builder().value(Event.builder().date(date).type(APPEAL_RECEIVED.getId()).build()).build());

        CcdResponse response = CcdResponse.builder()
            .caseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
            .notificationType(APPEAL_RECEIVED)
            .events(events)
            .build();

        Map<String, String> result = personalisation.setEventData(new HashMap<>(), response);

        assertEquals("05 August 2018", result.get(APPEAL_RESPOND_DATE));
    }

    @Test
    public void setEvidenceReceivedEventData() {
        List<Documents> documents = new ArrayList<>();

        Documents doc = Documents.builder().value(Doc.builder()
            .dateReceived("2018-07-01")
            .evidenceType("Medical")
            .evidenceProvidedBy("Caseworker").build()).build();

        documents.add(doc);

        Evidence evidence = Evidence.builder().documents(documents).build();

        CcdResponse response = CcdResponse.builder()
            .caseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
            .notificationType(EVIDENCE_RECEIVED)
            .evidence(evidence)
            .build();

        Map<String, String> result = personalisation.setEvidenceReceivedNotificationData(new HashMap<>(), response);

        assertEquals("01 July 2018", result.get(EVIDENCE_RECEIVED_DATE_LITERAL));
    }

    @Test
    public void givenHearingData_correctlySetTheHearingDetails() {
        LocalDate hearingDate = LocalDate.now().plusDays(7);

        Hearing hearing = Hearing.builder().value(HearingDetails.builder()
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

        List<Hearing> hearingList = new ArrayList<>();
        hearingList.add(hearing);

        CcdResponse response = CcdResponse.builder()
            .caseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .notificationType(HEARING_BOOKED)
            .subscriptions(subscriptions)
            .hearings(hearingList)
            .build();

        Map<String, String> result = personalisation.create(CcdResponseWrapper.builder().newCcdResponse(response).build());

        assertEquals(hearingDate.format(DateTimeFormatter.ofPattern(RESPONSE_DATE_FORMAT)), result.get(HEARING_DATE));
        assertEquals("12:00 PM", result.get(HEARING_TIME));
        assertEquals("The venue, 12 The Road Avenue, Village, Aberdeen, Aberdeenshire, AB12 0HN", result.get(VENUE_ADDRESS_LITERAL));
        assertEquals("http://www.googlemaps.com/aberdeenvenue", result.get(VENUE_MAP_LINK_LITERAL));
        assertEquals("in 7 days", result.get(DAYS_TO_HEARING_LITERAL));
    }

    @Test
    public void givenOnlyOneDayUntilHearing_correctlySetTheDaysToHearingText() {
        LocalDate hearingDate = LocalDate.now().plusDays(1);

        Hearing hearing = Hearing.builder().value(HearingDetails.builder()
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

        List<Hearing> hearingList = new ArrayList<>();
        hearingList.add(hearing);

        CcdResponse response = CcdResponse.builder()
            .caseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .notificationType(HEARING_BOOKED)
            .subscriptions(subscriptions)
            .hearings(hearingList)
            .build();

        Map<String, String> result = personalisation.create(CcdResponseWrapper.builder().newCcdResponse(response).build());

        assertEquals("in 1 day", result.get(DAYS_TO_HEARING_LITERAL));
    }

    @Test
    public void handleNullEventWhenPopulatingEventData() {
        CcdResponse response = CcdResponse.builder()
            .caseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
            .notificationType(POSTPONEMENT)
            .build();

        Map<String, String> result = personalisation.setEventData(new HashMap<>(), response);

        assertEquals(new HashMap<>(), result);
    }

    @Test
    public void handleEmptyEventsWhenPopulatingEventData() {
        CcdResponse response = CcdResponse.builder()
            .caseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
            .notificationType(POSTPONEMENT)
            .events(new ArrayList())
            .build();

        Map<String, String> result = personalisation.setEventData(new HashMap<>(), response);

        assertEquals(new HashMap<>(), result);
    }

    @Test
    public void shouldPopulateRegionalProcessingCenterFromCcdCaseIfItsPresent() {
        RegionalProcessingCenter rpc = new RegionalProcessingCenter();
        rpc.createRegionalProcessingCenter("LIVERPOOL", ADDRESS1,
            ADDRESS2, ADDRESS3,
            ADDRESS4, POSTCODE,
            CITY);

        CcdResponse response = CcdResponse.builder().regionalProcessingCenter(rpc).build();

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
    public void shouldPopulateHearingContactDateFromCcdCaseIfPresent() {

        CcdResponse response = CcdResponse.builder().build();

        ZonedDateTime now = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1528907807), ZoneId.of("UTC"));
        when(hearingContactDateExtractor.extract(response)).thenReturn(Optional.of(now));

        Map<String, String> values = new HashMap<>();
        personalisation.setHearingContactDate(values, response);

        assertEquals("13 June 2018", values.get(HEARING_CONTACT_DATE));
    }

    @Test
    public void shouldNotPopulateHearingContactDateFromCcdCaseIfNotPresent() {

        CcdResponse response = CcdResponse.builder().build();

        when(hearingContactDateExtractor.extract(response)).thenReturn(Optional.empty());

        Map<String, String> values = new HashMap<>();
        personalisation.setHearingContactDate(values, response);

        assertFalse(values.containsKey(HEARING_CONTACT_DATE));
    }
}
