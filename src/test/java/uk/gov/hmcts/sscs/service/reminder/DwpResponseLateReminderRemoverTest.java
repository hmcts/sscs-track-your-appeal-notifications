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
public class DwpResponseLateReminderRemoverTest {

    @Mock
    private JobGroupGenerator jobGroupGenerator;
    @Mock
    private JobRemover jobRemover;

    private DwpResponseLateReminderRemover dwpResponseLateReminderRemover;

    @Before
    public void setup() {
        dwpResponseLateReminderRemover = new DwpResponseLateReminderRemover(
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
                DWP_RESPONSE_RECEIVED
            ).contains(eventType)) {
                assertTrue(dwpResponseLateReminderRemover.canHandle(ccdResponse));
            } else {

                assertFalse(dwpResponseLateReminderRemover.canHandle(ccdResponse));
                assertThatThrownBy(() -> dwpResponseLateReminderRemover.handle(ccdResponse))
                    .hasMessage("cannot handle ccdResponse")
                    .isExactlyInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Test
    public void removeDwpResponseLateReminderWhenDwpRespond() {

        final String expectedJobGroup = "ID_EVENT";

        CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponse(
            DWP_RESPONSE_RECEIVED
        );

        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), DWP_RESPONSE_LATE_REMINDER.getId())).thenReturn(expectedJobGroup);

        dwpResponseLateReminderRemover.handle(ccdResponse);

        verify(jobRemover, times(1)).removeGroup(
            expectedJobGroup
        );
    }

    @Test
    public void removeDwpResponseLateReminderWhenAppealDormant() {

        final String expectedJobGroup = "ID_EVENT";

        CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponse(
            APPEAL_DORMANT
        );

        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), DWP_RESPONSE_LATE_REMINDER.getId())).thenReturn(expectedJobGroup);

        dwpResponseLateReminderRemover.handle(ccdResponse);

        verify(jobRemover, times(1)).removeGroup(
            expectedJobGroup
        );
    }

    @Test
    public void removeDwpResponseLateReminderWhenAppealWithdrawn() {

        final String expectedJobGroup = "ID_EVENT";

        CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponse(
            APPEAL_WITHDRAWN
        );

        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), DWP_RESPONSE_LATE_REMINDER.getId())).thenReturn(expectedJobGroup);

        dwpResponseLateReminderRemover.handle(ccdResponse);

        verify(jobRemover, times(1)).removeGroup(
            expectedJobGroup
        );
    }

    @Test
    public void removeDwpResponseLateReminderWhenAppealLapse() {

        final String expectedJobGroup = "ID_EVENT";

        CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponse(
            APPEAL_LAPSED
        );

        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), DWP_RESPONSE_LATE_REMINDER.getId())).thenReturn(expectedJobGroup);

        dwpResponseLateReminderRemover.handle(ccdResponse);

        verify(jobRemover, times(1)).removeGroup(
            expectedJobGroup
        );
    }

    @Test
    public void doesNotThrowExceptionWhenCannotFindReminder() {

        final String notExistantJobGroup = "NOT_EXISTANT";

        CcdResponse ccdResponse = CcdResponseUtils.buildBasicCcdResponse(
            DWP_RESPONSE_RECEIVED
        );

        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), DWP_RESPONSE_LATE_REMINDER.getId())).thenReturn(notExistantJobGroup);

        doThrow(JobNotFoundException.class)
            .when(jobRemover)
            .removeGroup(notExistantJobGroup);

        dwpResponseLateReminderRemover.handle(ccdResponse);

        verify(jobRemover, times(1)).removeGroup(
            notExistantJobGroup
        );
    }

}
