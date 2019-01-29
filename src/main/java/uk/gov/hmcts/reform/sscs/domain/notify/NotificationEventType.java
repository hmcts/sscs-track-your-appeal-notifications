package uk.gov.hmcts.reform.sscs.domain.notify;

import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;

public enum NotificationEventType {

    ADJOURNED_NOTIFICATION("hearingAdjourned", true, false, false, false, false),
    SYA_APPEAL_CREATED_NOTIFICATION("appealCreated", true, true, false, true, false),
    RESEND_APPEAL_CREATED_NOTIFICATION("resendAppealCreated", true, true, false, true, false),
    APPEAL_LAPSED_NOTIFICATION("appealLapsed", true, true, false, false, false),
    APPEAL_RECEIVED_NOTIFICATION("appealReceived", true, true, false, false, false),
    APPEAL_WITHDRAWN_NOTIFICATION("appealWithdrawn", true, true, false, false, false),
    APPEAL_DORMANT_NOTIFICATION("appealDormant", true, true, false, false, false),
    EVIDENCE_RECEIVED_NOTIFICATION("evidenceReceived", true, true, true, false, false),
    DWP_RESPONSE_RECEIVED_NOTIFICATION("responseReceived", true, true, true, false, false),
    HEARING_BOOKED_NOTIFICATION("hearingBooked", true, false, false, false, false),
    POSTPONEMENT_NOTIFICATION("hearingPostponed", true, false, false, false, false),
    SUBSCRIPTION_CREATED_NOTIFICATION("subscriptionCreated", true, true, false, false, false),
    SUBSCRIPTION_UPDATED_NOTIFICATION("subscriptionUpdated", true, true, false, true, false),
    SUBSCRIPTION_OLD_NOTIFICATION("subscriptionOld", false, true, false, true, false),
    EVIDENCE_REMINDER_NOTIFICATION("evidenceReminder", true, true, false, false, true),
    FIRST_HEARING_HOLDING_REMINDER_NOTIFICATION("hearingHoldingReminder", true, false, false, false, true),
    SECOND_HEARING_HOLDING_REMINDER_NOTIFICATION("secondHearingHoldingReminder", true, false, false, false, true),
    THIRD_HEARING_HOLDING_REMINDER_NOTIFICATION("thirdHearingHoldingReminder", true, false, false, false, true),
    FINAL_HEARING_HOLDING_REMINDER_NOTIFICATION("finalHearingHoldingReminder", true, false, false, false, true),
    HEARING_REMINDER_NOTIFICATION("hearingReminder", true, false, false, false, true),
    DWP_RESPONSE_LATE_REMINDER_NOTIFICATION("dwpResponseLateReminder", true, true, false, false, true),
    QUESTION_ROUND_ISSUED_NOTIFICATION("question_round_issued", false, false, true, false, false),
    QUESTION_DEADLINE_ELAPSED_NOTIFICATION("question_deadline_elapsed", false, false, true, false, false),
    QUESTION_DEADLINE_REMINDER_NOTIFICATION("question_deadline_reminder", false, false, true, false, true),
    HEARING_REQUIRED_NOTIFICATION("continuous_online_hearing_relisted", false, false, true, false, false),
    VIEW_ISSUED("decision_issued", false, false, true, false, false),
    DECISION_ISSUED_2("decision_issued_2", false, false, true, false, false), // placeholder until COH name this notification
    STRUCK_OUT("struckOut", true, true, false, true, false),
    INTERLOC_VALID_APPEAL("interlocValidAppeal", true, true, true, true, false),
    DO_NOT_SEND("");

    private String id;
    private boolean sendForOralCase;
    private boolean sendForPaperCase;
    private boolean sendForCohCase;
    private boolean allowOutOfHours;
    private boolean isReminder;

    NotificationEventType(String id) {
        this.id = id;
    }

    NotificationEventType(String id, Boolean sendForOralCase, Boolean sendForPaperCase, Boolean sendForCohCase, Boolean allowOutOfHours, Boolean isReminder) {
        this.id = id;
        this.sendForOralCase = sendForOralCase;
        this.sendForPaperCase = sendForPaperCase;
        this.sendForCohCase = sendForCohCase;
        this.allowOutOfHours = allowOutOfHours;
        this.isReminder = isReminder;
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

    public boolean isReminder() {
        return isReminder;
    }

    public boolean isAllowOutOfHours() {
        return allowOutOfHours;
    }
}
