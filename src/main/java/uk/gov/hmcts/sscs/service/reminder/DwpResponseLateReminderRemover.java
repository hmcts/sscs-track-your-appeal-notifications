package uk.gov.hmcts.sscs.service.reminder;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.sscs.domain.notify.EventType.*;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobNotFoundException;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobRemover;
import uk.gov.hmcts.sscs.domain.CcdResponse;

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

    public boolean canHandle(CcdResponse ccdResponse) {
        return Arrays.asList(
            APPEAL_DORMANT,
            APPEAL_LAPSED,
            APPEAL_WITHDRAWN,
            DWP_RESPONSE_RECEIVED
        ).contains(
            ccdResponse.getNotificationType()
        );
    }

    public void handle(CcdResponse ccdResponse) {
        if (!canHandle(ccdResponse)) {
            throw new IllegalArgumentException("cannot handle ccdResponse");
        }

        String caseId = ccdResponse.getCaseId();
        String jobGroup = jobGroupGenerator.generate(caseId, DWP_RESPONSE_LATE_REMINDER.getId());

        try {

            jobRemover.removeGroup(jobGroup);
            LOG.info("Removed DWP response late reminder from case id: {}", caseId);

        } catch (JobNotFoundException ignore) {
            LOG.warn("DWP response late reminder for case id: {} could not be found", caseId);
        }
    }

}
