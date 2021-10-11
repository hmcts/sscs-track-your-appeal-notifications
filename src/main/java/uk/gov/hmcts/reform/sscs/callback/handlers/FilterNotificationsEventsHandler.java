package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.ProcessRequestAction;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.exception.NotificationServiceException;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.NotificationService;
import uk.gov.hmcts.reform.sscs.service.RetryNotificationService;

@Service
public class FilterNotificationsEventsHandler implements CallbackHandler {

    private static final List<NotificationEventType> TURN_OFF_EVENTS_LIST = List.of(POSTPONEMENT_NOTIFICATION);
    private static final List<NotificationEventType> EVENTS_LIST = List.of(
            ADJOURNED_NOTIFICATION,
            ADMIN_APPEAL_WITHDRAWN,
            APPEAL_DORMANT_NOTIFICATION,
            APPEAL_LAPSED_NOTIFICATION,
            APPEAL_RECEIVED_NOTIFICATION,
            APPEAL_WITHDRAWN_NOTIFICATION,
            DECISION_ISSUED,
            DIRECTION_ISSUED,
            DECISION_ISSUED_WELSH,
            DIRECTION_ISSUED_WELSH,
            DWP_APPEAL_LAPSED_NOTIFICATION,
            DWP_RESPONSE_RECEIVED_NOTIFICATION,
            DWP_UPLOAD_RESPONSE_NOTIFICATION,
            EVIDENCE_RECEIVED_NOTIFICATION,
            HEARING_BOOKED_NOTIFICATION,
            ISSUE_ADJOURNMENT_NOTICE,
            ISSUE_ADJOURNMENT_NOTICE_WELSH,
            ISSUE_FINAL_DECISION,
            ISSUE_FINAL_DECISION_WELSH,
            NON_COMPLIANT_NOTIFICATION,
            DRAFT_TO_NON_COMPLIANT_NOTIFICATION,
            POSTPONEMENT_NOTIFICATION,
            REISSUE_DOCUMENT,
            REQUEST_INFO_INCOMPLETE,
            RESEND_APPEAL_CREATED_NOTIFICATION,
            STRUCK_OUT,
            SUBSCRIPTION_UPDATED_NOTIFICATION,
            VALID_APPEAL_CREATED,
            DRAFT_TO_VALID_APPEAL_CREATED,
            REVIEW_CONFIDENTIALITY_REQUEST,
            PROCESS_AUDIO_VIDEO,
            PROCESS_AUDIO_VIDEO_WELSH,
            JOINT_PARTY_ADDED,
            ACTION_HEARING_RECORDING_REQUEST,
            ACTION_POSTPONEMENT_REQUEST_WELSH
    );
    private final NotificationService notificationService;
    private static final int RETRY = 1;
    private final RetryNotificationService retryNotificationService;

    @Autowired
    public FilterNotificationsEventsHandler(NotificationService notificationService, RetryNotificationService retryNotificationService) {
        this.notificationService = notificationService;
        this.retryNotificationService = retryNotificationService;
    }

    @Override
    public boolean canHandle(SscsCaseDataWrapper callback) {
        final boolean eventInTheList = nonNull(callback.getNotificationEventType())
                && EVENTS_LIST.contains(callback.getNotificationEventType())
                && !TURN_OFF_EVENTS_LIST.contains(callback.getNotificationEventType());

        return eventInTheList || (ACTION_POSTPONEMENT_REQUEST.equals(callback.getNotificationEventType())
                && !ProcessRequestAction.SEND_TO_JUDGE.getValue().equals(
                callback.getOldSscsCaseData().getPostponementRequest().getActionPostponementRequestSelected()));
    }

    @Override
    public void handle(SscsCaseDataWrapper callback) {
        if (!canHandle(callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        final CcdNotificationWrapper notificationWrapper = new CcdNotificationWrapper(callback);
        try {
            notificationService.manageNotificationAndSubscription(notificationWrapper, false);
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
