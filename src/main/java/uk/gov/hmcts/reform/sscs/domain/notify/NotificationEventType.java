package uk.gov.hmcts.reform.sscs.domain.notify;

import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;

public enum NotificationEventType {

    ADJOURNED_NOTIFICATION("hearingAdjourned", true, false, false),
    SYA_APPEAL_CREATED_NOTIFICATION("appealCreated", true, true, false),
    APPEAL_LAPSED_NOTIFICATION("appealLapsed", true, false, false),
    APPEAL_RECEIVED_NOTIFICATION("appealReceived", true, true, false),
    APPEAL_WITHDRAWN_NOTIFICATION("appealWithdrawn", true, false, false),
    APPEAL_DORMANT_NOTIFICATION("appealDormant", true, false, false),
    DWP_RESPONSE_RECEIVED_NOTIFICATION("responseReceived", true, false, false),
    EVIDENCE_RECEIVED_NOTIFICATION("evidenceReceived", true, false, false),
    HEARING_BOOKED_NOTIFICATION("hearingBooked", true, false, false),
    POSTPONEMENT_NOTIFICATION("hearingPostponed", true, false, false),
    SUBSCRIPTION_CREATED_NOTIFICATION("subscriptionCreated", true, false, false),
    SUBSCRIPTION_UPDATED_NOTIFICATION("subscriptionUpdated", true, false, false),
    EVIDENCE_REMINDER_NOTIFICATION("evidenceReminder", true, false, false),
    FIRST_HEARING_HOLDING_REMINDER_NOTIFICATION("hearingHoldingReminder", true, false, false),
    SECOND_HEARING_HOLDING_REMINDER_NOTIFICATION("secondHearingHoldingReminder", true, false, false),
    THIRD_HEARING_HOLDING_REMINDER_NOTIFICATION("thirdHearingHoldingReminder", true, false, false),
    FINAL_HEARING_HOLDING_REMINDER_NOTIFICATION("finalHearingHoldingReminder", true, false, false),
    HEARING_REMINDER_NOTIFICATION("hearingReminder", true, false, false),
    DWP_RESPONSE_LATE_REMINDER_NOTIFICATION("dwpResponseLateReminder", true, false, false),
    QUESTION_ROUND_ISSUED_NOTIFICATION("question_round_issued", false, false, true),
    QUESTION_DEADLINE_ELAPSED_NOTIFICATION("question_deadline_elapsed", false, false, true),
    DO_NOT_SEND("");

    private String id;
    private boolean sendForOralCase;
    private boolean sendForPaperCase;
    private boolean sendForCohCase;

    NotificationEventType(String id) {
        this.id = id;
    }

    NotificationEventType(String id, Boolean sendForOralCase, Boolean sendForPaperCase, Boolean sendForCohCase) {
        this.id = id;
        this.sendForOralCase = sendForOralCase;
        this.sendForPaperCase = sendForPaperCase;
        this.sendForCohCase = sendForCohCase;
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

    public boolean isSendForPaperCase() {
        return sendForPaperCase;
    }

    public void setSendForPaperCase(Boolean sendForPaperCase) {
        this.sendForPaperCase = sendForPaperCase;
    }

    public boolean isSendForOralCase() {
        return sendForOralCase;
    }

    public void setSendForOralCase(Boolean sendForOralCase) {
        this.sendForOralCase = sendForOralCase;
    }

    public boolean isSendForCohCase() {
        return sendForCohCase;
    }

    public void setSendForCohCase(boolean sendForCohCase) {
        this.sendForCohCase = sendForCohCase;
    }
}
