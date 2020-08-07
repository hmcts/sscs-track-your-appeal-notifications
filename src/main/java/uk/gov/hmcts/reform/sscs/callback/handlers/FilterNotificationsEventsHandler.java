package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ADJOURNED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ADMIN_APPEAL_WITHDRAWN;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_DORMANT_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_LAPSED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_RECEIVED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_WITHDRAWN_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DECISION_ISSUED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DIRECTION_ISSUED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DWP_APPEAL_LAPSED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DWP_RESPONSE_RECEIVED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DWP_UPLOAD_RESPONSE_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.EVIDENCE_RECEIVED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.HEARING_BOOKED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ISSUE_FINAL_DECISION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.NON_COMPLIANT_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.POSTPONEMENT_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.REISSUE_DOCUMENT;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.REQUEST_INFO_INCOMPLETE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.RESEND_APPEAL_CREATED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.STRUCK_OUT;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SUBSCRIPTION_UPDATED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.VALID_APPEAL_CREATED;

import java.util.List;
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

    private static final List<NotificationEventType> EVENTS_LIST = unmodifiableList(asList(
            ADJOURNED_NOTIFICATION,
            ADMIN_APPEAL_WITHDRAWN,
            APPEAL_DORMANT_NOTIFICATION,
            APPEAL_LAPSED_NOTIFICATION,
            APPEAL_RECEIVED_NOTIFICATION,
            APPEAL_WITHDRAWN_NOTIFICATION,
            DECISION_ISSUED,
            DIRECTION_ISSUED,
            DWP_APPEAL_LAPSED_NOTIFICATION,
            DWP_RESPONSE_RECEIVED_NOTIFICATION,
            DWP_UPLOAD_RESPONSE_NOTIFICATION,
            EVIDENCE_RECEIVED_NOTIFICATION,
            HEARING_BOOKED_NOTIFICATION,
            ISSUE_ADJOURNMENT_NOTICE,
            ISSUE_FINAL_DECISION,
            NON_COMPLIANT_NOTIFICATION,
            POSTPONEMENT_NOTIFICATION,
            REISSUE_DOCUMENT,
            REQUEST_INFO_INCOMPLETE,
            RESEND_APPEAL_CREATED_NOTIFICATION,
            STRUCK_OUT,
            SUBSCRIPTION_UPDATED_NOTIFICATION,
            VALID_APPEAL_CREATED
    ));
    private final NotificationService notificationService;

    @Autowired
    public FilterNotificationsEventsHandler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public boolean canHandle(SscsCaseDataWrapper callback) {
        return EVENTS_LIST.contains(callback.getNotificationEventType());
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
