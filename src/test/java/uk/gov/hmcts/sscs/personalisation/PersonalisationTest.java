package uk.gov.hmcts.sscs.personalisation;

import static org.junit.Assert.assertEquals;
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
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.sscs.config.NotificationConfig;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.domain.Subscription;
import uk.gov.hmcts.sscs.domain.notify.Event;
import uk.gov.hmcts.sscs.domain.notify.Link;
import uk.gov.hmcts.sscs.service.MessageAuthenticationServiceImpl;

public class PersonalisationTest {

    public Personalisation personalisation;

    @Mock
    private NotificationConfig config;

    @Mock
    private MessageAuthenticationServiceImpl macService;

    ZonedDateTime dateTime;

    @Before
    public void setup() {
        initMocks(this);
        personalisation = new Personalisation(config, macService);
        when(config.getHmctsPhoneNumber()).thenReturn("01234543225");
        when(config.getManageEmailsLink()).thenReturn(new Link("http://manageemails.com/mac"));
        when(config.getTrackAppealLink()).thenReturn(new Link("http://tyalink.com/appeal_id"));
        when(config.getEvidenceSubmissionInfoLink()).thenReturn(new Link("http://link.com/appeal_id"));
        when(config.getManageEmailsLink()).thenReturn(new Link("http://link.com/manage-email-notifications/mac"));
        when(config.getClaimingExpensesLink()).thenReturn(new Link("http://link.com/progress/appeal_id/expenses"));
        when(config.getHearingInfoLink()).thenReturn(new Link("http://link.com/progress/appeal_id/abouthearing"));
        when(macService.generateToken("GLSCRR", PIP.name())).thenReturn("ZYX");

        dateTime = ZonedDateTime.of(LocalDate.of(2018, 7, 1), LocalTime.of(0, 0), ZoneId.of(ZONE_ID));
    }

    @Test
    public void customisePersonalisation() {
        Event event = new Event(dateTime, APPEAL_RECEIVED);

        Subscription appellantSubscription = new Subscription("Harry", "Kane", "Mr", "GLSCRR", "test@email.com",
                "07983495065", true, false);

        CcdResponse response = new CcdResponse(PIP, "1234", appellantSubscription, null, APPEAL_RECEIVED, null);
        response.setEvents(new ArrayList() {{
                add(event);
            }
        });

        Map<String, String> result = personalisation.create(new CcdResponseWrapper(response, null));

        assertEquals("PIP benefit", result.get(BENEFIT_NAME_ACRONYM_LITERAL));
        assertEquals("Personal Independence Payment", result.get(BENEFIT_FULL_NAME_LITERAL));
        assertEquals("1234", result.get(APPEAL_REF));
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
    }

    @Test
    public void setAppealReceivedEventData() {
        Event event = new Event(dateTime, APPEAL_RECEIVED);

        CcdResponse response = new CcdResponse(PIP,"1234", null, null, APPEAL_RECEIVED, null);

        response.setEvents(new ArrayList() {{
                add(event);
            }
        });

        Map<String, String> result = personalisation.setEventData(new HashMap<>(), response);

        assertEquals("05 August 2018", result.get(APPEAL_RESPOND_DATE));
        assertEquals("12 August 2018", result.get(HEARING_CONTACT_DATE));
    }

    @Test
    public void setEvidenceReceivedEventData() {
        Event event = new Event(dateTime, EVIDENCE_RECEIVED);

        CcdResponse response = new CcdResponse(PIP,"1234", null, null, EVIDENCE_RECEIVED, null);

        response.setEvents(new ArrayList() {{
                add(event);
            }
        });

        Map<String, String> result = personalisation.setEventData(new HashMap<>(), response);

        assertEquals("01 July 2018", result.get(EVIDENCE_RECEIVED_DATE_LITERAL));
    }

    @Test
    public void setEvidenceReceivedEventDataWhenNotLatestEvent() {
        Event event1 = new Event(dateTime, APPEAL_RECEIVED);
        Event event2 = new Event(dateTime.minusDays(1), EVIDENCE_RECEIVED);

        Subscription appellantSubscription = new Subscription("Harry", "Kane", "Mr", "GLSCRR", "test@email.com",
                "07983495065", true, false);

        CcdResponse response = new CcdResponse(PIP, "1234", appellantSubscription, null, EVIDENCE_RECEIVED, null);
        response.setEvents(new ArrayList() {{
                add(event1);
                add(event2);
            }
        });

        Map<String, String> result = personalisation.setEventData(new HashMap<>(), response);

        assertEquals("30 June 2018", result.get(EVIDENCE_RECEIVED_DATE_LITERAL));
    }

    @Test
    public void setPostponementEventData() {
        Event event = new Event(dateTime, POSTPONEMENT);

        CcdResponse response = new CcdResponse(PIP,"1234", null, null, POSTPONEMENT, null);

        response.setEvents(new ArrayList() {{
                add(event);
            }
        });

        Map<String, String> result = personalisation.setEventData(new HashMap<>(), response);

        assertEquals("12 August 2018", result.get(HEARING_CONTACT_DATE));
    }

    @Test
    public void handleNullEventWhenPopulatingEventData() {
        CcdResponse response = new CcdResponse(PIP,"1234", null, null, POSTPONEMENT, null);

        Map<String, String> result = personalisation.setEventData(new HashMap<>(), response);

        assertEquals(new HashMap<>(), result);
    }

    @Test
    public void handleEmptyEventsWhenPopulatingEventData() {
        CcdResponse response = new CcdResponse(PIP,"1234", null, null, POSTPONEMENT, null);

        response.setEvents(new ArrayList());

        Map<String, String> result = personalisation.setEventData(new HashMap<>(), response);

        assertEquals(new HashMap<>(), result);
    }
}
