package uk.gov.hmcts.sscs.service;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.sscs.domain.notify.EventType.DWP_RESPONSE_RECEIVED;
import static uk.gov.hmcts.sscs.domain.notify.EventType.EVIDENCE_REMINDER;

import java.time.ZonedDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.Events;
import uk.gov.hmcts.sscs.domain.notify.EventType;
import uk.gov.hmcts.sscs.domain.reminder.Action;
import uk.gov.hmcts.sscs.exception.ReminderException;

@Service
public class ReminderService {

    private JobScheduler<Action> jobScheduler;

    private static final org.slf4j.Logger LOG = getLogger(ReminderService.class);

    @Autowired
    public ReminderService(JobScheduler<Action> jobScheduler) {
        this.jobScheduler = jobScheduler;
    }

    public void createJob(CcdResponse ccdResponse) {
        String reminderType = findReminderType(ccdResponse.getNotificationType()).getId();

        if (reminderType != null) {
            Action action = new Action(ccdResponse.getCaseId(), reminderType);
            ZonedDateTime triggerAt = findReminderDate(ccdResponse);

            Job<Action> job = new Job(reminderType, action, triggerAt);

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
        for (Events events : ccdResponse.getEvents()) {
            if (events.getValue() != null) {
                switch (ccdResponse.getNotificationType()) {
                    case DWP_RESPONSE_RECEIVED: {
                        if (events.getValue().getEventType().equals(DWP_RESPONSE_RECEIVED)) {
                            return events.getValue().getDateTime().plusDays(2);
                        }
                    }
                    default: break;
                }
            }
        }
        ReminderException reminderException = new ReminderException(
                new Exception("Could not find reminder date for case reference" + ccdResponse.getCaseReference()));
        LOG.error("Reminder date not found", reminderException);
        throw reminderException;
    }
}
