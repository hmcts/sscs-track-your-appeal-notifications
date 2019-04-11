package uk.gov.hmcts.reform.sscs.service.reminder;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_RECEIVED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DWP_RESPONSE_LATE_REMINDER_NOTIFICATION;

import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.extractor.AppealReceivedDateExtractor;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;

@Component
public class DwpResponseLateReminder implements ReminderHandler {

    private static final org.slf4j.Logger LOG = getLogger(DwpResponseLateReminder.class);

    private final AppealReceivedDateExtractor appealReceivedDateExtractor;
    private final JobGroupGenerator jobGroupGenerator;
    private final JobScheduler jobScheduler;
    private final long delay;

    @Autowired
    public DwpResponseLateReminder(
        AppealReceivedDateExtractor appealReceivedDateExtractor,
        JobGroupGenerator jobGroupGenerator,
        JobScheduler jobScheduler,
        @Value("${reminder.dwpResponseLateReminder.delay.seconds}") long delay
    ) {
        this.appealReceivedDateExtractor = appealReceivedDateExtractor;
        this.jobGroupGenerator = jobGroupGenerator;
        this.jobScheduler = jobScheduler;
        this.delay = delay;
    }

    public boolean canHandle(NotificationWrapper wrapper) {
        return wrapper
            .getNotificationType()
            .equals(APPEAL_RECEIVED_NOTIFICATION);
    }

    public boolean canSchedule(NotificationWrapper wrapper) {
        boolean isReminderDatePresent = false;
        try {
            isReminderDatePresent = calculateReminderDate(wrapper.getNewSscsCaseData(), delay) != null;
        } catch (Exception e) {
            LOG.error("Error while calculating reminder date for case id {} with exception {}",
                    wrapper.getNewSscsCaseData().getCcdCaseId(), e);
        }
        if (!isReminderDatePresent) {
            LOG.info("Could not find reminder date for case id {}", wrapper.getNewSscsCaseData().getCcdCaseId());
        }
        return isReminderDatePresent;
    }


    public void handle(NotificationWrapper wrapper) {
        if (!canHandle(wrapper)) {
            throw new IllegalArgumentException("cannot handle ccdResponse");
        }

        SscsCaseData caseData = wrapper.getNewSscsCaseData();
        String caseId = wrapper.getNewSscsCaseData().getCcdCaseId();
        String eventId = DWP_RESPONSE_LATE_REMINDER_NOTIFICATION.getId();
        String jobGroup = jobGroupGenerator.generate(caseId, eventId);
        ZonedDateTime reminderDate = calculateReminderDate(caseData, delay);

        if (reminderDate != null) {
            jobScheduler.schedule(new Job<>(
                    jobGroup,
                    eventId,
                    caseId,
                    reminderDate
            ));


            LOG.info("Scheduled DWP response late reminder for case id: {} @ {}", caseId, reminderDate);
        } else {
            LOG.info("Could not find reminder date for case id {}", wrapper.getNewSscsCaseData().getCcdCaseId());
        }
    }

    private ZonedDateTime calculateReminderDate(SscsCaseData ccdResponse, long delay) {

        Optional<ZonedDateTime> appealReceivedDate = appealReceivedDateExtractor.extract(ccdResponse);

        if (appealReceivedDate.isPresent()) {
            return appealReceivedDate.get()
                .plusSeconds(delay);
        }
        return null;
    }

}
