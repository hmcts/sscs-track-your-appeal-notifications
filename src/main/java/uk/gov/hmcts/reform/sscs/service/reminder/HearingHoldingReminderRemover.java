package uk.gov.hmcts.reform.sscs.service.reminder;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobNotFoundException;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobRemover;

@Component
public class HearingHoldingReminderRemover implements ReminderHandler {

    private static final org.slf4j.Logger LOG = getLogger(HearingReminderRemover.class);

    private final JobGroupGenerator jobGroupGenerator;
    private final JobRemover jobRemover;

    @Autowired
    public HearingHoldingReminderRemover(
        JobGroupGenerator jobGroupGenerator,
        JobRemover jobRemover
    ) {
        this.jobGroupGenerator = jobGroupGenerator;
        this.jobRemover = jobRemover;
    }

    public boolean canHandle(NotificationWrapper wrapper) {
        return Arrays.asList(
            APPEAL_DORMANT_NOTIFICATION,
            APPEAL_LAPSED_NOTIFICATION,
            APPEAL_WITHDRAWN_NOTIFICATION,
            HEARING_BOOKED_NOTIFICATION
        ).contains(
            wrapper.getNotificationType()
        );
    }

    public boolean canSchedule(NotificationWrapper wrapper) {
        return true;
    }

    public void handle(NotificationWrapper wrapper) {
        if (!canHandle(wrapper)) {
            throw new IllegalArgumentException("cannot handle ccdResponse");
        }

        final String caseId = wrapper.getCaseId();

        ImmutableList
            .of(FIRST_HEARING_HOLDING_REMINDER_NOTIFICATION.getId(),
                SECOND_HEARING_HOLDING_REMINDER_NOTIFICATION.getId(),
                THIRD_HEARING_HOLDING_REMINDER_NOTIFICATION.getId(),
                FINAL_HEARING_HOLDING_REMINDER_NOTIFICATION.getId())
            .forEach(eventId -> {

                String jobGroup = jobGroupGenerator.generate(caseId, eventId);

                try {

                    jobRemover.removeGroup(jobGroup);
                    LOG.info("Removed hearing holding reminder from case id: {}", caseId);

                } catch (JobNotFoundException ignore) {
                    LOG.warn("Hearing holding reminder for case id: {} could not be found", caseId);
                }
            });
    }

}
