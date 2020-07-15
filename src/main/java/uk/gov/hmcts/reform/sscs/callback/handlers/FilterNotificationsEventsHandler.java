package uk.gov.hmcts.reform.sscs.callback.handlers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.NotificationService;

@Service
public class FilterNotificationsEventsHandler implements CallbackHandler {

    private final NotificationService notificationService;

    @Autowired
    public FilterNotificationsEventsHandler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public boolean canHandle(SscsCaseDataWrapper callback) {
        return callback.getNotificationEventType() == NotificationEventType.DWP_UPLOAD_RESPONSE_NOTIFICATION
                || callback.getNotificationEventType() == NotificationEventType.DIRECTION_ISSUED
                || callback.getNotificationEventType() == NotificationEventType.VALID_APPEAL_CREATED
                || callback.getNotificationEventType() == NotificationEventType.NON_COMPLIANT_NOTIFICATION
                || callback.getNotificationEventType() == NotificationEventType.REISSUE_DOCUMENT;
    }

    @Override
    public void handle(SscsCaseDataWrapper callback) {
        if (!canHandle(callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        notificationService.manageNotificationAndSubscription(new CcdNotificationWrapper(callback));
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }
}
