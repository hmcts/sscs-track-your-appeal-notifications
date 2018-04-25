package uk.gov.hmcts.sscs.personalisation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.sscs.config.AppConstants.*;
import static uk.gov.hmcts.sscs.domain.Benefit.PIP;
import static uk.gov.hmcts.sscs.domain.notify.EventType.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.sscs.config.NotificationConfig;
import uk.gov.hmcts.sscs.domain.*;
import uk.gov.hmcts.sscs.domain.notify.Event;
import uk.gov.hmcts.sscs.domain.notify.Link;
import uk.gov.hmcts.sscs.service.MessageAuthenticationServiceImpl;
import uk.gov.hmcts.sscs.service.RegionalProcessingCenterService;

public class PersonalisationTest {

    private static final String CASE_ID = "54321";

    @Mock
    private NotificationConfig config;

    @Mock
    private MessageAuthenticationServiceImpl macService;

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @InjectMocks
    @Resource
    public Personalisation personalisation;

    String date = "2018-07-01T14:01:18.243";

    ZonedDateTime zonedDateTime;

    Subscriptions subscriptions;

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
        when(config.isJobSchedulerEnabled()).thenReturn(true);
        when(macService.generateToken("GLSCRR", PIP.name())).thenReturn("ZYX");

        RegionalProcessingCenter rpc = new RegionalProcessingCenter();
        rpc.createRegionalProcessingCenter("LIVERPOOL", "HM Courts & Tribunals Service", "Social Security & Child Support Appeals",
                "Prudential Buildings", "36 Dale Street", "L2 5UZ", "LIVERPOOL");

        when(regionalProcessingCenterService.getByScReferenceCode("SC/1234/5")).thenReturn(rpc);

        zonedDateTime = ZonedDateTime.of(LocalDate.of(2018, 7, 1), LocalTime.of(0, 0), ZoneId.of(ZONE_ID));

        Subscription appellantSubscription = Subscription.builder()
                .firstName("Harry")
                .surname("Kane")
                .title("Mr")
                .tya("GLSCRR")
                .email("test@email.com")
                .mobile("07983495065")
                .subscribeEmail("Yes")
                .subscribeSms("No")
                .build();

