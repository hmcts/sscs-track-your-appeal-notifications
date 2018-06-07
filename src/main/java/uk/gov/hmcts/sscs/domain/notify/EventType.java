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
    HEARING_HOLDING_REMINDER("hearingHoldingReminder"),
    FINAL_HEARING_HOLDING_REMINDER("finalHearingHoldingReminder"),
    HEARING_REMINDER("hearingReminder"),
    DO_NOT_SEND("");

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
