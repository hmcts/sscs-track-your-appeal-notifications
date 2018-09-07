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
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.SscsCaseDataUtils;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
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

        for (NotificationEventType eventType : NotificationEventType.values()) {

            CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(eventType);

            if (Arrays.asList(
                APPEAL_DORMANT_NOTIFICATION,
                APPEAL_LAPSED_NOTIFICATION,
                APPEAL_WITHDRAWN_NOTIFICATION,
                HEARING_BOOKED_NOTIFICATION
            ).contains(eventType)) {
                assertTrue(hearingHoldingReminderRemoverTest.canHandle(wrapper));
            } else {

                assertFalse(hearingHoldingReminderRemoverTest.canHandle(wrapper));
                assertThatThrownBy(() -> hearingHoldingReminderRemoverTest.handle(wrapper))
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

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapperWithHearing(
            HEARING_BOOKED_NOTIFICATION,
            hearingDate,
            hearingTime
        );

        when(jobGroupGenerator.generate(wrapper.getCaseId(), FIRST_HEARING_HOLDING_REMINDER_NOTIFICATION.getId())).thenReturn(expectedFirstJobGroup);
        when(jobGroupGenerator.generate(wrapper.getCaseId(), SECOND_HEARING_HOLDING_REMINDER_NOTIFICATION.getId())).thenReturn(expectedSecondJobGroup);
        when(jobGroupGenerator.generate(wrapper.getCaseId(), THIRD_HEARING_HOLDING_REMINDER_NOTIFICATION.getId())).thenReturn(expectedThirdJobGroup);
        when(jobGroupGenerator.generate(wrapper.getCaseId(), FINAL_HEARING_HOLDING_REMINDER_NOTIFICATION.getId())).thenReturn(expectedFinalJobGroup);

        hearingHoldingReminderRemoverTest.handle(wrapper);

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

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapperWithHearing(
            HEARING_BOOKED_NOTIFICATION,
            hearingDate,
            hearingTime
        );

        when(jobGroupGenerator.generate(wrapper.getCaseId(), FIRST_HEARING_HOLDING_REMINDER_NOTIFICATION.getId())).thenReturn(notExistantJobGroup);
        when(jobGroupGenerator.generate(wrapper.getCaseId(), SECOND_HEARING_HOLDING_REMINDER_NOTIFICATION.getId())).thenReturn(notExistantJobGroup);
        when(jobGroupGenerator.generate(wrapper.getCaseId(), THIRD_HEARING_HOLDING_REMINDER_NOTIFICATION.getId())).thenReturn(notExistantJobGroup);
        when(jobGroupGenerator.generate(wrapper.getCaseId(), FINAL_HEARING_HOLDING_REMINDER_NOTIFICATION.getId())).thenReturn(notExistantJobGroup);

        doThrow(JobNotFoundException.class)
            .when(jobRemover)
            .removeGroup(notExistantJobGroup);

        hearingHoldingReminderRemoverTest.handle(wrapper);

        verify(jobRemover, times(4)).removeGroup(
            notExistantJobGroup
        );
    }

}
