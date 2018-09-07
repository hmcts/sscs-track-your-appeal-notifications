package uk.gov.hmcts.reform.sscs.service.reminder;

import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;

public interface ReminderHandler {

    boolean canHandle(NotificationWrapper wrapper);

    void handle(NotificationWrapper wrapper);
}
