package uk.gov.hmcts.reform.sscs.callback.handlers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.exception.NotificationServiceException;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.NotificationService;
import uk.gov.hmcts.reform.sscs.service.RetryNotificationService;

@Service
public class FilterNotificationsEventsHandler implements CallbackHandler {

    private final NotificationService notificationService;
    private static final int RETRY = 1;
    private RetryNotificationService retryNotificationService;

    @Autowired
    public FilterNotificationsEventsHandler(NotificationService notificationService, RetryNotificationService retryNotificationService) {
        this.notificationService = notificationService;
        this.retryNotificationService = retryNotificationService;
    }

    @Override
    public boolean canHandle(SscsCaseDataWrapper callback) {
        return callback.getNotificationEventType() == NotificationEventType.DWP_UPLOAD_RESPONSE_NOTIFICATION
                || callback.getNotificationEventType() == NotificationEventType.DIRECTION_ISSUED
                || callback.getNotificationEventType() == NotificationEventType.ADMIN_APPEAL_WITHDRAWN
                || callback.getNotificationEventType() == NotificationEventType.VALID_APPEAL_CREATED
                || callback.getNotificationEventType() == NotificationEventType.NON_COMPLIANT_NOTIFICATION
                || callback.getNotificationEventType() == NotificationEventType.REISSUE_DOCUMENT;
    }

    @Override
    public void handle(SscsCaseDataWrapper callback) {
        if (!canHandle(callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        final CcdNotificationWrapper notificationWrapper = new CcdNotificationWrapper(callback);
        try {
            notificationService.manageNotificationAndSubscription(notificationWrapper);
        } catch (NotificationServiceException e) {
            retryNotificationService.rescheduleIfHandledGovNotifyErrorStatus(RETRY, notificationWrapper, e);
            throw e;
        }
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }
}
