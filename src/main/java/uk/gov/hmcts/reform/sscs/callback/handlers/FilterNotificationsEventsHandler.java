package uk.gov.hmcts.reform.sscs.callback.handlers;

import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DIRECTION_ISSUED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DWP_UPLOAD_RESPONSE_NOTIFICATION;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
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

    private static final Set<NotificationEventType> ALLOWED_EVENTS =
            ImmutableSet.of(DWP_UPLOAD_RESPONSE_NOTIFICATION, DIRECTION_ISSUED);

    @Autowired
    public FilterNotificationsEventsHandler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public boolean canHandle(SscsCaseDataWrapper callback) {
        return ALLOWED_EVENTS.contains(callback.getNotificationEventType());
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
