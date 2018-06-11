package uk.gov.hmcts.sscs.service.reminder;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.sscs.domain.notify.EventType.*;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobNotFoundException;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobRemover;
import uk.gov.hmcts.sscs.domain.CcdResponse;

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

    public boolean canHandle(CcdResponse ccdResponse) {
        return Arrays.asList(
            APPEAL_DORMANT,
            APPEAL_LAPSED,
            APPEAL_WITHDRAWN,
            HEARING_BOOKED
        ).contains(
            ccdResponse.getNotificationType()
        );
    }

    public void handle(CcdResponse ccdResponse) {
        if (!canHandle(ccdResponse)) {
            throw new IllegalArgumentException("cannot handle ccdResponse");
        }

        final String caseId = ccdResponse.getCaseId();

        ImmutableList
            .of(HEARING_HOLDING_REMINDER.getId(),
                FINAL_HEARING_HOLDING_REMINDER.getId())
            .forEach(eventId -> {

                String jobGroup = jobGroupGenerator.generate(caseId, eventId);

                try {

                    jobRemover.removeGroup(jobGroup);
                    LOG.info("Removed hearing holding reminder from case: {}", caseId);

                } catch (JobNotFoundException ignore) {
                    LOG.warn("Hearing holding reminder for case: " + caseId + " could not be found");
                }
            });
    }

}
