package uk.gov.hmcts.reform.sscs.service.reminder;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DWP_RESPONSE_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.EVIDENCE_REMINDER;

import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.exception.ReminderException;
import uk.gov.hmcts.reform.sscs.extractor.DwpResponseReceivedDateExtractor;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;

@Component
public class EvidenceReminder implements ReminderHandler {

    private static final org.slf4j.Logger LOG = getLogger(EvidenceReminder.class);

    private final DwpResponseReceivedDateExtractor dwpResponseReceivedDateExtractor;
    private final JobGroupGenerator jobGroupGenerator;
    private final JobScheduler<String> jobScheduler;
    private final long evidenceReminderDelay;

    @Autowired
    public EvidenceReminder(
        DwpResponseReceivedDateExtractor dwpResponseReceivedDateExtractor,
        JobGroupGenerator jobGroupGenerator,
        JobScheduler<String> jobScheduler,
        @Value("${reminder.evidenceReminder.delay.seconds}") long evidenceReminderDelay
    ) {
        this.dwpResponseReceivedDateExtractor = dwpResponseReceivedDateExtractor;
        this.jobGroupGenerator = jobGroupGenerator;
        this.jobScheduler = jobScheduler;
        this.evidenceReminderDelay = evidenceReminderDelay;
    }

    public boolean canHandle(SscsCaseData ccdResponse) {
        return ccdResponse
            .getNotificationType()
            .equals(DWP_RESPONSE_RECEIVED);
    }

    public void handle(SscsCaseData ccdResponse) {
        if (!canHandle(ccdResponse)) {
            throw new IllegalArgumentException("cannot handle ccdResponse");
        }

        String caseId = ccdResponse.getCaseId();
        String eventId = EVIDENCE_REMINDER.getCcdType();
        String jobGroup = jobGroupGenerator.generate(caseId, eventId);
        ZonedDateTime reminderDate = calculateReminderDate(ccdResponse);

        jobScheduler.schedule(new Job<>(
            jobGroup,
            eventId,
            caseId,
            reminderDate
        ));

        LOG.info("Scheduled evidence reminder for case id: {} @ {}", caseId, reminderDate.toString());
    }

    private ZonedDateTime calculateReminderDate(SscsCaseData ccdResponse) {

        Optional<ZonedDateTime> dwpResponseReceivedDate = dwpResponseReceivedDateExtractor.extract(ccdResponse);

        if (dwpResponseReceivedDate.isPresent()) {
            return dwpResponseReceivedDate.get()
                .plusSeconds(evidenceReminderDelay);
        }

        ReminderException reminderException = new ReminderException(
            new Exception("Could not find reminder date for case id: " + ccdResponse.getCaseId())
        );

        LOG.error("Reminder date not found", reminderException);
        throw reminderException;
    }

}
