package uk.gov.hmcts.reform.sscs.service.reminder;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobNotFoundException;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobRemover;

@Component
public class DwpResponseLateReminderRemover implements ReminderHandler {

    private static final org.slf4j.Logger LOG = getLogger(HearingReminderRemover.class);

    private final JobGroupGenerator jobGroupGenerator;
    private final JobRemover jobRemover;

    @Autowired
    public DwpResponseLateReminderRemover(
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
            DWP_RESPONSE_RECEIVED_NOTIFICATION
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

        SscsCaseData caseData = wrapper.getNewSscsCaseData();
        String caseId = caseData.getCcdCaseId();
        String jobGroup = jobGroupGenerator.generate(caseId, DWP_RESPONSE_LATE_REMINDER_NOTIFICATION.getId());

        try {

            jobRemover.removeGroup(jobGroup);
            LOG.info("Removed DWP response late reminder from case id: {}", caseId);

        } catch (JobNotFoundException ignore) {
            LOG.warn("DWP response late reminder for case id: {} could not be found", caseId);
        }
    }

}
