package uk.gov.hmcts.sscs.service.reminder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.sscs.domain.notify.EventType.HEARING_REMINDER;
import static uk.gov.hmcts.sscs.domain.notify.EventType.POSTPONEMENT;

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
public class HearingReminderRemoverTest {

    @Mock
    private JobGroupGenerator jobGroupGenerator;
    @Mock
    private JobRemover jobRemover;

    private HearingReminderRemover hearingReminderRemoverTest;

    @Before
    public void setup() {
        hearingReminderRemoverTest = new HearingReminderRemover(
            jobGroupGenerator,
            jobRemover
        );
    }

    @Test
    public void canHandleEvent() {

        for (EventType eventType : EventType.values()) {

            CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponse(eventType);

            if (eventType == POSTPONEMENT) {
                assertTrue(hearingReminderRemoverTest.canHandle(ccdResponse));
            } else {

                assertFalse(hearingReminderRemoverTest.canHandle(ccdResponse));
                assertThatThrownBy(() -> hearingReminderRemoverTest.handle(ccdResponse))
                    .hasMessage("cannot handle ccdResponse")
                    .isExactlyInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Test
    public void removedHearingReminder() {

        final String expectedJobGroup = "ID_EVENT";

        String hearingDate = "2018-01-01";
        String hearingTime = "14:01:18";

        CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponseWithHearing(
            POSTPONEMENT,
            hearingDate,
            hearingTime
        );

        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), HEARING_REMINDER.getId())).thenReturn(expectedJobGroup);

        hearingReminderRemoverTest.handle(ccdResponse);

        verify(jobRemover, times(1)).removeGroup(
            expectedJobGroup
        );
    }

    @Test
    public void doesNotThrowExceptionWhenCannotFindReminder() {

        final String expectedJobGroup = "NOT_EXISTANT";

        String hearingDate = "2018-01-01";
        String hearingTime = "14:01:18";

        CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponseWithHearing(
            POSTPONEMENT,
            hearingDate,
            hearingTime
        );

        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), HEARING_REMINDER.getId())).thenReturn(expectedJobGroup);

        doThrow(JobNotFoundException.class)
            .when(jobRemover)
            .removeGroup(expectedJobGroup);

        hearingReminderRemoverTest.handle(ccdResponse);

        verify(jobRemover, times(1)).removeGroup(
            expectedJobGroup
        );
    }

}
