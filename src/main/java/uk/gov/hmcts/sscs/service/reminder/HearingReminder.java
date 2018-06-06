package uk.gov.hmcts.sscs.service.reminder;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.sscs.config.AppConstants.ZONE_ID;
import static uk.gov.hmcts.sscs.domain.notify.EventType.HEARING_BOOKED;
import static uk.gov.hmcts.sscs.domain.notify.EventType.HEARING_REMINDER;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.Hearing;
import uk.gov.hmcts.sscs.exception.ReminderException;

@Component
public class HearingReminder implements ReminderHandler {

    private static final org.slf4j.Logger LOG = getLogger(HearingReminder.class);

    private final JobGroupGenerator jobGroupGenerator;
    private JobScheduler<String> jobScheduler;

    private long beforeFirstHearingReminder;
    private long beforeSecondHearingReminder;

    @Autowired
    public HearingReminder(
        JobGroupGenerator jobGroupGenerator,
        JobScheduler<String> jobScheduler,
        @Value("${reminder.hearingReminder.beforeFirst.seconds}") long beforeFirstHearingReminder,
        @Value("${reminder.hearingReminder.beforeSecond.seconds}") long beforeSecondHearingReminder
    ) {
        this.jobGroupGenerator = jobGroupGenerator;
        this.jobScheduler = jobScheduler;
        this.beforeFirstHearingReminder = beforeFirstHearingReminder;
        this.beforeSecondHearingReminder = beforeSecondHearingReminder;
    }

    public boolean canHandle(CcdResponse ccdResponse) {
        return ccdResponse
            .getNotificationType()
            .equals(HEARING_BOOKED);
    }

    public void handle(CcdResponse ccdResponse) {
        if (!canHandle(ccdResponse)) {
            throw new IllegalArgumentException("cannot handle ccdResponse");
        }

        scheduleReminder(ccdResponse, beforeFirstHearingReminder);
        scheduleReminder(ccdResponse, beforeSecondHearingReminder);
    }

    private void scheduleReminder(CcdResponse ccdResponse, long secondsBeforeHearing) {

        String caseId = ccdResponse.getCaseId();
        String eventId = HEARING_REMINDER.getId();
        String jobGroup = jobGroupGenerator.generate(caseId, eventId);
        ZonedDateTime reminderDate = calculateReminderDate(ccdResponse, secondsBeforeHearing);

        jobScheduler.schedule(new Job<>(
            jobGroup,
            eventId,
            caseId,
            reminderDate
        ));

        LOG.info("Scheduled hearing reminder for case: " + caseId + " @ " + reminderDate.toString());
    }

    private ZonedDateTime calculateReminderDate(CcdResponse ccdResponse, long secondsBeforeHearing) {

        if (!ccdResponse.getHearings().isEmpty()) {
            Hearing hearing = ccdResponse.getHearings().get(0);
            LocalDateTime dateBefore = hearing.getValue().getHearingDateTime().minusSeconds(secondsBeforeHearing);
            return ZonedDateTime.ofLocal(dateBefore, ZoneId.of(ZONE_ID), null);
        }

        ReminderException reminderException = new ReminderException(
            new Exception("Could not find reminder date for case reference: " + ccdResponse.getCaseReference())
        );

        LOG.error("Reminder date not found", reminderException);
        throw reminderException;
    }

}
