package uk.gov.hmcts.reform.sscs.domain.notify;

import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;

public enum NotificationEventType {

    ADJOURNED_NOTIFICATION("hearingAdjourned", true, false, false, false),
    SYA_APPEAL_CREATED_NOTIFICATION("appealCreated", true, true, false, true),
    RESEND_APPEAL_CREATED_NOTIFICATION("resendAppealCreated", true, true, false, true),
    APPEAL_LAPSED_NOTIFICATION("appealLapsed", true, true, false, false),
    APPEAL_RECEIVED_NOTIFICATION("appealReceived", true, true, false, false),
    APPEAL_WITHDRAWN_NOTIFICATION("appealWithdrawn", true, true, false, false),
    APPEAL_DORMANT_NOTIFICATION("appealDormant", true, true, false, false),
    EVIDENCE_RECEIVED_NOTIFICATION("evidenceReceived", true, true, true, false),
    DWP_RESPONSE_RECEIVED_NOTIFICATION("responseReceived", true, true, true, false),
    HEARING_BOOKED_NOTIFICATION("hearingBooked", true, false, false, false),
    POSTPONEMENT_NOTIFICATION("hearingPostponed", true, false, false, false),
    SUBSCRIPTION_CREATED_NOTIFICATION("subscriptionCreated", true, true, false, false),
    SUBSCRIPTION_UPDATED_NOTIFICATION("subscriptionUpdated", true, true, false, true),
    SUBSCRIPTION_OLD_NOTIFICATION("subscriptionOld", false, true, false, true),
    EVIDENCE_REMINDER_NOTIFICATION("evidenceReminder", true, true, false, false),
    FIRST_HEARING_HOLDING_REMINDER_NOTIFICATION("hearingHoldingReminder", true, false, false, false),
    SECOND_HEARING_HOLDING_REMINDER_NOTIFICATION("secondHearingHoldingReminder", true, false, false, false),
    THIRD_HEARING_HOLDING_REMINDER_NOTIFICATION("thirdHearingHoldingReminder", true, false, false, false),
    FINAL_HEARING_HOLDING_REMINDER_NOTIFICATION("finalHearingHoldingReminder", true, false, false, false),
    HEARING_REMINDER_NOTIFICATION("hearingReminder", true, false, false, false),
    DWP_RESPONSE_LATE_REMINDER_NOTIFICATION("dwpResponseLateReminder", true, true, false, false),
    QUESTION_ROUND_ISSUED_NOTIFICATION("question_round_issued", false, false, true, false),
    QUESTION_DEADLINE_ELAPSED_NOTIFICATION("question_deadline_elapsed", false, false, true, false),
    QUESTION_DEADLINE_REMINDER_NOTIFICATION("question_deadline_reminder", false, false, true, false),
    HEARING_REQUIRED_NOTIFICATION("continuous_online_hearing_relisted", false, false, true, false),
    VIEW_ISSUED("decision_issued", false, false, true, false),
    DECISION_ISSUED_2("decision_issued_2", false, false, true, false), // placeholder until COH name this notification
    STRUCK_OUT("struckOut", true, true, false, true),
    DO_NOT_SEND("");

    private String id;
    private boolean sendForOralCase;
    private boolean sendForPaperCase;
    private boolean sendForCohCase;
    private boolean allowOutOfHours;

    NotificationEventType(String id) {
        this.id = id;
    }

    NotificationEventType(String id, Boolean sendForOralCase, Boolean sendForPaperCase, Boolean sendForCohCase, Boolean allowOutOfHours) {
        this.id = id;
        this.sendForOralCase = sendForOralCase;
        this.sendForPaperCase = sendForPaperCase;
        this.sendForCohCase = sendForCohCase;
        this.allowOutOfHours = allowOutOfHours;
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

    public static boolean checkEvent(String event) {
        for (NotificationEventType type : NotificationEventType.values()) {
            if (event.equals(type.getId())) {
                return true;
            }
        }
        return false;
    }

    public String getId() {
        return id;
    }

    public boolean isSendForPaperCase() {
        return sendForPaperCase;
    }

    public boolean isSendForOralCase() {
        return sendForOralCase;
    }

    public boolean isSendForCohCase() {
        return sendForCohCase;
    }

    public boolean isAllowOutOfHours() {
        return allowOutOfHours;
    }
}
