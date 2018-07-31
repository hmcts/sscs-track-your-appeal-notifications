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
import uk.gov.hmcts.sscs.extractor.AppealReceivedDateExtractor;

@RunWith(MockitoJUnitRunner.class)
public class DwpResponseLateReminderTest {

    @Mock
    private AppealReceivedDateExtractor appealReceivedDateExtractor;
    @Mock
    private JobGroupGenerator jobGroupGenerator;
    @Mock
    private JobScheduler<String> jobScheduler;

    private DwpResponseLateReminder dwpResponseLateReminder;

    @Before
    public void setup() {
        dwpResponseLateReminder = new DwpResponseLateReminder(
            appealReceivedDateExtractor,
            jobGroupGenerator,
            jobScheduler,
            86400
        );
    }

    @Test
    public void canHandleEvent() {

        for (EventType eventType : EventType.values()) {

            CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponse(eventType);

            if (eventType == APPEAL_RECEIVED) {
                assertTrue(dwpResponseLateReminder.canHandle(ccdResponse));
            } else {

                assertFalse(dwpResponseLateReminder.canHandle(ccdResponse));
                assertThatThrownBy(() -> dwpResponseLateReminder.handle(ccdResponse))
                    .hasMessage("cannot handle ccdResponse")
                    .isExactlyInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Test
    public void scheduleDwpResponseLateReminderWhenDwpResponseNotReceivedInTime() {

        final String expectedJobGroup = "ID_EVENT";
        final String expectedTriggerAt = "2018-01-02T14:01:18Z[Europe/London]";
        ZonedDateTime appealReceivedDate = ZonedDateTime.parse("2018-01-01T14:01:18Z[Europe/London]");

        CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponseWithEvent(
            APPEAL_RECEIVED,
            APPEAL_RECEIVED,
            appealReceivedDate.toString()
        );

        when(appealReceivedDateExtractor.extract(ccdResponse)).thenReturn(Optional.of(appealReceivedDate));
        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), DWP_RESPONSE_LATE_REMINDER.getId())).thenReturn(expectedJobGroup);

        dwpResponseLateReminder.handle(ccdResponse);

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);

        verify(jobScheduler, times(1)).schedule(
            jobCaptor.capture()
        );

        Job<String> job = jobCaptor.getAllValues().get(0);
        assertEquals(expectedJobGroup, job.group);
        assertEquals(DWP_RESPONSE_LATE_REMINDER.getId(), job.name);
        assertEquals(CcdResponseUtils.CASE_ID, job.payload);
        assertEquals(expectedTriggerAt, job.triggerAt.toString());
    }

    @Test(expected = ReminderException.class)
    public void throwExceptionWhenAppealReceivedDateNotPresent() {

        CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponse(APPEAL_RECEIVED);

        when(appealReceivedDateExtractor.extract(ccdResponse)).thenReturn(Optional.empty());

        dwpResponseLateReminder.handle(ccdResponse);
    }

}
