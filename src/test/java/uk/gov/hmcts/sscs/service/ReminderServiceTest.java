package uk.gov.hmcts.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.sscs.config.AppConstants.ZONE_ID;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.sscs.client.RestClient;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.Subscription;
import uk.gov.hmcts.sscs.domain.notify.Event;
import uk.gov.hmcts.sscs.domain.notify.EventType;

public class ReminderServiceTest {

    private ReminderService service;

    @Mock
    private RestClient client;

    @Before
    public void setup() {
        initMocks(this);
        service = new ReminderService(client);
    }

    @Test
    public void createReminderFromCcdResponse() throws Exception {
        CcdResponse ccdResponse = new CcdResponse();
        Subscription subscription = new Subscription();
        subscription.setAppealNumber("123456");
        ccdResponse.setAppellantSubscription(subscription);
        ccdResponse.setNotificationType(EventType.DWP_RESPONSE_RECEIVED);
        ZonedDateTime dateTime = ZonedDateTime.of(LocalDate.of(2018, 4, 1), LocalTime.of(0, 0), ZoneId.of(ZONE_ID));

        Event event = new Event(dateTime, EventType.DWP_RESPONSE_RECEIVED);
        List<Event> events = new ArrayList();
        events.add(event);
        ccdResponse.setEvents(events);

        ReflectionTestUtils.setField(service, "callbackUrl", "www.test.com");

        service.createJob(ccdResponse);

        ArgumentCaptor captor = ArgumentCaptor.forClass(JSONObject.class);

        verify(client).post((JSONObject) captor.capture(), eq("jobs"));

        JSONObject j = (JSONObject) captor.getValue();

        JSONObject expectedBodyJson = new JSONObject();
        expectedBodyJson.put("appealNumber", "123456");
        expectedBodyJson.put("reminderType", "evidenceReminder");

        assertEquals("SSCS_evidenceReminder", j.get("name"));
        assertEquals("www.test.com", ((JSONObject) j.get("action")).get("url"));
        assertEquals("POST", ((JSONObject) j.get("action")).get("method"));
        assertEquals(expectedBodyJson.toString(), ((JSONObject) j.get("action")).get("body"));
        assertEquals("2018-04-03T00:00:00+01:00", ((JSONObject) j.get("trigger")).get("start_date_time"));
    }
}
