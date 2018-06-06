package uk.gov.hmcts.sscs.domain.notify;


public enum EventType {
    ADJOURNED("hearingAdjourned", false),
    SYA_APPEAL_CREATED("appealCreated", false),
    APPEAL_LAPSED("appealLapsed", false),
    APPEAL_RECEIVED("appealReceived", false),
    APPEAL_WITHDRAWN("appealWithdrawn", false),
    DWP_RESPONSE_RECEIVED("responseReceived", true),
    EVIDENCE_RECEIVED("evidenceReceived", false),
    HEARING_BOOKED("hearingBooked", true),
    POSTPONEMENT("hearingPostponed", false),
    SUBSCRIPTION_CREATED("subscriptionCreated", false),
    SUBSCRIPTION_UPDATED("subscriptionUpdated", false),
    EVIDENCE_REMINDER("evidenceReminder", false),
    HEARING_REMINDER("hearingReminder", false),
    DO_NOT_SEND("", false);

    private String id;
    private Boolean scheduleReminder;

    EventType(String id, Boolean scheduleReminder) {
        this.id = id;
        this.scheduleReminder = scheduleReminder;
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

    public Boolean isScheduleReminder() {
        return scheduleReminder;
    }

}