        subscriptions = Subscriptions.builder().appellantSubscription(appellantSubscription).build();

    }

    @Test
    public void customisePersonalisation() {
        List<Events> events = new ArrayList<>();
        events.add(Events.builder().value(Event.builder().date(date).type(APPEAL_RECEIVED.getId()).build()).build());

        CcdResponse response = CcdResponse.builder()
            .caseId(CASE_ID).benefitType(PIP).caseReference("SC/1234/5")
            .subscriptions(subscriptions)
            .notificationType(APPEAL_RECEIVED)
            .events(events)
            .build();

        Map<String, String> result = personalisation.create(CcdResponseWrapper.builder().newCcdResponse(response).build());

        assertEquals("PIP benefit", result.get(BENEFIT_NAME_ACRONYM_LITERAL));
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
        assertEquals("12 August 2018", result.get(HEARING_CONTACT_DATE));
        assertEquals("http://link.com/GLSCRR", result.get(SUBMIT_EVIDENCE_LINK_LITERAL));
        assertEquals("http://link.com/progress/GLSCRR/expenses", result.get(CLAIMING_EXPENSES_LINK_LITERAL));
        assertEquals("http://link.com/progress/GLSCRR/abouthearing", result.get(HEARING_INFO_LINK_LITERAL));
        assertNull(result.get(EVIDENCE_RECEIVED_DATE_LITERAL));

        assertEquals("HM Courts & Tribunals Service", result.get(REGIONAL_OFFICE_NAME_LITERAL));
        assertEquals(DEPARTMENT_NAME_STRING, result.get(DEPARTMENT_NAME_LITERAL));
        assertEquals("Social Security & Child Support Appeals", result.get(SUPPORT_CENTRE_NAME_LITERAL));
        assertEquals("Prudential Buildings", result.get(ADDRESS_LINE_LITERAL));
        assertEquals("36 Dale Street", result.get(TOWN_LITERAL));
        assertEquals("LIVERPOOL", result.get(COUNTY_LITERAL));
        assertEquals("L2 5UZ", result.get(POSTCODE_LITERAL));
    }

    @Test
    public void givenEvidenceReceivedNotification_customisePersonalisation() {
        List<Events> events = new ArrayList<>();
        events.add(Events.builder().value(Event.builder().date(date).type(APPEAL_RECEIVED.getId()).build()).build());

        Evidence evidence = Evidence.builder()
                .dateReceived(zonedDateTime.toLocalDate())
                .evidenceType("Medical")
                .evidenceProvidedBy("Caseworker").build();

        List<Evidence> evidenceList = new ArrayList<>();
        evidenceList.add(evidence);

        Subscription appellantSubscription = Subscription.builder()
                .firstName("Harry")
                .surname("Kane")
                .title("Mr")
                .tya("GLSCRR")
                .email("test@email.com")
                .mobile("07983495065")
                .subscribeEmail("Yes")
                .subscribeSms("No")
                .build();

        Subscriptions subscriptions = Subscriptions.builder().appellantSubscription(appellantSubscription).build();

        CcdResponse response = CcdResponse.builder()
                .caseId(CASE_ID).benefitType(PIP).caseReference("SC/1234/5")
                .subscriptions(subscriptions)
                .notificationType(EVIDENCE_RECEIVED)
                .events(events)
                .evidences(evidenceList)
                .build();

        Map<String, String> result = personalisation.create(CcdResponseWrapper.builder().newCcdResponse(response).build());

        assertEquals("01 July 2018", result.get(EVIDENCE_RECEIVED_DATE_LITERAL));
    }

    @Test
    public void setAppealReceivedEventData() {
        List<Events> events = new ArrayList<>();
        events.add(Events.builder().value(Event.builder().date(date).type(APPEAL_RECEIVED.getId()).build()).build());

        CcdResponse response = CcdResponse.builder()
                .caseId(CASE_ID).benefitType(PIP).caseReference("SC/1234/5")
                .notificationType(APPEAL_RECEIVED)
                .events(events)
                .build();

        Map<String, String> result = personalisation.setEventData(new HashMap<>(), response);

        assertEquals("05 August 2018", result.get(APPEAL_RESPOND_DATE));
        assertEquals("12 August 2018", result.get(HEARING_CONTACT_DATE));
    }

    @Test
    public void setEvidenceReceivedEventData() {
        Evidence evidence = Evidence.builder()
                .dateReceived(zonedDateTime.toLocalDate())
                .evidenceType("Medical")
                .evidenceProvidedBy("Caseworker").build();

        List<Evidence> evidenceList = new ArrayList<>();
        evidenceList.add(evidence);

        CcdResponse response = CcdResponse.builder()
                .caseId(CASE_ID).benefitType(PIP).caseReference("SC/1234/5")
                .notificationType(EVIDENCE_RECEIVED)
                .evidences(evidenceList)
                .build();

        Map<String, String> result = personalisation.setEvidenceReceivedNotificationData(new HashMap<>(), response);

        assertEquals("01 July 2018", result.get(EVIDENCE_RECEIVED_DATE_LITERAL));
    }

    @Test
    public void givenHearingData_correctlySetTheHearingDetails() {
        Hearing hearing = Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2018-01-01")
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
                .caseId(CASE_ID).benefitType(PIP).caseReference("SC/1234/5")
                .notificationType(HEARING_BOOKED)
                .subscriptions(subscriptions)
                .hearings(hearingList)
                .build();

        Map<String, String> result = personalisation.create(CcdResponseWrapper.builder().newCcdResponse(response).build());

        assertEquals("01 January 2018", result.get(HEARING_DATE));
        assertEquals("12:00 PM", result.get(HEARING_TIME));
        assertEquals("The venue, 12 The Road Avenue, Village, Aberdeen, Aberdeenshire, AB12 0HN", result.get(VENUE_ADDRESS_LITERAL));
        assertEquals("http://www.googlemaps.com/aberdeenvenue", result.get(VENUE_MAP_LINK_LITERAL));
    }

    @Test
    public void setPostponementEventData() {
        List<Events> events = new ArrayList<>();
        events.add(Events.builder().value(Event.builder().date(date).type(POSTPONEMENT.getId()).build()).build());

        CcdResponse response = CcdResponse.builder()
                .caseId(CASE_ID).benefitType(PIP).caseReference("SC/1234/5")
                .notificationType(POSTPONEMENT)
                .events(events)
                .build();

        Map<String, String> result = personalisation.setEventData(new HashMap<>(), response);

        assertEquals("12 August 2018", result.get(HEARING_CONTACT_DATE));
    }

    @Test
    public void handleNullEventWhenPopulatingEventData() {
        CcdResponse response = CcdResponse.builder()
                .caseId(CASE_ID).benefitType(PIP).caseReference("SC/1234/5")
                .notificationType(POSTPONEMENT)
                .build();

        Map<String, String> result = personalisation.setEventData(new HashMap<>(), response);

        assertEquals(new HashMap<>(), result);
    }

    @Test
    public void handleEmptyEventsWhenPopulatingEventData() {
        CcdResponse response = CcdResponse.builder()
                .caseId(CASE_ID).benefitType(PIP).caseReference("SC/1234/5")
                .notificationType(POSTPONEMENT)
                .events(new ArrayList())
                .build();

        Map<String, String> result = personalisation.setEventData(new HashMap<>(), response);

        assertEquals(new HashMap<>(), result);
    }
}
