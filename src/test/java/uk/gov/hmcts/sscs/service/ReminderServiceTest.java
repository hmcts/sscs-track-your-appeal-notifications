package uk.gov.hmcts.sscs.service;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.sscs.config.AppConstants.ZONE_ID;
import static uk.gov.hmcts.sscs.domain.notify.EventType.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.Events;
import uk.gov.hmcts.sscs.domain.notify.Event;
import uk.gov.hmcts.sscs.domain.notify.EventType;
import uk.gov.hmcts.sscs.exception.ReminderException;

public class ReminderServiceTest {

    private ReminderService service;

    @Mock
    private JobScheduler<String> jobScheduler;

    @Before
    public void setup() {
        initMocks(this);
        service = new ReminderService(jobScheduler);
        ReflectionTestUtils.setField(service, "evidenceReminderDelay", "172800");
    }

    @Test
    public void createReminderFromCcdResponse() {
        String date = "2018-01-01T00:00:00.000";

        List<Events> events = new ArrayList<>();
        events.add(Events.builder().value(Event.builder().date(date).type(DWP_RESPONSE_RECEIVED.getId()).build()).build());

        CcdResponse ccdResponse = CcdResponse.builder()
                .caseId("123456").notificationType(DWP_RESPONSE_RECEIVED).events(events)
                .build();

        ArgumentCaptor captor = ArgumentCaptor.forClass(Job.class);
        service.createJob(ccdResponse);

        verify(jobScheduler).schedule((Job<String>) captor.capture());

        Job<String> actualJob = (Job<String>) captor.getValue();

        ZonedDateTime expectedDateTrigger = ZonedDateTime.of(LocalDate.of(2018, 1, 3), LocalTime.of(0, 0, 0), ZoneId.of(ZONE_ID));

        Job<String> expected = new Job(EVIDENCE_REMINDER.getId(), new String("123456"), expectedDateTrigger);

        assertEquals(expected.name, actualJob.name);
        assertEquals(expected.payload, actualJob.payload);
        assertEquals(expected.triggerAt, actualJob.triggerAt);
    }

    @Test
    public void findEvidenceReminderTypeFromDwpResponseReceivedEvent() {
        EventType result = service.findReminderType(DWP_RESPONSE_RECEIVED);

        assertEquals(EVIDENCE_REMINDER, result);
    }

    @Test
    public void doNotFindReminderTypeFromAnEventWithNoReminderRequired() {
        assertNull(service.findReminderType(APPEAL_RECEIVED));
    }

    @Test
    public void findReminderDateForEventWithDwpResponseReceived() {
        String date = "2018-01-01T14:01:18";
        List<Events> events = new ArrayList<>();
        events.add(Events.builder().value(Event.builder().date(date).type(DWP_RESPONSE_RECEIVED.getId()).build()).build());

        CcdResponse ccdResponse = CcdResponse.builder().notificationType(DWP_RESPONSE_RECEIVED).events(events).build();

        ZonedDateTime result = service.findReminderDate(ccdResponse);

        assertEquals(ZonedDateTime.of(LocalDate.of(2018, 1, 3), LocalTime.of(14, 01, 18), ZoneId.of(ZONE_ID)), result);
    }

    @Test(expected = ReminderException.class)
    public void throwExceptionWhenCannotFindEventDateForDwpResponseReceivedEvent() {
        String date = "2018-01-01T14:01:18";
        List<Events> events = new ArrayList<>();
        events.add(Events.builder().value(Event.builder().date(date).type(APPEAL_WITHDRAWN.getId()).build()).build());

        CcdResponse ccdResponse = CcdResponse.builder().notificationType(DWP_RESPONSE_RECEIVED).events(events).build();

        service.findReminderDate(ccdResponse);
    }

    @Test(expected = ReminderException.class)
    public void throwExceptionForUnrecognisedReminderEvent() {
        String date = "2018-01-01T14:01:18";
        List<Events> events = new ArrayList<>();
        events.add(Events.builder().value(Event.builder().date(date).type(DWP_RESPONSE_RECEIVED.getId()).build()).build());

        CcdResponse ccdResponse = CcdResponse.builder().notificationType(APPEAL_WITHDRAWN).events(events).build();

        service.findReminderDate(ccdResponse);
    }
}
