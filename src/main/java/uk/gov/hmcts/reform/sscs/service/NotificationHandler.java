package uk.gov.hmcts.reform.sscs.service;

import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.exception.NotificationClientRuntimeException;
import uk.gov.hmcts.reform.sscs.exception.NotificationServiceException;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;
import uk.gov.hmcts.reform.sscs.service.reminder.JobGroupGenerator;
import uk.gov.service.notify.NotificationClientException;

@Service
@Slf4j
public class NotificationHandler {

    private final OutOfHoursCalculator outOfHoursCalculator;
    private final JobScheduler jobScheduler;
    private final JobGroupGenerator jobGroupGenerator;

    @Autowired
    public NotificationHandler(OutOfHoursCalculator outOfHoursCalculator, JobScheduler jobScheduler, JobGroupGenerator jobGroupGenerator) {
        this.outOfHoursCalculator = outOfHoursCalculator;
        this.jobScheduler = jobScheduler;
        this.jobGroupGenerator = jobGroupGenerator;
    }

    public boolean sendNotification(NotificationWrapper wrapper, String notificationTemplate, final String notificationType, SendNotification sendNotification) {
        final String caseId = wrapper.getCaseId();
        try {
            log.info("Sending {} template {} for case id: {}", notificationType, notificationTemplate, caseId);
            sendNotification.send();
            log.info("{} template {} sent for case id: {}", notificationType, notificationTemplate, caseId);

            return true;
        } catch (Exception ex) {
            log.error("Could not send notification for case id: ", wrapper.getCaseId());
            wrapAndThrowNotificationExceptionIfRequired(caseId, notificationTemplate, ex);
        }

        return false;
    }

    public void scheduleNotification(NotificationWrapper wrapper) {
        scheduleNotification(wrapper, outOfHoursCalculator.getStartOfNextInHoursPeriod());
    }

    public void scheduleNotification(NotificationWrapper wrapper, ZonedDateTime dateTime) {
        final String caseId = wrapper.getCaseId();
        String eventId = wrapper.getNotificationType().getId();
        String jobGroup = jobGroupGenerator.generate(caseId, eventId);
        log.info("Scheduled {} for case id: {} @ {}", eventId, caseId, dateTime);

        jobScheduler.schedule(new Job<>(
                jobGroup,
                eventId,
                wrapper.getSchedulerPayload(),
                dateTime
        ));
    }

    private void wrapAndThrowNotificationExceptionIfRequired(String caseId, String templateId, Exception ex) {
        if (ex.getCause() instanceof UnknownHostException) {
            NotificationClientRuntimeException exception = new NotificationClientRuntimeException(caseId, ex);
            log.error("Runtime error on GovUKNotify for case id: {}, template: {}", caseId, templateId, exception);
            throw exception;
        } else {
            NotificationServiceException exception = new NotificationServiceException(caseId, ex);
            log.error("Error on GovUKNotify for case id: {}, template: {}", caseId, templateId, exception);
        }
    }

    @FunctionalInterface
    public interface SendNotification {
        void send() throws NotificationClientException;
    }
}
