package uk.gov.hmcts.reform.sscs.service.reminder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_RECEIVED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DWP_RESPONSE_LATE_REMINDER_NOTIFICATION;

import java.time.ZonedDateTime;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.SscsCaseDataUtils;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.extractor.AppealReceivedDateExtractor;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;

@RunWith(MockitoJUnitRunner.class)
public class DwpResponseLateReminderTest {

    @Mock
    private AppealReceivedDateExtractor appealReceivedDateExtractor;
    @Mock
    private JobGroupGenerator jobGroupGenerator;
    @Mock
    private JobScheduler jobScheduler;

    private DwpResponseLateReminder dwpResponseLateReminder;

    @Before
    public void setup() {
        dwpResponseLateReminder = new DwpResponseLateReminder(
            appealReceivedDateExtractor,
            jobGroupGenerator,
            jobScheduler,
            86400
        );
    }

    @Test
    public void canHandleEvent() {

        for (NotificationEventType eventType : NotificationEventType.values()) {

            CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(eventType);

            if (eventType == APPEAL_RECEIVED_NOTIFICATION) {
                assertTrue(dwpResponseLateReminder.canHandle(wrapper));
            } else {

                assertFalse(dwpResponseLateReminder.canHandle(wrapper));
                assertThatThrownBy(() -> dwpResponseLateReminder.handle(wrapper))
                    .hasMessage("cannot handle ccdResponse")
                    .isExactlyInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Test
    public void scheduleDwpResponseLateReminderWhenDwpResponseNotReceivedInTime() {

        final String expectedJobGroup = "ID_EVENT";
        final String expectedTriggerAt = "2018-01-02T14:01:18Z[Europe/London]";
        ZonedDateTime appealReceivedDate = ZonedDateTime.parse("2018-01-01T14:01:18Z[Europe/London]");

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapperWithEvent(
            APPEAL_RECEIVED_NOTIFICATION,
            APPEAL_RECEIVED,
            appealReceivedDate.toString()
        );

        when(appealReceivedDateExtractor.extract(wrapper.getNewSscsCaseData())).thenReturn(Optional.of(appealReceivedDate));
        when(jobGroupGenerator.generate(wrapper.getCaseId(), DWP_RESPONSE_LATE_REMINDER_NOTIFICATION.getId())).thenReturn(expectedJobGroup);

        dwpResponseLateReminder.handle(wrapper);

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);

        verify(jobScheduler, times(1)).schedule(
            jobCaptor.capture()
        );

        Job<String> job = jobCaptor.getAllValues().get(0);
        assertEquals(expectedJobGroup, job.group);
        assertEquals(DWP_RESPONSE_LATE_REMINDER_NOTIFICATION.getId(), job.name);
        assertEquals(SscsCaseDataUtils.CASE_ID, job.payload);
        assertEquals(expectedTriggerAt, job.triggerAt.toString());
    }

    @Test(expected = Exception.class)
    public void canScheduleReturnFalseWhenAppealReceivedDateThrowError() {

        CcdNotificationWrapper wrapper = null;
        assertFalse(dwpResponseLateReminder.canSchedule(wrapper));
    }

    @Test
    public void canScheduleReturnFalseWhenAppealReceivedDateNotPresent() {

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(APPEAL_RECEIVED_NOTIFICATION);

        when(appealReceivedDateExtractor.extract(wrapper.getNewSscsCaseData())).thenReturn(Optional.empty());

        assertFalse(dwpResponseLateReminder.canSchedule(wrapper));
    }

}
