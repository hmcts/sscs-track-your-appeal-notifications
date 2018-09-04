package uk.gov.hmcts.reform.sscs.service.reminder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;

import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.SscsCaseDataUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobNotFoundException;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobRemover;

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

            SscsCaseData ccdResponse = SscsCaseDataUtils.buildBasicSscsCaseData(eventType);

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

        final String expectedFirstJobGroup = "ID_FIRST_EVENT";
        final String expectedSecondJobGroup = "ID_SECOND_EVENT";
        final String expectedThirdJobGroup = "ID_THIRD_EVENT";
        final String expectedFinalJobGroup = "ID_FINAL_EVENT";

        String hearingDate = "2018-01-01";
        String hearingTime = "14:01:18";

        SscsCaseData ccdResponse = SscsCaseDataUtils.buildBasicSscsCaseDataWithHearing(
            HEARING_BOOKED,
            hearingDate,
            hearingTime
        );

        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), FIRST_HEARING_HOLDING_REMINDER.getCcdType())).thenReturn(expectedFirstJobGroup);
        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), SECOND_HEARING_HOLDING_REMINDER.getCcdType())).thenReturn(expectedSecondJobGroup);
        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), THIRD_HEARING_HOLDING_REMINDER.getCcdType())).thenReturn(expectedThirdJobGroup);
        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), FINAL_HEARING_HOLDING_REMINDER.getCcdType())).thenReturn(expectedFinalJobGroup);

        hearingHoldingReminderRemoverTest.handle(ccdResponse);

        verify(jobRemover, times(1)).removeGroup(
            expectedFirstJobGroup
        );

        verify(jobRemover, times(1)).removeGroup(
            expectedSecondJobGroup
        );

        verify(jobRemover, times(1)).removeGroup(
            expectedThirdJobGroup
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

        SscsCaseData ccdResponse = SscsCaseDataUtils.buildBasicSscsCaseDataWithHearing(
            HEARING_BOOKED,
            hearingDate,
            hearingTime
        );

        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), FIRST_HEARING_HOLDING_REMINDER.getCcdType())).thenReturn(notExistantJobGroup);
        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), SECOND_HEARING_HOLDING_REMINDER.getCcdType())).thenReturn(notExistantJobGroup);
        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), THIRD_HEARING_HOLDING_REMINDER.getCcdType())).thenReturn(notExistantJobGroup);
        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), FINAL_HEARING_HOLDING_REMINDER.getCcdType())).thenReturn(notExistantJobGroup);

        doThrow(JobNotFoundException.class)
            .when(jobRemover)
            .removeGroup(notExistantJobGroup);

        hearingHoldingReminderRemoverTest.handle(ccdResponse);

        verify(jobRemover, times(4)).removeGroup(
            notExistantJobGroup
        );
    }

}
