package uk.gov.hmcts.sscs.service.reminder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.sscs.domain.notify.EventType.*;

import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobNotFoundException;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobRemover;
import uk.gov.hmcts.sscs.CcdResponseUtils;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.notify.EventType;

@RunWith(MockitoJUnitRunner.class)
public class HearingHoldingReminderRemoverTest {

    @Mock
    private JobGroupGenerator jobGroupGenerator;
    @Mock
    private JobRemover jobRemover;

    private HearingHoldingReminderRemover hearingHoldingReminderRemoverTest;

    @Before
    public void setup() {
        hearingHoldingReminderRemoverTest = new HearingHoldingReminderRemover(
            jobGroupGenerator,
            jobRemover
        );
    }

    @Test
    public void canHandleEvent() {

        for (EventType eventType : EventType.values()) {

            CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponse(eventType);

            if (Arrays.asList(
                APPEAL_DORMANT,
                APPEAL_LAPSED,
                APPEAL_WITHDRAWN,
                HEARING_BOOKED
            ).contains(eventType)) {
                assertTrue(hearingHoldingReminderRemoverTest.canHandle(ccdResponse));
            } else {

                assertFalse(hearingHoldingReminderRemoverTest.canHandle(ccdResponse));
                assertThatThrownBy(() -> hearingHoldingReminderRemoverTest.handle(ccdResponse))
                    .hasMessage("cannot handle ccdResponse")
                    .isExactlyInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Test
    public void removeHearingHoldingRemindersWhenHearingBooked() {

        final String expectedInterimJobGroup = "ID_EVENT";
        final String expectedFinalJobGroup = "ID_FINAL_EVENT";

        String hearingDate = "2018-01-01";
        String hearingTime = "14:01:18";

        CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponseWithHearing(
            HEARING_BOOKED,
            hearingDate,
            hearingTime
        );

        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), HEARING_HOLDING_REMINDER.getId())).thenReturn(expectedInterimJobGroup);
        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), FINAL_HEARING_HOLDING_REMINDER.getId())).thenReturn(expectedFinalJobGroup);

        hearingHoldingReminderRemoverTest.handle(ccdResponse);

        verify(jobRemover, times(1)).removeGroup(
            expectedInterimJobGroup
        );

        verify(jobRemover, times(1)).removeGroup(
            expectedFinalJobGroup
        );
    }

    @Test
    public void doesNotThrowExceptionWhenCannotFindInterimReminder() {

        final String notExistantJobGroup = "NOT_EXISTANT";

        String hearingDate = "2018-01-01";
        String hearingTime = "14:01:18";

        CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponseWithHearing(
            HEARING_BOOKED,
            hearingDate,
            hearingTime
        );

        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), HEARING_HOLDING_REMINDER.getId())).thenReturn(notExistantJobGroup);
        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), FINAL_HEARING_HOLDING_REMINDER.getId())).thenReturn(notExistantJobGroup);

        doThrow(JobNotFoundException.class)
            .when(jobRemover)
            .removeGroup(notExistantJobGroup);

        hearingHoldingReminderRemoverTest.handle(ccdResponse);

        verify(jobRemover, times(2)).removeGroup(
            notExistantJobGroup
        );
    }

}
