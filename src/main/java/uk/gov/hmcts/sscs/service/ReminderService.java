package uk.gov.hmcts.sscs.service;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.sscs.domain.notify.EventType.DWP_RESPONSE_RECEIVED;
import static uk.gov.hmcts.sscs.domain.notify.EventType.EVIDENCE_REMINDER;

import java.time.ZonedDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.Event;
import uk.gov.hmcts.sscs.domain.notify.EventType;
import uk.gov.hmcts.sscs.exception.ReminderException;

@Service
public class ReminderService {

    private JobScheduler<String> jobScheduler;

    @Value("${reminder.evidenceReminder.delay.seconds}")
    private String evidenceReminderDelay;

    private static final org.slf4j.Logger LOG = getLogger(ReminderService.class);

    @Autowired
    public ReminderService(JobScheduler<String> jobScheduler) {
        this.jobScheduler = jobScheduler;
    }

    public void createJob(CcdResponse ccdResponse) {
        String reminderType = findReminderType(ccdResponse.getNotificationType()).getId();

        if (reminderType != null) {
            ZonedDateTime triggerAt = findReminderDate(ccdResponse);

            Job<String> job = new Job(reminderType, ccdResponse.getCaseId(), triggerAt);

            jobScheduler.schedule(job);
        }
    }

    public EventType findReminderType(EventType eventType) {
        switch (eventType) {
            case DWP_RESPONSE_RECEIVED: return EVIDENCE_REMINDER;
            default: return null;
        }
    }

    public ZonedDateTime findReminderDate(CcdResponse ccdResponse) {
        switch (ccdResponse.getNotificationType()) {
            case DWP_RESPONSE_RECEIVED: {
                return calculateDate(ccdResponse, DWP_RESPONSE_RECEIVED, evidenceReminderDelay);
            }
            default: break;
        }
        ReminderException reminderException = new ReminderException(
                new Exception("Could not find reminder date for case reference" + ccdResponse.getCaseReference()));
        LOG.error("Reminder date not found", reminderException);
        throw reminderException;
    }

    private ZonedDateTime calculateDate(CcdResponse ccdResponse, EventType eventType, String delay) {
        for (Event event : ccdResponse.getEvents()) {
            if (event.getValue() != null && event.getValue().getEventType().equals(eventType)) {
                return event.getValue().getDateTime().plusSeconds(Long.parseLong(delay));
            }
        }
        return null;
    }
}
