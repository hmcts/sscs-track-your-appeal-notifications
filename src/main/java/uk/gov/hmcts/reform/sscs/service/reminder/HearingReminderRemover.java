package uk.gov.hmcts.reform.sscs.service.reminder;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.HEARING_REMINDER_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.POSTPONEMENT_NOTIFICATION;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobNotFoundException;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobRemover;

@Component
public class HearingReminderRemover implements ReminderHandler {

    private static final org.slf4j.Logger LOG = getLogger(HearingReminderRemover.class);

    private final JobGroupGenerator jobGroupGenerator;
    private final JobRemover jobRemover;

    @Autowired
    public HearingReminderRemover(
        JobGroupGenerator jobGroupGenerator,
        JobRemover jobRemover
    ) {
        this.jobGroupGenerator = jobGroupGenerator;
        this.jobRemover = jobRemover;
    }

    public boolean canHandle(NotificationWrapper wrapper) {
        return wrapper
            .getNotificationType()
            .equals(POSTPONEMENT_NOTIFICATION);
    }

    public void handle(NotificationWrapper wrapper) {
        if (!canHandle(wrapper)) {
            throw new IllegalArgumentException("cannot handle ccdResponse");
        }

        String caseId = wrapper.getCaseId();
        String jobGroup = jobGroupGenerator.generate(caseId, HEARING_REMINDER_NOTIFICATION.getId());

        try {

            jobRemover.removeGroup(jobGroup);
            LOG.info("Removed hearing reminders from case id: {}", caseId);

        } catch (JobNotFoundException ignore) {
            LOG.warn("Hearing reminder for case id: {} could not be found", caseId);
        }
    }

}
