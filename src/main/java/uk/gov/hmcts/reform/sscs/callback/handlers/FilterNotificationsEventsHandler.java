package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.GAPS;
import static uk.gov.hmcts.reform.sscs.config.NotificationEventTypeLists.EVENTS_TO_HANDLE;
import static uk.gov.hmcts.reform.sscs.config.NotificationEventTypeLists.EVENTS_TO_HANDLE_POSTHEARINGS_A;
import static uk.gov.hmcts.reform.sscs.config.NotificationEventTypeLists.EVENTS_TO_HANDLE_POSTHEARINGS_B;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DEATH_OF_APPELLANT;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.HEARING_BOOKED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.POSTPONEMENT_GRANTED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.POSTPONEMENT_REFUSED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.PROVIDE_APPOINTEE_DETAILS;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.ProcessRequestAction;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.exception.NotificationServiceException;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.NotificationService;
import uk.gov.hmcts.reform.sscs.service.RetryNotificationService;

@Service
@Slf4j
@RequiredArgsConstructor
public class FilterNotificationsEventsHandler implements CallbackHandler {
    private final NotificationService notificationService;
    private static final int RETRY = 1;
    private final RetryNotificationService retryNotificationService;

    @Value("${feature.postHearings.enabled}")
    private boolean isPostHearingsEnabled;

    @Value("${feature.postHearingsB.enabled}")
    private boolean isPostHearingsBEnabled;

    @Override
    public boolean canHandle(SscsCaseDataWrapper callback) {
        return nonNull(callback.getNotificationEventType())
            && EVENTS_TO_HANDLE.contains(callback.getNotificationEventType())
            || (isPostHearingsEnabled && EVENTS_TO_HANDLE_POSTHEARINGS_A.contains(callback.getNotificationEventType()))
            || (isPostHearingsBEnabled && EVENTS_TO_HANDLE_POSTHEARINGS_B.contains(callback.getNotificationEventType()))
            || shouldActionPostponementBeNotified(callback)
            || hasNewAppointeeAddedForAppellantDeceasedCase(callback)
            || shouldHandleForHearingRoute(callback);
    }

    @Override
    public void handle(SscsCaseDataWrapper callback) {
        if (!canHandle(callback)) {
            IllegalStateException illegalStateException = new IllegalStateException("Cannot handle callback");
            String caseId = Optional.ofNullable(callback.getOldSscsCaseData())
                .map(SscsCaseData::getCcdCaseId)
                .orElse(null);
            log.error("Cannot handle callback for event {} for caseId {}",
                callback.getNotificationEventType(), caseId, illegalStateException);
            throw illegalStateException;
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
        return (POSTPONEMENT_GRANTED.equals(callback.getNotificationEventType()) || POSTPONEMENT_REFUSED.equals(callback.getNotificationEventType()))
            && !ProcessRequestAction.SEND_TO_JUDGE.getValue().equals(
                callback.getOldSscsCaseData().getPostponementRequest().getActionPostponementRequestSelected());
    }

    private boolean hasNewAppointeeAddedForAppellantDeceasedCase(SscsCaseDataWrapper callback) {
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

    private boolean shouldHandleForHearingRoute(SscsCaseDataWrapper callback) {
        return HEARING_BOOKED == callback.getNotificationEventType()
            && GAPS != callback.getNewSscsCaseData().getSchedulingAndListingFields().getHearingRoute();
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }
}
