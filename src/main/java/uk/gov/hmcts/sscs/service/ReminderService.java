package uk.gov.hmcts.sscs.service;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.sscs.config.AppConstants.ZONE_ID;
import static uk.gov.hmcts.sscs.domain.notify.EventType.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.Events;
import uk.gov.hmcts.sscs.domain.Hearing;
import uk.gov.hmcts.sscs.domain.notify.EventType;
import uk.gov.hmcts.sscs.exception.ReminderException;

@Service
public class ReminderService {

    private JobScheduler<String> jobScheduler;

    @Value("${reminder.evidenceReminder.delay.seconds}")
    private String evidenceReminderDelay;

    @Value("${reminder.hearingReminder.beforeFirst.seconds}")
    private String beforeFirstHearingReminder;

    @Value("${reminder.hearingReminder.beforeSecond.seconds}")
    private String beforeSecondHearingReminder;

    private static final org.slf4j.Logger LOG = getLogger(ReminderService.class);

    @Autowired
    public ReminderService(JobScheduler<String> jobScheduler) {
        this.jobScheduler = jobScheduler;
    }

    public void createJob(CcdResponse ccdResponse) {
        EventType reminderType = findReminderType(ccdResponse.getNotificationType());

        if (reminderType != null) {
            switch (ccdResponse.getNotificationType()) {
                case DWP_RESPONSE_RECEIVED: {
                    scheduleReminder(reminderType, ccdResponse, evidenceReminderDelay);
                    break;
                }
                case HEARING_BOOKED: {
                    scheduleReminder(reminderType, ccdResponse, beforeFirstHearingReminder);
                    scheduleReminder(reminderType, ccdResponse, beforeSecondHearingReminder);
                    break;
                }
                default: break;
            }
        }
    }

    public EventType findReminderType(EventType eventType) {
        switch (eventType) {
            case DWP_RESPONSE_RECEIVED: return EVIDENCE_REMINDER;
            case HEARING_BOOKED: return HEARING_REMINDER;
            default: return null;
        }
    }

    private void scheduleReminder(EventType reminderType, CcdResponse ccdResponse, String delay) {

        ZonedDateTime triggerAt = findReminderDate(ccdResponse, delay);

        Job<String> job = new Job<>(reminderType.getId(), ccdResponse.getCaseId(), triggerAt);

        jobScheduler.schedule(job);
    }

    public ZonedDateTime findReminderDate(CcdResponse ccdResponse, String delay) {
        ZonedDateTime reminderDate = null;
        switch (ccdResponse.getNotificationType()) {
            case DWP_RESPONSE_RECEIVED: {
                reminderDate = calculateDate(ccdResponse, DWP_RESPONSE_RECEIVED, delay);
                break;
            }
            case HEARING_BOOKED: {
                reminderDate = calculateHearingDate(ccdResponse, delay);
                break;
            }
            default: break;
        }

        if (reminderDate != null) {
            return reminderDate;
        }

        ReminderException reminderException = new ReminderException(
                new Exception("Could not find reminder date for case reference" + ccdResponse.getCaseReference()));
        LOG.error("Reminder date not found", reminderException);
        throw reminderException;
    }

    private ZonedDateTime calculateDate(CcdResponse ccdResponse, EventType eventType, String delay) {
        for (Events events : ccdResponse.getEvents()) {
            if (events.getValue() != null && events.getValue().getEventType().equals(eventType)) {
                return events.getValue().getDateTime().plusSeconds(Long.parseLong(delay));
            }
        }
        return null;
    }

    private ZonedDateTime calculateHearingDate(CcdResponse ccdResponse, String sendSecondsBefore) {

        if (!ccdResponse.getHearings().isEmpty()) {
            Hearing hearing = ccdResponse.getHearings().get(0);
            LocalDateTime dateBefore = hearing.getValue().getHearingDateTime().minusSeconds(Long.parseLong(sendSecondsBefore));

            return ZonedDateTime.ofLocal(dateBefore, ZoneId.of(ZONE_ID), null);
        }
        return null;
    }
}
