package uk.gov.hmcts.sscs.domain.notify;


public enum NotificationType {
    ADJOURNED("hearingAdjourned"),
    APPEAL_RECEIVED("appealReceived"),
    DWP_RESPONSE_RECEIVED("responseReceived"),
    EVIDENCE_RECEIVED("evidenceReceived");

    private String id;

    NotificationType(String id) {
        this.id = id;
    }

    public static NotificationType getNotificationById(String id) {
        NotificationType b = null;
        for (NotificationType type : NotificationType.values()) {
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
