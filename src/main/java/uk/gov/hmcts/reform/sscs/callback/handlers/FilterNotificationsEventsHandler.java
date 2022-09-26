package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.config.NotificationEventTypeLists.EVENTS_TO_HANDLE;
import static uk.gov.hmcts.reform.sscs.config.NotificationEventTypeLists.EVENTS_TO_NOT_HANDLE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.ProcessRequestAction;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.exception.NotificationServiceException;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.NotificationService;
import uk.gov.hmcts.reform.sscs.service.RetryNotificationService;

@Service
public class FilterNotificationsEventsHandler implements CallbackHandler {


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
            && EVENTS_TO_HANDLE.contains(callback.getNotificationEventType())
            && !EVENTS_TO_NOT_HANDLE.contains(callback.getNotificationEventType());

        return eventInTheList
            || shouldActionPostponementBeNotified(callback)
            || hasNewAppointeeAddedForAppellantDecesedCase(callback);
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

    private boolean shouldActionPostponementBeNotified(SscsCaseDataWrapper callback) {
        return ACTION_POSTPONEMENT_REQUEST.equals(callback.getNotificationEventType())
                && !ProcessRequestAction.SEND_TO_JUDGE.getValue().equals(
                callback.getOldSscsCaseData().getPostponementRequest().getActionPostponementRequestSelected());
    }

    private boolean hasNewAppointeeAddedForAppellantDecesedCase(SscsCaseDataWrapper callback) {
        if (!(DEATH_OF_APPELLANT.equals(callback.getNotificationEventType())
                || PROVIDE_APPOINTEE_DETAILS.equals(callback.getNotificationEventType()))) {
            return false;
        }

        Appointee appointeeBefore = null;
        if (callback.getOldSscsCaseData() != null
                && "yes".equalsIgnoreCase(callback.getOldSscsCaseData().getAppeal().getAppellant().getIsAppointee())
                && callback.getOldSscsCaseData().getAppeal().getAppellant().getAppointee() != null) {
            appointeeBefore = callback.getOldSscsCaseData().getAppeal().getAppellant().getAppointee();
        }

        Appointee appointeeAfter = null;
        if ("yes".equalsIgnoreCase(callback.getNewSscsCaseData().getAppeal().getAppellant().getIsAppointee())
                && callback.getNewSscsCaseData().getAppeal().getAppellant().getAppointee() != null) {
            appointeeAfter = callback.getNewSscsCaseData().getAppeal().getAppellant().getAppointee();
        }


        return ((appointeeBefore == null && appointeeAfter != null)
                || (appointeeBefore != null && appointeeAfter != null && !appointeeBefore.equals(appointeeAfter)));
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }
}
