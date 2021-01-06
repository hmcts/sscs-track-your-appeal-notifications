package uk.gov.hmcts.reform.sscs.domain.notify;

import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;

public enum NotificationEventType {

    ADJOURNED_NOTIFICATION("hearingAdjourned", true, false, false, false, false, 0),
    SYA_APPEAL_CREATED_NOTIFICATION("appealCreated", true, true, false, true, false, 0),
    RESEND_APPEAL_CREATED_NOTIFICATION("resendAppealCreated", true, true, false, true, false, 0),
    APPEAL_LAPSED_NOTIFICATION("appealLapsed", true, true, false, false, false, 0),
    HMCTS_APPEAL_LAPSED_NOTIFICATION("hmctsLapseCase", true, true, false, false, false, 0),
    DWP_APPEAL_LAPSED_NOTIFICATION("confirmLapsed", true, true, false, false, false, 0),
    APPEAL_RECEIVED_NOTIFICATION("appealReceived", true, true, false, false, false, 300L),
    APPEAL_WITHDRAWN_NOTIFICATION("appealWithdrawn", true, true, false, false, false, 0),
    APPEAL_DORMANT_NOTIFICATION("appealDormant", true, true, false, false, false, 0),
    EVIDENCE_RECEIVED_NOTIFICATION("evidenceReceived", true, true, true, false, false, 0),
    DWP_RESPONSE_RECEIVED_NOTIFICATION("responseReceived", true, true, true, false, false, 0),
    DWP_UPLOAD_RESPONSE_NOTIFICATION("dwpUploadResponse", true, true, true, false, false, 0),
    HEARING_BOOKED_NOTIFICATION("hearingBooked", true, false, false, false, false, 0),
    POSTPONEMENT_NOTIFICATION("hearingPostponed", true, false, false, false, false, 0),
    SUBSCRIPTION_CREATED_NOTIFICATION("subscriptionCreated", true, true, false, false, false, 0),
    SUBSCRIPTION_UPDATED_NOTIFICATION("subscriptionUpdated", true, true, false, true, false, 0),
    SUBSCRIPTION_OLD_NOTIFICATION("subscriptionOld", false, true, false, true, false, 0),
    EVIDENCE_REMINDER_NOTIFICATION("evidenceReminder", true, true, false, false, true, 0),
    HEARING_REMINDER_NOTIFICATION("hearingReminder", true, false, false, false, true, 0),
    STRUCK_OUT("struckOut", true, true, false, false, false, 0),
    CASE_UPDATED("caseUpdated", false, false, false, false, false, 0),
    DIRECTION_ISSUED("directionIssued", true, true, true, false, false, 0),
    DIRECTION_ISSUED_WELSH("directionIssuedWelsh", true, true, true, false, false, 0),
    DECISION_ISSUED("decisionIssued", true, true, true, false, false, 0),
    DECISION_ISSUED_WELSH("decisionIssuedWelsh", true, true, true, false, false, 0),
    ISSUE_FINAL_DECISION("issueFinalDecision", true, true, true, false, false, 0),
    ISSUE_ADJOURNMENT_NOTICE("issueAdjournmentNotice", true, true, true, false, false, 0),
    ISSUE_ADJOURNMENT_NOTICE_WELSH("issueAdjournmentNoticeWelsh", true, true, true, false, false, 0),
    VALID_APPEAL_CREATED("validAppealCreated", true, true, false, true, false, 240L),
    REQUEST_INFO_INCOMPLETE("requestInfoIncompleteApplication", true, true, true, false, false, 0),
    JUDGE_DECISION_APPEAL_TO_PROCEED("judgeDecisionAppealToProceed", true, true, true, false, false, 0),
    TCW_DECISION_APPEAL_TO_PROCEED("tcwDecisionAppealToProceed", true, true, true, false, false, 0),
    NON_COMPLIANT_NOTIFICATION("nonCompliant", true, true, true, false, false, 0),
    ADMIN_APPEAL_WITHDRAWN("adminAppealWithdrawn", true, true, true, false, false, 0),
    REVIEW_CONFIDENTIALITY_REQUEST("reviewConfidentialityRequest", true, true, true, false, false, 0),
    JOINT_PARTY_ADDED("jointPartyAdded", true, true, true, true, false, 0),
    // Allow out of hours for this event as we rely on the case data to decide who to send to. It could get out of sync if we wait a few hours to send, for example they could try to reissue to 2 parties so this event would be triggered twice.
    // If the reminder service looks the case up from CCD, the original request for who to send the notification to will be lost and the second party would receive the notification twice.
    REISSUE_DOCUMENT("reissueDocument", true, true, true, true, false, 0),
    DO_NOT_SEND("");

    private String id;
    private boolean sendForOralCase;
    private boolean sendForPaperCase;
    private boolean sendForCohCase;
    private boolean allowOutOfHours;
    private boolean isReminder;
    private long delayInSeconds;

    NotificationEventType(String id) {
        this.id = id;
    }

    NotificationEventType(String id, boolean sendForOralCase, boolean sendForPaperCase, boolean sendForCohCase, boolean allowOutOfHours, boolean isReminder, long delayInSeconds) {
        this.id = id;
        this.sendForOralCase = sendForOralCase;
        this.sendForPaperCase = sendForPaperCase;
        this.sendForCohCase = sendForCohCase;
        this.allowOutOfHours = allowOutOfHours;
        this.isReminder = isReminder;
        this.delayInSeconds = delayInSeconds;
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

    public boolean isToBeDelayed() {
        return delayInSeconds > 0;
    }

    public long getDelayInSeconds() {
        return delayInSeconds;
    }
}
