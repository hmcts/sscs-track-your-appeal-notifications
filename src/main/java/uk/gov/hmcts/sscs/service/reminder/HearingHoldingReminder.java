package uk.gov.hmcts.sscs.service.reminder;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.sscs.domain.notify.EventType.DWP_RESPONSE_RECEIVED;
import static uk.gov.hmcts.sscs.domain.notify.EventType.FINAL_HEARING_HOLDING_REMINDER;
import static uk.gov.hmcts.sscs.domain.notify.EventType.HEARING_HOLDING_REMINDER;

import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.exception.ReminderException;
import uk.gov.hmcts.sscs.extractor.DwpResponseReceivedDateExtractor;

@Component
public class HearingHoldingReminder implements ReminderHandler {

    private static final org.slf4j.Logger LOG = getLogger(HearingHoldingReminder.class);

    private final DwpResponseReceivedDateExtractor dwpResponseReceivedDateExtractor;
    private final JobGroupGenerator jobGroupGenerator;
    private final JobScheduler<String> jobScheduler;
    private final long initialDelay;
    private final long subsequentDelay;

    @Autowired
    public HearingHoldingReminder(
        DwpResponseReceivedDateExtractor dwpResponseReceivedDateExtractor,
        JobGroupGenerator jobGroupGenerator,
        JobScheduler<String> jobScheduler,
        @Value("${reminder.hearingHoldingReminder.initialDelay.seconds}") long initialDelay,
        @Value("${reminder.hearingHoldingReminder.subsequentDelay.seconds}") long subsequentDelay
    ) {
        this.dwpResponseReceivedDateExtractor = dwpResponseReceivedDateExtractor;
        this.jobGroupGenerator = jobGroupGenerator;
        this.jobScheduler = jobScheduler;
        this.initialDelay = initialDelay;
        this.subsequentDelay = subsequentDelay;
    }

    public boolean canHandle(CcdResponse ccdResponse) {
        return ccdResponse
            .getNotificationType()
            .equals(DWP_RESPONSE_RECEIVED);
    }

    public void handle(CcdResponse ccdResponse) {
        if (!canHandle(ccdResponse)) {
            throw new IllegalArgumentException("cannot handle ccdResponse");
        }

        scheduleReminder(ccdResponse, initialDelay);
        scheduleReminder(ccdResponse, initialDelay + subsequentDelay);
        scheduleReminder(ccdResponse, initialDelay + (subsequentDelay * 2));
        scheduleFinalReminder(ccdResponse, initialDelay + (subsequentDelay * 3));
    }

    private void scheduleReminder(CcdResponse ccdResponse, long delay) {

        String caseId = ccdResponse.getCaseId();
        String eventId = HEARING_HOLDING_REMINDER.getId();
        String jobGroup = jobGroupGenerator.generate(caseId, eventId);
        ZonedDateTime reminderDate = calculateReminderDate(ccdResponse, delay);

        jobScheduler.schedule(new Job<>(
            jobGroup,
            eventId,
            caseId,
            reminderDate
        ));

        LOG.info("Scheduled hearing holding reminder for case id: {} @ {}", caseId, reminderDate.toString());
    }

    private void scheduleFinalReminder(CcdResponse ccdResponse, long delay) {

        String caseId = ccdResponse.getCaseId();
        String eventId = FINAL_HEARING_HOLDING_REMINDER.getId();
        String jobGroup = jobGroupGenerator.generate(caseId, eventId);
        ZonedDateTime reminderDate = calculateReminderDate(ccdResponse, delay);

        jobScheduler.schedule(new Job<>(
            jobGroup,
            eventId,
            caseId,
            reminderDate
        ));

        LOG.info("Scheduled final hearing holding reminder for case id: {} @ {}", caseId, reminderDate.toString());
    }

    private ZonedDateTime calculateReminderDate(CcdResponse ccdResponse, long delay) {

        Optional<ZonedDateTime> dwpResponseReceivedDate = dwpResponseReceivedDateExtractor.extract(ccdResponse);

        if (dwpResponseReceivedDate.isPresent()) {
            return dwpResponseReceivedDate.get()
                .plusSeconds(delay);
        }

        ReminderException reminderException = new ReminderException(
            new Exception("Could not find reminder date for case id: " + ccdResponse.getCaseId())
        );

        LOG.error("Reminder date not found", reminderException);
        throw reminderException;
    }

}
