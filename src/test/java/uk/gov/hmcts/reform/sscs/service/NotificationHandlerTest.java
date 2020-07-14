package uk.gov.hmcts.reform.sscs.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.service.NotificationServiceTest.verifyExpectedErrorLogMessage;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.exception.NotificationClientRuntimeException;
import uk.gov.hmcts.reform.sscs.exception.NotificationServiceException;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;
import uk.gov.hmcts.reform.sscs.service.reminder.JobGroupGenerator;
import uk.gov.service.notify.NotificationClientException;

@RunWith(MockitoJUnitRunner.class)
public class NotificationHandlerTest {

    private static final NotificationEventType A_NOTIFICATION_THAT_CAN_TRIGGER_OUT_OF_HOURS = NotificationEventType.SYA_APPEAL_CREATED_NOTIFICATION;
    private static final NotificationEventType A_NOTIFICATION_THAT_CANNOT_TRIGGER_OUT_OF_HOURS = NotificationEventType.QUESTION_ROUND_ISSUED_NOTIFICATION;
    @Mock
    private OutOfHoursCalculator outOfHoursCalculator;
    @Mock
    private JobScheduler jobScheduler;
    @Mock
    private JobGroupGenerator jobGroupGenerator;
    @Mock
    private NotificationWrapper notificationWrapper;
    @Mock
    private NotificationHandler.SendNotification sendNotification;
    private NotificationHandler underTest;

    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Captor
    private ArgumentCaptor<ILoggingEvent> captorLoggingEvent;

    @Before
    public void setUp() {
        underTest = new NotificationHandler(outOfHoursCalculator, jobScheduler, jobGroupGenerator);

        Logger logger = (Logger) LoggerFactory.getLogger(NotificationHandler.class.getName());
        logger.addAppender(mockAppender);
    }

    @Test
    public void shouldSendNotificationIfNotificationTypeCanBeSentOutOfHouseAndItIsInHours() throws NotificationClientException {
        canSendNotification(A_NOTIFICATION_THAT_CAN_TRIGGER_OUT_OF_HOURS, false);
    }

    @Test
    public void shouldSendValidAppealCreatedNotificationIfNotificationTypeCanBeSentOutOfHouseAndItIsInHours() throws NotificationClientException {
        canSendNotification(NotificationEventType.VALID_APPEAL_CREATED, false);
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

    @Test
    public void canScheduleNotificationsAtASpecifiedTime() {
        when(notificationWrapper.getNotificationType()).thenReturn(A_NOTIFICATION_THAT_CANNOT_TRIGGER_OUT_OF_HOURS);
        String payload = "payload";
        when(notificationWrapper.getSchedulerPayload()).thenReturn(payload);
        String caseId = "caseId";
        when(notificationWrapper.getCaseId()).thenReturn(caseId);

        ZonedDateTime whenToScheduleJob = ZonedDateTime.now();
        String group = "group";
        when(jobGroupGenerator.generate(caseId, A_NOTIFICATION_THAT_CANNOT_TRIGGER_OUT_OF_HOURS.getId())).thenReturn(group);

        underTest.scheduleNotification(notificationWrapper, whenToScheduleJob);
        ArgumentCaptor<Job> argument = ArgumentCaptor.forClass(Job.class);
        verify(jobScheduler).schedule(argument.capture());

        Job value = argument.getValue();
        assertThat(value.triggerAt, is(whenToScheduleJob));
        assertThat(value.group, is(group));
        assertThat(value.name, is(A_NOTIFICATION_THAT_CANNOT_TRIGGER_OUT_OF_HOURS.getId()));
        assertThat(value.payload, is(payload));
    }

    private void canSendNotification(NotificationEventType notificationEventType, boolean isOutOfHours) throws NotificationClientException {
        underTest.sendNotification(notificationWrapper, "someTemplate", "Email", sendNotification);

        verify(sendNotification).send();
    }

    @Test(expected = NotificationClientRuntimeException.class)
    public void shouldThrowNotificationClientRuntimeExceptionForAnyNotificationException() throws Exception {
        doThrow(new NotificationClientException(new UnknownHostException()))
                .when(sendNotification)
                .send();

        underTest.sendNotification(notificationWrapper, "someTemplate", "Email", sendNotification);

        verifyExpectedErrorLogMessage(mockAppender, captorLoggingEvent, notificationWrapper.getNewSscsCaseData().getCcdCaseId(), "Could not send notification for case id:");
    }

    @Test(expected = NotificationServiceException.class)
    public void shouldNotContinueWithAGovNotifyException() throws Exception {
        String caseId = "123";
        when(notificationWrapper.getCaseId()).thenReturn(caseId);
        SscsCaseData stubbedCaseData = SscsCaseData.builder().ccdCaseId(caseId).build();
        when(notificationWrapper.getNewSscsCaseData()).thenReturn(stubbedCaseData);
        doThrow(new NotificationClientException(new RuntimeException()))
                .when(sendNotification)
                .send();

        try {
            underTest.sendNotification(notificationWrapper, "someTemplate", "Email", sendNotification);
        } catch (Throwable throwable) {
            verifyExpectedErrorLogMessage(mockAppender, captorLoggingEvent, notificationWrapper.getNewSscsCaseData().getCcdCaseId(), "Could not send notification for case id:");
            throw throwable;
        }
    }

    @Test
    public void shouldContinueAndHandleAnyOtherException() throws Exception {
        String caseId = "123";
        when(notificationWrapper.getCaseId()).thenReturn(caseId);
        SscsCaseData stubbedCaseData = SscsCaseData.builder().ccdCaseId(caseId).build();
        when(notificationWrapper.getNewSscsCaseData()).thenReturn(stubbedCaseData);
        doThrow(new RuntimeException())
                .when(sendNotification)
                .send();

        underTest.sendNotification(notificationWrapper, "someTemplate", "Email", sendNotification);
        verifyExpectedErrorLogMessage(mockAppender, captorLoggingEvent, notificationWrapper.getNewSscsCaseData().getCcdCaseId(), "Could not send notification for case id:");
    }
}
