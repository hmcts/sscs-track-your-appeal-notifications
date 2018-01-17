package uk.gov.hmcts.sscs.domain.notify;


public enum NotificationType {
    APPEAL_RECEIVED("appealReceivedNotification", 1, true);

    private String id;
    private final int order;
    private boolean notifiable;

    NotificationType(String id, int order, boolean notifiable) {
        this.id = id;
        this.order = order;
        this.notifiable = notifiable;
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

    public int getOrder() {
        return order;
    }

    public boolean isStatusEvent() {
        return order > 0;
    }

    public boolean isNotifiable() {
        return notifiable;
    }
}
