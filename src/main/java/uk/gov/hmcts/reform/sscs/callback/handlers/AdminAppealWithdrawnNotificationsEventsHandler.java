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
public class AdminAppealWithdrawnNotificationsEventsHandler implements CallbackHandler {

    private final NotificationService notificationService;

    @Autowired
    public AdminAppealWithdrawnNotificationsEventsHandler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public boolean canHandle(SscsCaseDataWrapper callback) {
        return NotificationEventType.ADMIN_APPEAL_WITHDRAWN == callback.getNotificationEventType();
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
        return DispatchPriority.EARLIEST;
    }
}
