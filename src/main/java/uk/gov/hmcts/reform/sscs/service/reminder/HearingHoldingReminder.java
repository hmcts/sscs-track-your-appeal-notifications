package uk.gov.hmcts.reform.sscs.service.reminder;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.extractor.HearingContactDateExtractor;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;

@Component
public class HearingHoldingReminder implements ReminderHandler {

    private static final org.slf4j.Logger LOG = getLogger(HearingHoldingReminder.class);

    private final HearingContactDateExtractor hearingContactDateExtractor;
    private final JobGroupGenerator jobGroupGenerator;
    private final JobScheduler jobScheduler;

    @Autowired
    public HearingHoldingReminder(
        HearingContactDateExtractor hearingContactDateExtractor,
        JobGroupGenerator jobGroupGenerator,
        JobScheduler jobScheduler
    ) {
        this.hearingContactDateExtractor = hearingContactDateExtractor;
        this.jobGroupGenerator = jobGroupGenerator;
        this.jobScheduler = jobScheduler;
    }

    public boolean canHandle(NotificationWrapper wrapper) {
        return wrapper
            .getNotificationType()
            .equals(DWP_RESPONSE_RECEIVED_NOTIFICATION);
    }

    public boolean canSchedule(NotificationWrapper wrapper) {
        boolean isReminderDatePresent = false;
        try {
            isReminderDatePresent = calculateReminderDate(wrapper.getNewSscsCaseData(), DWP_RESPONSE_RECEIVED_NOTIFICATION) != null;
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

        SscsCaseData sscsCaseData = wrapper.getNewSscsCaseData();
        scheduleReminder(sscsCaseData, DWP_RESPONSE_RECEIVED_NOTIFICATION, FIRST_HEARING_HOLDING_REMINDER_NOTIFICATION);
        scheduleReminder(sscsCaseData, FIRST_HEARING_HOLDING_REMINDER_NOTIFICATION, SECOND_HEARING_HOLDING_REMINDER_NOTIFICATION);
        scheduleReminder(sscsCaseData, SECOND_HEARING_HOLDING_REMINDER_NOTIFICATION, THIRD_HEARING_HOLDING_REMINDER_NOTIFICATION);
        scheduleReminder(sscsCaseData, THIRD_HEARING_HOLDING_REMINDER_NOTIFICATION, FINAL_HEARING_HOLDING_REMINDER_NOTIFICATION);
    }

    private void scheduleReminder(
        SscsCaseData ccdResponse,
        NotificationEventType referenceNotificationEventType,
        NotificationEventType scheduledNotificationEventType
    ) {
        String caseId = ccdResponse.getCcdCaseId();
        String eventId = scheduledNotificationEventType.getId();
        String jobGroup = jobGroupGenerator.generate(caseId, eventId);
        ZonedDateTime reminderDate = calculateReminderDate(ccdResponse, referenceNotificationEventType);

        if (reminderDate != null) {
            jobScheduler.schedule(new Job<>(
                    jobGroup,
                    eventId,
                    caseId,
                    reminderDate
            ));

            LOG.info("Scheduled hearing holding reminder for case id: {} @ {}", caseId, reminderDate);
        } else {
            LOG.info("Could not find reminder date for case id {}", ccdResponse.getCcdCaseId());
        }
    }

    private ZonedDateTime calculateReminderDate(SscsCaseData ccdResponse, NotificationEventType referenceNotificationEventType) {

        Optional<ZonedDateTime> hearingContactDate =
                hearingContactDateExtractor.extractForReferenceEvent(ccdResponse, referenceNotificationEventType);

        if (hearingContactDate.isPresent()) {
            return hearingContactDate.get();
        }

        return null;
    }

}
