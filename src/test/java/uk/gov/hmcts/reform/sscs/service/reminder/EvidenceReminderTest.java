package uk.gov.hmcts.reform.sscs.service.reminder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DWP_RESPOND;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DWP_RESPONSE_RECEIVED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.EVIDENCE_REMINDER_NOTIFICATION;

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
import uk.gov.hmcts.reform.sscs.exception.ReminderException;
import uk.gov.hmcts.reform.sscs.extractor.DwpResponseReceivedDateExtractor;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;

@RunWith(MockitoJUnitRunner.class)
public class EvidenceReminderTest {

    @Mock
    private DwpResponseReceivedDateExtractor dwpResponseReceivedDateExtractor;
    @Mock
    private JobGroupGenerator jobGroupGenerator;
    @Mock
    private JobScheduler jobScheduler;

    private EvidenceReminder evidenceReminder;

    @Before
    public void setup() {
        evidenceReminder = new EvidenceReminder(
            dwpResponseReceivedDateExtractor,
            jobGroupGenerator,
            jobScheduler,
            172800
        );
    }

    @Test
    public void canHandleEvent() {

        for (NotificationEventType eventType : NotificationEventType.values()) {

            CcdNotificationWrapper ccdResponse = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(eventType);

            if (eventType == DWP_RESPONSE_RECEIVED_NOTIFICATION) {
                assertTrue(evidenceReminder.canHandle(ccdResponse));
            } else {

                assertFalse(evidenceReminder.canHandle(ccdResponse));
                assertThatThrownBy(() -> evidenceReminder.handle(ccdResponse))
                    .hasMessage("cannot handle ccdResponse")
                    .isExactlyInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Test
    public void schedulesReminder() {

        final String expectedJobGroup = "ID_EVENT";
        final String expectedTriggerAt = "2018-01-03T14:01:18Z[Europe/London]";

        ZonedDateTime dwpResponseReceivedDate = ZonedDateTime.parse("2018-01-01T14:01:18Z[Europe/London]");

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapperWithEvent(
            DWP_RESPONSE_RECEIVED_NOTIFICATION,
            DWP_RESPOND,
            dwpResponseReceivedDate.toString()
        );

        when(dwpResponseReceivedDateExtractor.extract(wrapper.getNewSscsCaseData())).thenReturn(Optional.of(dwpResponseReceivedDate));
        when(jobGroupGenerator.generate(wrapper.getCaseId(), EVIDENCE_REMINDER_NOTIFICATION.getId())).thenReturn(expectedJobGroup);

        evidenceReminder.handle(wrapper);

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);

        verify(jobScheduler, times(1)).schedule(
            jobCaptor.capture()
        );

        Job<String> job = jobCaptor.getValue();
        assertEquals(expectedJobGroup, job.group);
        assertEquals(EVIDENCE_REMINDER_NOTIFICATION.getId(), job.name);
        assertEquals(SscsCaseDataUtils.CASE_ID, job.payload);
        assertEquals(expectedTriggerAt, job.triggerAt.toString());
    }

    @Test(expected = ReminderException.class)
    public void throwExceptionWhenDwpResponseReceivedDateNotPresent() {

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(DWP_RESPONSE_RECEIVED_NOTIFICATION);

        when(dwpResponseReceivedDateExtractor.extract(wrapper.getNewSscsCaseData())).thenReturn(Optional.empty());

        evidenceReminder.handle(wrapper);
    }

}
