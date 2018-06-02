package uk.gov.hmcts.sscs.service.reminder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.sscs.domain.notify.EventType.*;

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

@RunWith(MockitoJUnitRunner.class)
public class DwpResponseReceivedReminderHandlerTest {

    @Mock
    private JobGroupGenerator jobGroupGenerator;
    @Mock
    private JobScheduler<String> jobScheduler;

    private DwpResponseReceivedReminderHandler dwpResponseReceivedReminderHandler;

    @Before
    public void setup() {
        dwpResponseReceivedReminderHandler = new DwpResponseReceivedReminderHandler(
            jobGroupGenerator,
            jobScheduler,
            172800
        );
    }

    @Test
    public void canHandleEvent() {

        for (EventType eventType : EventType.values()) {

            CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponse(eventType);

            if (eventType == DWP_RESPONSE_RECEIVED) {
                assertTrue(dwpResponseReceivedReminderHandler.canHandle(ccdResponse));
            } else {

                assertFalse(dwpResponseReceivedReminderHandler.canHandle(ccdResponse));
                assertThatThrownBy(() -> dwpResponseReceivedReminderHandler.handle(ccdResponse))
                    .hasMessage("cannot handle ccdResponse")
                    .isExactlyInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Test
    public void schedulesReminder() {

        final String expectedJobGroup = "ID_EVENT";
        final String expectedTriggerAt = "2018-01-03T14:01:18Z[Europe/London]";

        String eventDate = "2018-01-01T14:01:18";

        CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponseWithEvent(
            DWP_RESPONSE_RECEIVED,
            DWP_RESPONSE_RECEIVED,
            eventDate
        );

        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), EVIDENCE_REMINDER)).thenReturn(expectedJobGroup);

        dwpResponseReceivedReminderHandler.handle(ccdResponse);

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);

        verify(jobScheduler, times(1)).schedule(
            jobCaptor.capture()
        );

        Job<String> job = jobCaptor.getValue();
        assertEquals(expectedJobGroup, job.group);
        assertEquals(EVIDENCE_REMINDER.getId(), job.name);
        assertEquals(CcdResponseUtils.CASE_ID, job.payload);
        assertEquals(expectedTriggerAt, job.triggerAt.toString());
    }

    @Test(expected = ReminderException.class)
    public void throwExceptionWhenCannotFindEventDateForDwpResponseReceivedEvent() {

        CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponse(DWP_RESPONSE_RECEIVED);

        dwpResponseReceivedReminderHandler.handle(ccdResponse);
    }

    @Test(expected = ReminderException.class)
    public void throwExceptionForUnrecognisedReminderEvent() {

        CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponseWithEvent(
            DWP_RESPONSE_RECEIVED,
            APPEAL_WITHDRAWN,
            "2018-01-01T14:01:18"
        );

        dwpResponseReceivedReminderHandler.handle(ccdResponse);
    }

}
