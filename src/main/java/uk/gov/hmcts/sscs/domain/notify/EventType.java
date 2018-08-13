package uk.gov.hmcts.sscs.domain.notify;


public enum EventType {

    ADJOURNED("hearingAdjourned"),
    SYA_APPEAL_CREATED("appealCreated"),
    APPEAL_LAPSED("appealLapsed"),
    APPEAL_RECEIVED("appealReceived"),
    APPEAL_WITHDRAWN("appealWithdrawn"),
    APPEAL_DORMANT("appealDormant"),
    DWP_RESPONSE_RECEIVED("responseReceived"),
    EVIDENCE_RECEIVED("evidenceReceived"),
    HEARING_BOOKED("hearingBooked"),
    POSTPONEMENT("hearingPostponed"),
    SUBSCRIPTION_CREATED("subscriptionCreated"),
    SUBSCRIPTION_UPDATED("subscriptionUpdated"),
    EVIDENCE_REMINDER("evidenceReminder"),
    FIRST_HEARING_HOLDING_REMINDER("hearingHoldingReminder"),
    SECOND_HEARING_HOLDING_REMINDER("secondHearingHoldingReminder"),
    THIRD_HEARING_HOLDING_REMINDER("thirdHearingHoldingReminder"),
    FINAL_HEARING_HOLDING_REMINDER("finalHearingHoldingReminder"),
    HEARING_REMINDER("hearingReminder"),
    DWP_RESPONSE_LATE_REMINDER("dwpResponseLateReminder"),
    DO_NOT_SEND(""),

    QUESTION_ROUND_ISSUED("question_round_issued");

    private String id;

    EventType(String id) {
        this.id = id;
    }

    public static EventType getNotificationById(String id) {
        EventType b = null;
        for (EventType type : EventType.values()) {
            if (type.getId().equals(id)) {
                b = type;
            }
        }
        return b;
    }

    public String getId() {
        return id;
    }

}
