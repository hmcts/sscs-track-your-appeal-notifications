package uk.gov.hmcts.reform.sscs.service.reminder;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.HEARING_BOOKED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.HEARING_REMINDER_NOTIFICATION;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.config.AppConstants;
import uk.gov.hmcts.reform.sscs.exception.ReminderException;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;

@Component
public class HearingReminder implements ReminderHandler {

    private static final org.slf4j.Logger LOG = getLogger(HearingReminder.class);

    private final JobGroupGenerator jobGroupGenerator;
    private JobScheduler jobScheduler;

    private long beforeFirstHearingReminder;
    private long beforeSecondHearingReminder;

    @Autowired
    public HearingReminder(
        JobGroupGenerator jobGroupGenerator,
        JobScheduler jobScheduler,
        @Value("${reminder.hearingReminder.beforeFirst.seconds}") long beforeFirstHearingReminder,
        @Value("${reminder.hearingReminder.beforeSecond.seconds}") long beforeSecondHearingReminder
    ) {
        this.jobGroupGenerator = jobGroupGenerator;
        this.jobScheduler = jobScheduler;
        this.beforeFirstHearingReminder = beforeFirstHearingReminder;
        this.beforeSecondHearingReminder = beforeSecondHearingReminder;
    }

    public boolean canHandle(NotificationWrapper wrapper) {
        return wrapper
            .getNotificationType()
            .equals(HEARING_BOOKED_NOTIFICATION);
    }

    public void handle(NotificationWrapper wrapper) {
        if (!canHandle(wrapper)) {
            throw new IllegalArgumentException("cannot handle ccdResponse");
        }

        scheduleReminder(wrapper.getNewSscsCaseData(), beforeFirstHearingReminder);
        scheduleReminder(wrapper.getNewSscsCaseData(), beforeSecondHearingReminder);
    }

    private void scheduleReminder(SscsCaseData ccdResponse, long secondsBeforeHearing) {

        String caseId = ccdResponse.getCaseId();
        String eventId = HEARING_REMINDER_NOTIFICATION.getId();
        String jobGroup = jobGroupGenerator.generate(caseId, eventId);
        ZonedDateTime reminderDate = calculateReminderDate(ccdResponse, secondsBeforeHearing);

        jobScheduler.schedule(new Job<>(
            jobGroup,
            eventId,
            caseId,
            reminderDate
        ));

        LOG.info("Scheduled hearing reminder for case id: {} @ {}", caseId, reminderDate.toString());
    }

    private ZonedDateTime calculateReminderDate(SscsCaseData ccdResponse, long secondsBeforeHearing) {

        if (!ccdResponse.getHearings().isEmpty()) {
            Hearing hearing = ccdResponse.getHearings().get(0);
            LocalDateTime dateBefore = hearing.getValue().getHearingDateTime().minusSeconds(secondsBeforeHearing);
            return ZonedDateTime.ofLocal(dateBefore, ZoneId.of(AppConstants.ZONE_ID), null);
        }

        ReminderException reminderException = new ReminderException(
            new Exception("Could not find reminder date for case id: " + ccdResponse.getCaseId())
        );

        LOG.error("Reminder date not found", reminderException);
        throw reminderException;
    }

}
