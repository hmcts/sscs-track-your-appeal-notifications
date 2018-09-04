package uk.gov.hmcts.reform.sscs.service.reminder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.HEARING_BOOKED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.HEARING_REMINDER;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.SscsCaseDataUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.exception.ReminderException;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;

@RunWith(MockitoJUnitRunner.class)
public class HearingReminderTest {

    @Mock
    private JobGroupGenerator jobGroupGenerator;
    @Mock
    private JobScheduler<String> jobScheduler;

    private HearingReminder hearingReminder;

    @Before
    public void setup() {
        hearingReminder = new HearingReminder(
            jobGroupGenerator,
            jobScheduler,
            172800,
            (172800 * 2)
        );
    }

    @Test
    public void canHandleEvent() {

        for (EventType eventType : EventType.values()) {

            SscsCaseData ccdResponse = SscsCaseDataUtils.buildBasicSscsCaseData(eventType);

            if (eventType == HEARING_BOOKED) {
                assertTrue(hearingReminder.canHandle(ccdResponse));
            } else {

                assertFalse(hearingReminder.canHandle(ccdResponse));
                assertThatThrownBy(() -> hearingReminder.handle(ccdResponse))
                    .hasMessage("cannot handle ccdResponse")
                    .isExactlyInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Test
    public void schedulesReminder() {

        final String expectedJobGroup = "ID_EVENT";
        final String expectedFirstTriggerAt = "2017-12-30T14:01:18Z[Europe/London]";
        final String expectedSecondTriggerAt = "2017-12-28T14:01:18Z[Europe/London]";

        String hearingDate = "2018-01-01";
        String hearingTime = "14:01:18";

        SscsCaseData ccdResponse = SscsCaseDataUtils.buildBasicSscsCaseDataWithHearing(
            HEARING_BOOKED,
            hearingDate,
            hearingTime
        );

        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), HEARING_REMINDER.getCcdType())).thenReturn(expectedJobGroup);

        hearingReminder.handle(ccdResponse);

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);

        verify(jobScheduler, times(2)).schedule(
            jobCaptor.capture()
        );

        Job<String> firstJob = jobCaptor.getAllValues().get(0);
        assertEquals(expectedJobGroup, firstJob.group);
        assertEquals(HEARING_REMINDER.getCcdType(), firstJob.name);
        assertEquals(SscsCaseDataUtils.CASE_ID, firstJob.payload);
        assertEquals(expectedFirstTriggerAt, firstJob.triggerAt.toString());

        Job<String> secondJob = jobCaptor.getAllValues().get(1);
        assertEquals(expectedJobGroup, secondJob.group);
        assertEquals(HEARING_REMINDER.getCcdType(), secondJob.name);
        assertEquals(SscsCaseDataUtils.CASE_ID, secondJob.payload);
        assertEquals(expectedSecondTriggerAt, secondJob.triggerAt.toString());
    }

    @Test(expected = ReminderException.class)
    public void throwExceptionWhenCannotFindHearingDate() {

        SscsCaseData ccdResponse = SscsCaseDataUtils.buildBasicSscsCaseData(HEARING_BOOKED);

        hearingReminder.handle(ccdResponse);
    }

}
