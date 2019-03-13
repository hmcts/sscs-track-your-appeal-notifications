package uk.gov.hmcts.reform.sscs.service;

import static org.slf4j.LoggerFactory.getLogger;

import java.net.UnknownHostException;
import org.slf4j.Logger;
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
public class NotificationHandler {
    private static final Logger LOG = getLogger(NotificationHandler.class);

    private final OutOfHoursCalculator outOfHoursCalculator;
    private final JobScheduler jobScheduler;
    private final JobGroupGenerator jobGroupGenerator;

    @Autowired
    public NotificationHandler(OutOfHoursCalculator outOfHoursCalculator, JobScheduler jobScheduler, JobGroupGenerator jobGroupGenerator) {
        this.outOfHoursCalculator = outOfHoursCalculator;
        this.jobScheduler = jobScheduler;
        this.jobGroupGenerator = jobGroupGenerator;
    }

    public <T> void sendNotification(NotificationWrapper wrapper, String notificationTemplate, final String notificationType, SendNotification sendNotification) {
        final String caseId = wrapper.getCaseId();
        try {
            LOG.info("Sending {} template {} for case id: {}", notificationType, notificationTemplate, caseId);
            sendNotification.send();
            LOG.info("{} template {} sent for case id: {}", notificationType, notificationTemplate, caseId);
        } catch (Exception ex) {
            wrapAndThrowNotificationExceptionIfRequired(caseId, notificationTemplate, ex);
        }
    }

    public void scheduleNotification(NotificationWrapper wrapper) {
        final String caseId = wrapper.getCaseId();
        String eventId = wrapper.getNotificationType().getId();
        String jobGroup = jobGroupGenerator.generate(caseId, eventId);
        LOG.info("Scheduled {} for case id: {} @ {}", eventId, caseId, outOfHoursCalculator.getStartOfNextInHoursPeriod());

        jobScheduler.schedule(new Job<>(
                jobGroup,
                eventId,
                wrapper.getSchedulerPayload(),
                outOfHoursCalculator.getStartOfNextInHoursPeriod()
        ));
    }

    private void wrapAndThrowNotificationExceptionIfRequired(String caseId, String templateId, Exception ex) {
        if (ex.getCause() instanceof UnknownHostException) {
            NotificationClientRuntimeException exception = new NotificationClientRuntimeException(caseId, ex);
            LOG.error("Runtime error on GovUKNotify for case id: {}, template: {}", caseId, templateId, exception);
            throw exception;
        } else {
            NotificationServiceException exception = new NotificationServiceException(caseId, ex);
            LOG.error("Error on GovUKNotify for case id: {}, template: {}", caseId, templateId, exception);
        }
    }

    @FunctionalInterface
    public interface SendNotification {
        void send() throws NotificationClientException;
    }
}
