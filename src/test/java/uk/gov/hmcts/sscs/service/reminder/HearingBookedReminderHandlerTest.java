package uk.gov.hmcts.sscs.service.reminder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
public class HearingBookedReminderHandlerTest {

    @Mock
    private JobScheduler<String> jobScheduler;

    private HearingBookedReminderHandler hearingBookedReminderHandler;

    @Before
    public void setup() {
        hearingBookedReminderHandler = new HearingBookedReminderHandler(jobScheduler, 172800, (172800 * 2));
    }

    @Test
    public void canHandleDwpResponseReceivedEvent() {

        for (EventType eventType : EventType.values()) {

            CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponse(eventType);

            if (eventType == HEARING_BOOKED) {
                assertTrue(hearingBookedReminderHandler.canHandle(ccdResponse));
            } else {

                assertFalse(hearingBookedReminderHandler.canHandle(ccdResponse));
                assertThatThrownBy(() -> hearingBookedReminderHandler.handle(ccdResponse))
                    .hasMessage("cannot handle ccdResponse")
                    .isExactlyInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Test
    public void schedulesEvidenceReminder() {

        String hearingDate = "2018-01-01";
        String hearingTime = "14:01:18";

        final String expectedFirstTriggerAt = "2017-12-30T14:01:18Z[Europe/London]";
        final String expectedSecondTriggerAt = "2017-12-28T14:01:18Z[Europe/London]";

        CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponseWithHearing(
            HEARING_BOOKED,
            hearingDate,
            hearingTime
        );

        hearingBookedReminderHandler.handle(ccdResponse);

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);

        verify(jobScheduler, times(2)).schedule(
            jobCaptor.capture()
        );

        Job<String> firstJob = jobCaptor.getAllValues().get(0);
        assertEquals(HEARING_REMINDER.getId(), firstJob.name);
        assertEquals(CcdResponseUtils.CASE_ID, firstJob.payload);
        assertEquals(expectedFirstTriggerAt, firstJob.triggerAt.toString());

        Job<String> secondJob = jobCaptor.getAllValues().get(1);
        assertEquals(HEARING_REMINDER.getId(), secondJob.name);
        assertEquals(CcdResponseUtils.CASE_ID, secondJob.payload);
        assertEquals(expectedSecondTriggerAt, secondJob.triggerAt.toString());
    }

    @Test(expected = ReminderException.class)
    public void throwExceptionWhenCannotFindHearingDate() {

        CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponse(HEARING_BOOKED);

        hearingBookedReminderHandler.handle(ccdResponse);
    }

}
