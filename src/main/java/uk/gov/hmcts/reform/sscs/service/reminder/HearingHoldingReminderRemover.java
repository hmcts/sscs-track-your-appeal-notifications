package uk.gov.hmcts.reform.sscs.service.reminder;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
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

    public boolean canHandle(SscsCaseData ccdResponse) {
        return Arrays.asList(
            APPEAL_DORMANT,
            APPEAL_LAPSED,
            APPEAL_WITHDRAWN,
            HEARING_BOOKED
        ).contains(
            ccdResponse.getNotificationType()
        );
    }

    public void handle(SscsCaseData ccdResponse) {
        if (!canHandle(ccdResponse)) {
            throw new IllegalArgumentException("cannot handle ccdResponse");
        }

        final String caseId = ccdResponse.getCaseId();

        ImmutableList
            .of(FIRST_HEARING_HOLDING_REMINDER.getCcdType(),
                SECOND_HEARING_HOLDING_REMINDER.getCcdType(),
                THIRD_HEARING_HOLDING_REMINDER.getCcdType(),
                FINAL_HEARING_HOLDING_REMINDER.getCcdType())
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
