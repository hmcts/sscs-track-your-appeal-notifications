package uk.gov.hmcts.sscs.personalisation;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.sscs.config.AppConstants.*;
import static uk.gov.hmcts.sscs.domain.notify.EventType.*;

import java.util.*;
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

    Calendar c1;

    @Before
    public void setup() {
        initMocks(this);
        personalisation = new Personalisation(config, macService);
        when(config.getHmctsPhoneNumber()).thenReturn("01234543225");
        when(config.getManageEmailsLink()).thenReturn(new Link("http://manageemails.com/mac"));
        when(config.getTrackAppealLink()).thenReturn(new Link("http://tyalink.com/appeal_id"));
        when(config.getEvidenceSubmissionInfoLink()).thenReturn(new Link("http://link.com/appeal_id"));
        when(config.getManageEmailsLink()).thenReturn(new Link("http://link.com/manage-email-notifications/mac"));
        when(macService.generateToken("GLSCRR")).thenReturn("ZYX");
        c1 = GregorianCalendar.getInstance();
        c1.set(2018, Calendar.JANUARY, 01);
    }

    @Test
    public void customisePersonalisation() {
        Event event = new Event(c1.getTime(), DWP_RESPONSE_RECEIVED);

        Subscription appellantSubscription = new Subscription("Harry", "Kane", "Mr", "GLSCRR", "test@email.com",
                "07983495065", true, false);

        CcdResponse response = new CcdResponse("1234", appellantSubscription, null, DWP_RESPONSE_RECEIVED);
        response.setEvents(new ArrayList() {{
                add(event);
            }
        });

        Map<String, String> result = personalisation.create(new CcdResponseWrapper(response, null));

        assertEquals(BENEFIT_NAME_ACRONYM, result.get(BENEFIT_NAME_ACRONYM_LITERAL));
        assertEquals(BENEFIT_FULL_NAME, result.get(BENEFIT_FULL_NAME_LITERAL));
        assertEquals("1234", result.get(APPEAL_REF));
        assertEquals("GLSCRR", result.get(APPEAL_ID));
        assertEquals("Harry Kane", result.get(APPELLANT_NAME));
        assertEquals("01234543225", result.get(PHONE_NUMBER));
        assertEquals("http://link.com/manage-email-notifications/ZYX", result.get(MANAGE_EMAILS_LINK_LITERAL));
        assertEquals("http://tyalink.com/GLSCRR", result.get(TRACK_APPEAL_LINK_LITERAL));
        assertEquals(DWP_ACRONYM, result.get(FIRST_TIER_AGENCY_ACRONYM));
        assertEquals(DWP_FUL_NAME, result.get(FIRST_TIER_AGENCY_FULL_NAME));
        assertEquals("05 February 2018", result.get(APPEAL_RESPOND_DATE));
        assertEquals("12 February 2018", result.get(HEARING_CONTACT_DATE));
        assertEquals("http://link.com/GLSCRR", result.get(SUBMIT_EVIDENCE_LINK_LITERAL));
    }

    @Test
    public void setDwpResponseReceivedEventData() {
        Event event = new Event(c1.getTime(), DWP_RESPONSE_RECEIVED);

        CcdResponse response = new CcdResponse("1234", null, null, DWP_RESPONSE_RECEIVED);

        response.setEvents(new ArrayList() {{
                add(event);
            }
        });

        Map<String, String> result = personalisation.setEventData(new HashMap<>(), response);

        assertEquals("05 February 2018", result.get(APPEAL_RESPOND_DATE));
        assertEquals("12 February 2018", result.get(HEARING_CONTACT_DATE));
    }

    @Test
    public void setEvidenceReceivedEventData() {
        Event event = new Event(c1.getTime(), EVIDENCE_RECEIVED);

        CcdResponse response = new CcdResponse("1234", null, null, EVIDENCE_RECEIVED);

        response.setEvents(new ArrayList() {{
                add(event);
            }
        });

        Map<String, String> result = personalisation.setEventData(new HashMap<>(), response);

        assertEquals("01 January 2018", result.get(EVIDENCE_RECEIVED_DATE_LITERAL));
    }

    @Test
    public void setPostponementEventData() {
        Event event = new Event(c1.getTime(), POSTPONEMENT);

        CcdResponse response = new CcdResponse("1234", null, null, POSTPONEMENT);

        response.setEvents(new ArrayList() {{
                add(event);
            }
        });

        Map<String, String> result = personalisation.setEventData(new HashMap<>(), response);

        assertEquals("12 February 2018", result.get(HEARING_CONTACT_DATE));
    }

    @Test
    public void handleNullEventWhenPopulatingEventData() {
        CcdResponse response = new CcdResponse("1234", null, null, POSTPONEMENT);

        Map<String, String> result = personalisation.setEventData(new HashMap<>(), response);

        assertEquals(new HashMap<>(), result);
    }

    @Test
    public void handleEmptyEventsWhenPopulatingEventData() {
        CcdResponse response = new CcdResponse("1234", null, null, POSTPONEMENT);

        response.setEvents(new ArrayList());

        Map<String, String> result = personalisation.setEventData(new HashMap<>(), response);

        assertEquals(new HashMap<>(), result);
    }
}
