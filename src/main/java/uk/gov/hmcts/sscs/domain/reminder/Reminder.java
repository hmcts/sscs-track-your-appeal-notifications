//package uk.gov.hmcts.sscs.domain.reminder;
//
//import static org.slf4j.LoggerFactory.getLogger;
//import static uk.gov.hmcts.sscs.domain.notify.EventType.DWP_RESPONSE_RECEIVED;
//import static uk.gov.hmcts.sscs.domain.notify.EventType.EVIDENCE_REMINDER;
//
//import java.time.ZonedDateTime;
//import lombok.Value;
//import uk.gov.hmcts.sscs.domain.CcdResponse;
//import uk.gov.hmcts.sscs.domain.Events;
//import uk.gov.hmcts.sscs.domain.notify.EventType;
//import uk.gov.hmcts.sscs.exception.ReminderException;
//
//@Value
//public class Reminder {
//
//    private String name;
//    private Action action;
//    private ZonedDateTime triggerAt;
//    private static final org.slf4j.Logger LOG = getLogger(Reminder.class);
//
//    public Reminder(CcdResponse ccdResponse) {
//        String reminderType = findReminderType(ccdResponse.getNotificationType()).getId();
//        this.name = reminderType;
//        this.action = new Action(ccdResponse.getCaseId(), reminderType);
//        this.triggerAt = findReminderDate(ccdResponse);
//    }
//
//    private EventType findReminderType(EventType eventType) {
//        switch (eventType) {
//            case DWP_RESPONSE_RECEIVED: return EVIDENCE_REMINDER;
//            default: break;
//        }
//        ReminderException reminderException = new ReminderException(new Exception("Unknown reminder type " + eventType));
//        LOG.error("Reminder type not known", reminderException);
//        throw reminderException;
//    }
//
//    private ZonedDateTime findReminderDate(CcdResponse ccdResponse) {
//        for (Events events : ccdResponse.getEvents()) {
//            if (events.getValue() != null) {
//                switch (ccdResponse.getNotificationType()) {
//                    case DWP_RESPONSE_RECEIVED: {
//                        if (events.getValue().getEventType().equals(DWP_RESPONSE_RECEIVED)) {
//                            return events.getValue().getDateTime().plusDays(2);
//                        }
//                        break;
//                    }
//                    default:
//                        break;
//                }
//            }
//        }
//        ReminderException reminderException = new ReminderException(
//                new Exception("Could not find reminder date for case reference" + ccdResponse.getCaseReference()));
//        LOG.error("Reminder date not found", reminderException);
//        throw reminderException;
//    }
//}
