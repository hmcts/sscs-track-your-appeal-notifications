package uk.gov.hmcts.reform.sscs.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.exception.NotificationClientRuntimeException;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;
import uk.gov.hmcts.reform.sscs.service.reminder.JobGroupGenerator;
import uk.gov.service.notify.NotificationClientException;

public class NotificationHandlerTest {

    private static final NotificationEventType A_NOTIFICATION_THAT_CAN_TRIGGER_OUT_OF_HOURS = NotificationEventType.SYA_APPEAL_CREATED_NOTIFICATION;
    private static final NotificationEventType A_NOTIFICATION_THAT_CANNOT_TRIGGER_OUT_OF_HOURS = NotificationEventType.QUESTION_ROUND_ISSUED_NOTIFICATION;
    private OutOfHoursCalculator outOfHoursCalculator;
    private JobScheduler jobScheduler;
    private JobGroupGenerator jobGroupGenerator;
    private NotificationWrapper notificationWrapper;
    private NotificationHandler.SendNotification sendNotification;
    private NotificationHandler underTest;

    @Before
    public void setUp() {
        outOfHoursCalculator = mock(OutOfHoursCalculator.class);
        jobScheduler = mock(JobScheduler.class);
        jobGroupGenerator = mock(JobGroupGenerator.class);
        notificationWrapper = mock(NotificationWrapper.class);
        sendNotification = mock(NotificationHandler.SendNotification.class);

        underTest = new NotificationHandler(outOfHoursCalculator, jobScheduler, jobGroupGenerator);
    }

    @Test
    public void shouldSendNotificationIfNotificationTypeCanBeSentOutOfHouseAndItIsInHours() throws NotificationClientException {
        canSendNotification(A_NOTIFICATION_THAT_CAN_TRIGGER_OUT_OF_HOURS, false);
    }

    @Test
    public void shouldSendNotificationIfNotificationTypeCanBeSentOutOfHouseAndItIsOutOfHours() throws NotificationClientException {
        canSendNotification(A_NOTIFICATION_THAT_CAN_TRIGGER_OUT_OF_HOURS, true);
    }

    @Test
    public void shouldSendNotificationIfNotificationTypeCannotBeSentOutOfHouseAndItIsInHours() throws NotificationClientException {
        canSendNotification(A_NOTIFICATION_THAT_CANNOT_TRIGGER_OUT_OF_HOURS, false);
    }

    @Test
    public void canScheduleNotifications() {
        when(notificationWrapper.getNotificationType()).thenReturn(A_NOTIFICATION_THAT_CANNOT_TRIGGER_OUT_OF_HOURS);
        String payload = "payload";
        when(notificationWrapper.getSchedulerPayload()).thenReturn(payload);
        String caseId = "caseId";
        when(notificationWrapper.getCaseId()).thenReturn(caseId);

        ZonedDateTime whenToScheduleJob = ZonedDateTime.now();
        when(outOfHoursCalculator.getStartOfNextInHoursPeriod()).thenReturn(whenToScheduleJob);
        String group = "group";
        when(jobGroupGenerator.generate(caseId, A_NOTIFICATION_THAT_CANNOT_TRIGGER_OUT_OF_HOURS.getId())).thenReturn(group);

        underTest.scheduleNotification(notificationWrapper);
        ArgumentCaptor<Job> argument = ArgumentCaptor.forClass(Job.class);
        verify(jobScheduler).schedule(argument.capture());

        Job value = argument.getValue();
        assertThat(value.triggerAt, is(whenToScheduleJob));
        assertThat(value.group, is(group));
        assertThat(value.name, is(A_NOTIFICATION_THAT_CANNOT_TRIGGER_OUT_OF_HOURS.getId()));
        assertThat(value.payload, is(payload));
    }

    private void canSendNotification(NotificationEventType notificationEventType, boolean isOutOfHours) throws NotificationClientException {
        when(notificationWrapper.getNotificationType()).thenReturn(notificationEventType);
        when(outOfHoursCalculator.isItOutOfHours()).thenReturn(isOutOfHours);

        underTest.sendNotification(notificationWrapper, "someTemplate", "Email", sendNotification);

        verify(sendNotification).send();
    }

    @Test(expected = NotificationClientRuntimeException.class)
    public void shouldThrowNotificationClientRuntimeExceptionForAnyNotificationException() throws Exception {
        when(notificationWrapper.getNotificationType()).thenReturn(A_NOTIFICATION_THAT_CAN_TRIGGER_OUT_OF_HOURS);
        when(outOfHoursCalculator.isItOutOfHours()).thenReturn(false);
        doThrow(new NotificationClientException(new UnknownHostException()))
                .when(sendNotification)
                .send();

        underTest.sendNotification(notificationWrapper, "someTemplate", "Email", sendNotification);
    }

    @Test
    public void shouldContinueAndHandleAGovNotifyException() throws Exception {
        when(notificationWrapper.getNotificationType()).thenReturn(A_NOTIFICATION_THAT_CAN_TRIGGER_OUT_OF_HOURS);
        when(outOfHoursCalculator.isItOutOfHours()).thenReturn(false);
        doThrow(new NotificationClientException(new RuntimeException()))
                .when(sendNotification)
                .send();

        underTest.sendNotification(notificationWrapper, "someTemplate", "Email", sendNotification);
    }
}
