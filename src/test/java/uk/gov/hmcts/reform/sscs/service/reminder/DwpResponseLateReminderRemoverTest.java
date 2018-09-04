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

            SscsCaseData ccdResponse = SscsCaseDataUtils.buildBasicSscsCaseData(eventType);

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

        SscsCaseData ccdResponse = SscsCaseDataUtils.buildBasicSscsCaseData(
            DWP_RESPONSE_RECEIVED
        );

        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), DWP_RESPONSE_LATE_REMINDER.getCcdType())).thenReturn(expectedJobGroup);

        dwpResponseLateReminderRemover.handle(ccdResponse);

        verify(jobRemover, times(1)).removeGroup(
            expectedJobGroup
        );
    }

    @Test
    public void removeDwpResponseLateReminderWhenAppealDormant() {

        final String expectedJobGroup = "ID_EVENT";

        SscsCaseData ccdResponse = SscsCaseDataUtils.buildBasicSscsCaseData(
            APPEAL_DORMANT
        );

        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), DWP_RESPONSE_LATE_REMINDER.getCcdType())).thenReturn(expectedJobGroup);

        dwpResponseLateReminderRemover.handle(ccdResponse);

        verify(jobRemover, times(1)).removeGroup(
            expectedJobGroup
        );
    }

    @Test
    public void removeDwpResponseLateReminderWhenAppealWithdrawn() {

        final String expectedJobGroup = "ID_EVENT";

        SscsCaseData ccdResponse = SscsCaseDataUtils.buildBasicSscsCaseData(
            APPEAL_WITHDRAWN
        );

        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), DWP_RESPONSE_LATE_REMINDER.getCcdType())).thenReturn(expectedJobGroup);

        dwpResponseLateReminderRemover.handle(ccdResponse);

        verify(jobRemover, times(1)).removeGroup(
            expectedJobGroup
        );
    }

    @Test
    public void removeDwpResponseLateReminderWhenAppealLapse() {

        final String expectedJobGroup = "ID_EVENT";

        SscsCaseData ccdResponse = SscsCaseDataUtils.buildBasicSscsCaseData(
            APPEAL_LAPSED
        );

        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), DWP_RESPONSE_LATE_REMINDER.getCcdType())).thenReturn(expectedJobGroup);

        dwpResponseLateReminderRemover.handle(ccdResponse);

        verify(jobRemover, times(1)).removeGroup(
            expectedJobGroup
        );
    }

    @Test
    public void doesNotThrowExceptionWhenCannotFindReminder() {

        final String notExistantJobGroup = "NOT_EXISTANT";

        SscsCaseData ccdResponse = SscsCaseDataUtils.buildBasicSscsCaseData(
            DWP_RESPONSE_RECEIVED
        );

        when(jobGroupGenerator.generate(ccdResponse.getCaseId(), DWP_RESPONSE_LATE_REMINDER.getCcdType())).thenReturn(notExistantJobGroup);

        doThrow(JobNotFoundException.class)
            .when(jobRemover)
            .removeGroup(notExistantJobGroup);

        dwpResponseLateReminderRemover.handle(ccdResponse);

        verify(jobRemover, times(1)).removeGroup(
            notExistantJobGroup
        );
    }

}
