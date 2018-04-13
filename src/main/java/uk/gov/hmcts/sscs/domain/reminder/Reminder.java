package uk.gov.hmcts.sscs.domain.reminder;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.sscs.domain.notify.EventType.DWP_RESPONSE_RECEIVED;
import static uk.gov.hmcts.sscs.domain.notify.EventType.EVIDENCE_REMINDER;

import java.time.ZonedDateTime;
import lombok.Value;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.notify.Event;
import uk.gov.hmcts.sscs.domain.notify.EventType;
import uk.gov.hmcts.sscs.exception.ReminderException;

@Value
public class Reminder {

    private String name;
    private Action action;
    private Trigger trigger;
    private static final String NAME_PREFIX = "SSCS_";
    private static final org.slf4j.Logger LOG = getLogger(Reminder.class);

    public Reminder(CcdResponse ccdResponse, String callbackUrl) {
        String reminderType = findReminderType(ccdResponse.getNotificationType()).getId();
        this.name = NAME_PREFIX + reminderType;
        this.action = new Action(ccdResponse.getCaseId(), reminderType, callbackUrl);
        this.trigger = new Trigger(findReminderDate(ccdResponse));
    }

    public EventType findReminderType(EventType eventType) {
        switch (eventType) {
            case DWP_RESPONSE_RECEIVED: return EVIDENCE_REMINDER;
            default: break;
        }
        ReminderException reminderException = new ReminderException(new Exception("Unknown reminder type " + eventType));
        LOG.error("Reminder type not known", reminderException);
        throw reminderException;
    }

    public ZonedDateTime findReminderDate(CcdResponse ccdResponse) {
        for (Event event : ccdResponse.getEvents()) {
            switch (ccdResponse.getNotificationType()) {
                case DWP_RESPONSE_RECEIVED: {
                    if (event.getEventType().equals(DWP_RESPONSE_RECEIVED)) {
                        return event.getDateTime().plusDays(2);
                    }
                    break;
                }
                default: break;
            }
        }
        ReminderException reminderException = new ReminderException(
                new Exception("Could not find reminder date for case reference" + ccdResponse.getCaseReference()));
        LOG.error("Reminder date not found", reminderException);
        throw reminderException;
    }
}
