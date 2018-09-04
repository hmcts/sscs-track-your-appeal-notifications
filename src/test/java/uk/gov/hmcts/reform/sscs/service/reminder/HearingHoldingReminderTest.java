package uk.gov.hmcts.reform.sscs.service.reminder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;

import java.time.ZonedDateTime;
import java.util.Optional;
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
import uk.gov.hmcts.reform.sscs.extractor.HearingContactDateExtractor;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;

@RunWith(MockitoJUnitRunner.class)
public class HearingHoldingReminderTest {

    @Mock
    private HearingContactDateExtractor hearingContactDateExtractor;
    @Mock
    private JobGroupGenerator jobGroupGenerator;
    @Mock
    private JobScheduler<String> jobScheduler;

    private HearingHoldingReminder hearingHoldingReminder;

    @Before
    public void setup() {
        hearingHoldingReminder = new HearingHoldingReminder(
            hearingContactDateExtractor,
            jobGroupGenerator,
            jobScheduler
        );
    }

    @Test
    public void canHandleEvent() {

        for (EventType eventType : EventType.values()) {

            SscsCaseData ccdResponse = SscsCaseDataUtils.buildBasicSscsCaseData(eventType);

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

        final ZonedDateTime firstHearingContactDate = ZonedDateTime.parse("2018-01-01T15:00:18Z[Europe/London]");
        final ZonedDateTime secondHearingContactDate = ZonedDateTime.parse("2018-01-01T16:00:18Z[Europe/London]");
        final ZonedDateTime thirdHearingContactDate = ZonedDateTime.parse("2018-01-01T17:00:18Z[Europe/London]");
        final ZonedDateTime finalHearingContactDate = ZonedDateTime.parse("2018-01-01T18:00:18Z[Europe/London]");

        final String expectedFirstJobGroup = "ID_FIRST_EVENT";
        final String expectedSecondJobGroup = "ID_SECOND_EVENT";
        final String expectedThirdJobGroup = "ID_THIRD_EVENT";
        final String expectedFinalJobGroup = "ID_FINAL_EVENT";

        SscsCaseData ccdResponse = SscsCaseDataUtils.buildBasicSscsCaseData(DWP_RESPONSE_RECEIVED);

        when(hearingContactDateExtractor.extractForReferenceEvent(ccdResponse, DWP_RESPONSE_RECEIVED))
            .thenReturn(Optional.of(firstHearingContactDate));

        when(hearingContactDateExtractor.extractForReferenceEvent(ccdResponse, FIRST_HEARING_HOLDING_REMINDER))
            .thenReturn(Optional.of(secondHearingContactDate));

        when(hearingContactDateExtractor.extractForReferenceEvent(ccdResponse, SECOND_HEARING_HOLDING_REMINDER))
            .thenReturn(Optional.of(thirdHearingContactDate));

        when(hearingContactDateExtractor.extractForReferenceEvent(ccdResponse, THIRD_HEARING_HOLDING_REMINDER))
            .thenReturn(Optional.of(finalHearingContactDate));

        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), FIRST_HEARING_HOLDING_REMINDER.getCcdType()))
            .thenReturn(expectedFirstJobGroup);

        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), SECOND_HEARING_HOLDING_REMINDER.getCcdType()))
            .thenReturn(expectedSecondJobGroup);

        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), THIRD_HEARING_HOLDING_REMINDER.getCcdType()))
            .thenReturn(expectedThirdJobGroup);

        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), FINAL_HEARING_HOLDING_REMINDER.getCcdType()))
            .thenReturn(expectedFinalJobGroup);

        hearingHoldingReminder.handle(ccdResponse);

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);

        verify(jobScheduler, times(4)).schedule(
            jobCaptor.capture()
        );

        Job<String> job1 = jobCaptor.getAllValues().get(0);
        assertEquals(expectedFirstJobGroup, job1.group);
        assertEquals(FIRST_HEARING_HOLDING_REMINDER.getCcdType(), job1.name);
        assertEquals(SscsCaseDataUtils.CASE_ID, job1.payload);
        assertEquals(firstHearingContactDate, job1.triggerAt);

        Job<String> job2 = jobCaptor.getAllValues().get(1);
        assertEquals(expectedSecondJobGroup, job2.group);
        assertEquals(SECOND_HEARING_HOLDING_REMINDER.getCcdType(), job2.name);
        assertEquals(SscsCaseDataUtils.CASE_ID, job2.payload);
        assertEquals(secondHearingContactDate, job2.triggerAt);

        Job<String> job3 = jobCaptor.getAllValues().get(2);
        assertEquals(expectedThirdJobGroup, job3.group);
        assertEquals(THIRD_HEARING_HOLDING_REMINDER.getCcdType(), job3.name);
        assertEquals(SscsCaseDataUtils.CASE_ID, job3.payload);
        assertEquals(thirdHearingContactDate, job3.triggerAt);

        Job<String> job4 = jobCaptor.getAllValues().get(3);
        assertEquals(expectedFinalJobGroup, job4.group);
        assertEquals(FINAL_HEARING_HOLDING_REMINDER.getCcdType(), job4.name);
        assertEquals(SscsCaseDataUtils.CASE_ID, job4.payload);
        assertEquals(finalHearingContactDate, job4.triggerAt);
    }

    @Test(expected = ReminderException.class)
    public void throwExceptionWhenHearingContactDateNotPresent() {

        SscsCaseData ccdResponse = SscsCaseDataUtils.buildBasicSscsCaseData(DWP_RESPONSE_RECEIVED);

        when(hearingContactDateExtractor.extractForReferenceEvent(ccdResponse, DWP_RESPONSE_RECEIVED))
            .thenReturn(Optional.empty());

        hearingHoldingReminder.handle(ccdResponse);
    }

}
