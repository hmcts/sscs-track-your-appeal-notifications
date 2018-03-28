package uk.gov.hmcts.sscs.domain.reminder;

import static uk.gov.hmcts.sscs.domain.notify.EventType.DWP_RESPONSE_RECEIVED;
import static uk.gov.hmcts.sscs.domain.notify.EventType.EVIDENCE_REMINDER;

import java.time.ZonedDateTime;
import java.util.Objects;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.notify.Event;
import uk.gov.hmcts.sscs.domain.notify.EventType;

public class Reminder {

    private String name;
    private Action action;
    private Trigger trigger;
    private final String NAME_PREFIX = "SSCS_";

    public Reminder(CcdResponse ccdResponse, String callbackUrl) throws Exception {
        String reminderType = findReminderType(ccdResponse.getNotificationType()).getId();
        this.name = NAME_PREFIX + reminderType;
        this.action = new Action(ccdResponse.getAppellantSubscription().getAppealNumber(), reminderType, callbackUrl);
        this.trigger = new Trigger(findReminderDate(ccdResponse));
    }

    public EventType findReminderType(EventType eventType) throws Exception {
        switch(eventType) {
            case DWP_RESPONSE_RECEIVED: return EVIDENCE_REMINDER;
            default: break;
        }
        throw new Exception("Unknown reminder type");
    }

    public ZonedDateTime findReminderDate(CcdResponse ccdResponse) throws Exception {
        for (Event event : ccdResponse.getEvents()) {
            switch (ccdResponse.getNotificationType()) {
                case DWP_RESPONSE_RECEIVED: {
                    if (event.getEventType().equals(DWP_RESPONSE_RECEIVED)) {
                        return event.getDateTime().plusDays(2);
                    }
                }
                default: break;
            }
        }
        throw new Exception("Could not find reminder date");
    }

    public String getName() {
        return name;
    }

    public Action getAction() {
        return action;
    }

    public Trigger getTrigger() {
        return trigger;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reminder reminder = (Reminder) o;
        return Objects.equals(name, reminder.name)
                && Objects.equals(action, reminder.action)
                && Objects.equals(trigger, reminder.trigger);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, action, trigger);
    }
}
