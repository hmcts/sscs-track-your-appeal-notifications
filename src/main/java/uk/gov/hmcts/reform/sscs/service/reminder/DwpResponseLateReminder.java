package uk.gov.hmcts.reform.sscs.service.reminder;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DWP_RESPONSE_LATE_REMINDER;

import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.exception.ReminderException;
import uk.gov.hmcts.reform.sscs.extractor.AppealReceivedDateExtractor;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;

@Component
public class DwpResponseLateReminder implements ReminderHandler {

    private static final org.slf4j.Logger LOG = getLogger(DwpResponseLateReminder.class);

    private final AppealReceivedDateExtractor appealReceivedDateExtractor;
    private final JobGroupGenerator jobGroupGenerator;
    private final JobScheduler<String> jobScheduler;
    private final long delay;

    @Autowired
    public DwpResponseLateReminder(
        AppealReceivedDateExtractor appealReceivedDateExtractor,
        JobGroupGenerator jobGroupGenerator,
        JobScheduler<String> jobScheduler,
        @Value("${reminder.dwpResponseLateReminder.delay.seconds}") long delay
    ) {
        this.appealReceivedDateExtractor = appealReceivedDateExtractor;
        this.jobGroupGenerator = jobGroupGenerator;
        this.jobScheduler = jobScheduler;
        this.delay = delay;
    }

    public boolean canHandle(SscsCaseData ccdResponse) {
        return ccdResponse
            .getNotificationType()
            .equals(APPEAL_RECEIVED);
    }

    public void handle(SscsCaseData ccdResponse) {
        if (!canHandle(ccdResponse)) {
            throw new IllegalArgumentException("cannot handle ccdResponse");
        }

        String caseId = ccdResponse.getCaseId();
        String eventId = DWP_RESPONSE_LATE_REMINDER.getCcdType();
        String jobGroup = jobGroupGenerator.generate(caseId, eventId);
        ZonedDateTime reminderDate = calculateReminderDate(ccdResponse, delay);

        jobScheduler.schedule(new Job<>(
            jobGroup,
            eventId,
            caseId,
            reminderDate
        ));

        LOG.info("Scheduled DWP response late reminder for case id: {} @ {}", caseId, reminderDate.toString());
    }

    private ZonedDateTime calculateReminderDate(SscsCaseData ccdResponse, long delay) {

        Optional<ZonedDateTime> appealReceivedDate = appealReceivedDateExtractor.extract(ccdResponse);

        if (appealReceivedDate.isPresent()) {
            return appealReceivedDate.get()
                .plusSeconds(delay);
        }

        ReminderException reminderException = new ReminderException(
            new Exception("Could not find reminder date for case id: " + ccdResponse.getCaseId())
        );

        LOG.error("Reminder date not found", reminderException);
        throw reminderException;
    }

}
