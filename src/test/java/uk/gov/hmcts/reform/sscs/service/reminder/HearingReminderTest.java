package uk.gov.hmcts.reform.sscs.service.reminder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.HEARING_BOOKED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.HEARING_REMINDER_NOTIFICATION;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.SscsCaseDataUtils;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;

@RunWith(MockitoJUnitRunner.class)
public class HearingReminderTest {

    @Mock
    private JobGroupGenerator jobGroupGenerator;
    @Mock
    private JobScheduler jobScheduler;

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

        for (NotificationEventType eventType : NotificationEventType.values()) {

            CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(eventType);

            if (eventType == HEARING_BOOKED_NOTIFICATION) {
                assertTrue(hearingReminder.canHandle(wrapper));
            } else {

                assertFalse(hearingReminder.canHandle(wrapper));
                assertThatThrownBy(() -> hearingReminder.handle(wrapper))
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

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapperWithHearing(
            HEARING_BOOKED_NOTIFICATION,
            hearingDate,
            hearingTime
        );

        when(jobGroupGenerator.generate(wrapper.getCaseId(), HEARING_REMINDER_NOTIFICATION.getId())).thenReturn(expectedJobGroup);

        hearingReminder.handle(wrapper);

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);

        verify(jobScheduler, times(2)).schedule(
            jobCaptor.capture()
        );

        Job<String> firstJob = jobCaptor.getAllValues().get(0);
        assertEquals(expectedJobGroup, firstJob.group);
        assertEquals(HEARING_REMINDER_NOTIFICATION.getId(), firstJob.name);
        assertEquals(SscsCaseDataUtils.CASE_ID, firstJob.payload);
        assertEquals(expectedFirstTriggerAt, firstJob.triggerAt.toString());

        Job<String> secondJob = jobCaptor.getAllValues().get(1);
        assertEquals(expectedJobGroup, secondJob.group);
        assertEquals(HEARING_REMINDER_NOTIFICATION.getId(), secondJob.name);
        assertEquals(SscsCaseDataUtils.CASE_ID, secondJob.payload);
        assertEquals(expectedSecondTriggerAt, secondJob.triggerAt.toString());
    }

    @Test
    public void canScheduleReturnFalseWhenCannotFindHearingDate() {

        CcdNotificationWrapper ccdResponse = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(HEARING_BOOKED_NOTIFICATION);

        assertFalse(hearingReminder.canSchedule(ccdResponse));
    }

}
