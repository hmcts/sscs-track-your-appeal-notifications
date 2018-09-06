package uk.gov.hmcts.reform.sscs.domain.notify;

import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;

public enum NotificationEventType {

    ADJOURNED_NOTIFICATION("hearingAdjourned"),
    SYA_APPEAL_CREATED_NOTIFICATION("appealCreated"),
    APPEAL_LAPSED_NOTIFICATION("appealLapsed"),
    APPEAL_RECEIVED_NOTIFICATION("appealReceived"),
    APPEAL_WITHDRAWN_NOTIFICATION("appealWithdrawn"),
    APPEAL_DORMANT_NOTIFICATION("appealDormant"),
    DWP_RESPONSE_RECEIVED_NOTIFICATION("responseReceived"),
    EVIDENCE_RECEIVED_NOTIFICATION("evidenceReceived"),
    HEARING_BOOKED_NOTIFICATION("hearingBooked"),
    POSTPONEMENT_NOTIFICATION("hearingPostponed"),
    SUBSCRIPTION_CREATED_NOTIFICATION("subscriptionCreated"),
    SUBSCRIPTION_UPDATED_NOTIFICATION("subscriptionUpdated"),
    EVIDENCE_REMINDER_NOTIFICATION("evidenceReminder"),
    FIRST_HEARING_HOLDING_REMINDER_NOTIFICATION("hearingHoldingReminder"),
    SECOND_HEARING_HOLDING_REMINDER_NOTIFICATION("secondHearingHoldingReminder"),
    THIRD_HEARING_HOLDING_REMINDER_NOTIFICATION("thirdHearingHoldingReminder"),
    FINAL_HEARING_HOLDING_REMINDER_NOTIFICATION("finalHearingHoldingReminder"),
    HEARING_REMINDER_NOTIFICATION("hearingReminder"),
    DWP_RESPONSE_LATE_REMINDER_NOTIFICATION("dwpResponseLateReminder"),
    QUESTION_ROUND_ISSUED_NOTIFICATION("question_round_issued"),
    QUESTION_DEADLINE_ELAPSED_NOTIFICATION("question_deadline_elapsed"),
    DO_NOT_SEND("");

    private String id;

    NotificationEventType(String id) {
        this.id = id;
    }

    public static NotificationEventType getNotificationById(String id) {
        NotificationEventType b = null;
        for (NotificationEventType type : NotificationEventType.values()) {
            if (type.getId().equals(id)) {
                b = type;
            }
        }
        return b;
    }

    public static NotificationEventType getNotificationByCcdEvent(EventType ccdEventType) {
        NotificationEventType b = null;
        for (NotificationEventType type : NotificationEventType.values()) {
            if (ccdEventType.getCcdType().equals(type.getId())) {
                b = type;
            }
        }
        return b;
    }

    public String getId() {
        return id;
    }

}
