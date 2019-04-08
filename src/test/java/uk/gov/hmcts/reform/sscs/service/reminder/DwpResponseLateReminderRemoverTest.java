package uk.gov.hmcts.reform.sscs.service.reminder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.SscsCaseDataUtils;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
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

        for (NotificationEventType eventType : NotificationEventType.values()) {

            CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(eventType);

            if (Arrays.asList(
                APPEAL_DORMANT_NOTIFICATION,
                APPEAL_LAPSED_NOTIFICATION,
                APPEAL_WITHDRAWN_NOTIFICATION,
                DWP_RESPONSE_RECEIVED_NOTIFICATION
            ).contains(eventType)) {
                assertTrue(dwpResponseLateReminderRemover.canHandle(wrapper));
            } else {

                assertFalse(dwpResponseLateReminderRemover.canHandle(wrapper));
                assertThatThrownBy(() -> dwpResponseLateReminderRemover.handle(wrapper))
                    .hasMessage("cannot handle ccdResponse")
                    .isExactlyInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Test
    public void removeDwpResponseLateReminderWhenDwpRespond() {

        final String expectedJobGroup = "ID_EVENT";

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(
            DWP_RESPONSE_RECEIVED_NOTIFICATION
        );

        when(jobGroupGenerator.generate(wrapper.getCaseId(), DWP_RESPONSE_LATE_REMINDER_NOTIFICATION.getId())).thenReturn(expectedJobGroup);

        dwpResponseLateReminderRemover.handle(wrapper);

        verify(jobRemover, times(1)).removeGroup(
            expectedJobGroup
        );
    }

    @Test
    public void removeDwpResponseLateReminderWhenAppealDormant() {

        final String expectedJobGroup = "ID_EVENT";

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(
            APPEAL_DORMANT_NOTIFICATION
        );

        when(jobGroupGenerator.generate(wrapper.getCaseId(), DWP_RESPONSE_LATE_REMINDER_NOTIFICATION.getId())).thenReturn(expectedJobGroup);

        dwpResponseLateReminderRemover.handle(wrapper);

        verify(jobRemover, times(1)).removeGroup(
            expectedJobGroup
        );
    }

    @Test
    public void removeDwpResponseLateReminderWhenAppealWithdrawn() {

        final String expectedJobGroup = "ID_EVENT";

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(
            APPEAL_WITHDRAWN_NOTIFICATION
        );

        when(jobGroupGenerator.generate(wrapper.getCaseId(), DWP_RESPONSE_LATE_REMINDER_NOTIFICATION.getId())).thenReturn(expectedJobGroup);

        dwpResponseLateReminderRemover.handle(wrapper);

        verify(jobRemover, times(1)).removeGroup(
            expectedJobGroup
        );
    }

    @Test
    public void removeDwpResponseLateReminderWhenAppealLapse() {

        final String expectedJobGroup = "ID_EVENT";

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(
            APPEAL_LAPSED_NOTIFICATION
        );

        when(jobGroupGenerator.generate(wrapper.getCaseId(), DWP_RESPONSE_LATE_REMINDER_NOTIFICATION.getId())).thenReturn(expectedJobGroup);

        dwpResponseLateReminderRemover.handle(wrapper);

        verify(jobRemover, times(1)).removeGroup(
            expectedJobGroup
        );
    }

    @Test
    public void doesNotThrowExceptionWhenCannotFindReminder() {

        final String notExistantJobGroup = "NOT_EXISTANT";

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(
            DWP_RESPONSE_RECEIVED_NOTIFICATION
        );

        when(jobGroupGenerator.generate(wrapper.getCaseId(), DWP_RESPONSE_LATE_REMINDER_NOTIFICATION.getId())).thenReturn(notExistantJobGroup);

        doThrow(JobNotFoundException.class)
            .when(jobRemover)
            .removeGroup(notExistantJobGroup);

        dwpResponseLateReminderRemover.handle(wrapper);

        verify(jobRemover, times(1)).removeGroup(
            notExistantJobGroup
        );
    }

    @Test
    public void canScheduleReturnAlwaysTrue() {

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(APPEAL_RECEIVED_NOTIFICATION);

        assertTrue(dwpResponseLateReminderRemover.canSchedule(wrapper));
    }

}
