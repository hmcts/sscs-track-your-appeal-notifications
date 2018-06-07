package uk.gov.hmcts.sscs.service.reminder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.sscs.domain.notify.EventType.*;

import java.time.ZonedDateTime;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;
import uk.gov.hmcts.sscs.CcdResponseUtils;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.notify.EventType;
import uk.gov.hmcts.sscs.exception.ReminderException;
import uk.gov.hmcts.sscs.extractor.DwpResponseReceivedDateExtractor;

@RunWith(MockitoJUnitRunner.class)
public class HearingHoldingReminderTest {

    @Mock
    private DwpResponseReceivedDateExtractor dwpResponseReceivedDateExtractor;
    @Mock
    private JobGroupGenerator jobGroupGenerator;
    @Mock
    private JobScheduler<String> jobScheduler;

    private HearingHoldingReminder hearingHoldingReminder;

    @Before
    public void setup() {
        hearingHoldingReminder = new HearingHoldingReminder(
            dwpResponseReceivedDateExtractor,
            jobGroupGenerator,
            jobScheduler,
            86400,
            (86400 * 2)
        );
    }

    @Test
    public void canHandleEvent() {

        for (EventType eventType : EventType.values()) {

            CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponse(eventType);

            if (eventType == DWP_RESPONSE_RECEIVED) {
                assertTrue(hearingHoldingReminder.canHandle(ccdResponse));
            } else {

                assertFalse(hearingHoldingReminder.canHandle(ccdResponse));
                assertThatThrownBy(() -> hearingHoldingReminder.handle(ccdResponse))
                    .hasMessage("cannot handle ccdResponse")
                    .isExactlyInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Test
    public void scheduleHearingHoldingRemindersWhenDwpResponseReceived() {

        final String expectedInterimJobGroup = "ID_EVENT";
        final String expectedFinalJobGroup = "ID_FINAL_EVENT";

        final String expectedTriggerAt1 = "2018-01-02T14:01:18Z[Europe/London]";
        final String expectedTriggerAt2 = "2018-01-04T14:01:18Z[Europe/London]";
        final String expectedTriggerAt3 = "2018-01-06T14:01:18Z[Europe/London]";
        final String expectedTriggerAt4 = "2018-01-08T14:01:18Z[Europe/London]";

        ZonedDateTime dwpResponseReceivedDate = ZonedDateTime.parse("2018-01-01T14:01:18Z[Europe/London]");

        CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponseWithEvent(
            DWP_RESPONSE_RECEIVED,
            DWP_RESPONSE_RECEIVED,
            dwpResponseReceivedDate.toString()
        );

        when(dwpResponseReceivedDateExtractor.extract(ccdResponse)).thenReturn(Optional.of(dwpResponseReceivedDate));
        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), HEARING_HOLDING_REMINDER.getId())).thenReturn(expectedInterimJobGroup);
        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), FINAL_HEARING_HOLDING_REMINDER.getId())).thenReturn(expectedFinalJobGroup);

        hearingHoldingReminder.handle(ccdResponse);

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);

        verify(jobScheduler, times(4)).schedule(
            jobCaptor.capture()
        );

        Job<String> job1 = jobCaptor.getAllValues().get(0);
        assertEquals(expectedInterimJobGroup, job1.group);
        assertEquals(HEARING_HOLDING_REMINDER.getId(), job1.name);
        assertEquals(CcdResponseUtils.CASE_ID, job1.payload);
        assertEquals(expectedTriggerAt1, job1.triggerAt.toString());

        Job<String> job2 = jobCaptor.getAllValues().get(1);
        assertEquals(expectedInterimJobGroup, job2.group);
        assertEquals(HEARING_HOLDING_REMINDER.getId(), job2.name);
        assertEquals(CcdResponseUtils.CASE_ID, job2.payload);
        assertEquals(expectedTriggerAt2, job2.triggerAt.toString());

        Job<String> job3 = jobCaptor.getAllValues().get(2);
        assertEquals(expectedInterimJobGroup, job3.group);
        assertEquals(HEARING_HOLDING_REMINDER.getId(), job3.name);
        assertEquals(CcdResponseUtils.CASE_ID, job3.payload);
        assertEquals(expectedTriggerAt3, job3.triggerAt.toString());

        Job<String> job4 = jobCaptor.getAllValues().get(3);
        assertEquals(expectedFinalJobGroup, job4.group);
        assertEquals(FINAL_HEARING_HOLDING_REMINDER.getId(), job4.name);
        assertEquals(CcdResponseUtils.CASE_ID, job4.payload);
        assertEquals(expectedTriggerAt4, job4.triggerAt.toString());
    }

    @Test(expected = ReminderException.class)
    public void throwExceptionWhenDwpResponseReceivedDateNotPresent() {

        CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponse(DWP_RESPONSE_RECEIVED);

        when(dwpResponseReceivedDateExtractor.extract(ccdResponse)).thenReturn(Optional.empty());

        hearingHoldingReminder.handle(ccdResponse);
    }

}
