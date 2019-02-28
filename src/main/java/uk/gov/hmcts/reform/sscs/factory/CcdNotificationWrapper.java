package uk.gov.hmcts.reform.sscs.factory;

import static uk.gov.hmcts.reform.sscs.config.AppealHearingType.*;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPOINTEE;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.service.scheduler.CcdActionSerializer;

public class CcdNotificationWrapper implements NotificationWrapper {
    private static final String HEARING_TYPE_ONLINE_RESOLUTION = "cor";

    private final SscsCaseDataWrapper responseWrapper;

    public CcdNotificationWrapper(SscsCaseDataWrapper responseWrapper) {
        this.responseWrapper = responseWrapper;
    }

    @Override
    public NotificationEventType getNotificationType() {
        return responseWrapper.getNotificationEventType();
    }

    @Override
    public SscsCaseData getNewSscsCaseData() {
        return responseWrapper.getNewSscsCaseData();
    }

    @Override
    public Subscription getAppellantSubscription() {
        return responseWrapper.getNewSscsCaseData().getSubscriptions().getAppellantSubscription();
    }

    @Override
    public Subscription getRepresentativeSubscription() {
        return responseWrapper.getNewSscsCaseData().getSubscriptions().getRepresentativeSubscription();
    }

    @Override
    public Subscription getAppointeeSubscription() {
        return responseWrapper.getNewSscsCaseData().getSubscriptions().getAppointeeSubscription();
    }

    @Override
    public SscsCaseDataWrapper getSscsCaseDataWrapper() {
        return responseWrapper;
    }

    @Override
    public String getCaseId() {
        return responseWrapper.getNewSscsCaseData().getCcdCaseId();
    }

    public AppealHearingType getHearingType() {
        final String hearingType = responseWrapper.getNewSscsCaseData().getAppeal().getHearingType();
        AppealHearingType returnHearingType = REGULAR;
        if (StringUtils.equalsAnyIgnoreCase(HEARING_TYPE_ONLINE_RESOLUTION, hearingType)) {
            returnHearingType = ONLINE;
        } else if (StringUtils.equalsAnyIgnoreCase(PAPER.name(), hearingType)) {
            returnHearingType = PAPER;
        } else if (StringUtils.equalsAnyIgnoreCase(ORAL.name(), hearingType)) {
            returnHearingType = ORAL;
        }
        return returnHearingType;
    }

    @Override
    public String getSchedulerPayload() {
        return new CcdActionSerializer().serialize(getCaseId());
    }

    @Override
    public SscsCaseData getOldSscsCaseData() {
        return responseWrapper.getOldSscsCaseData();
    }

    @Override
    public List<SubscriptionWithType> getSubscriptionsBasedOnNotificationType() {
        List<SubscriptionWithType> subscriptionWithTypeList = new ArrayList<>();

        if (hasAppointee(responseWrapper) && hasAppointeeSubscription(responseWrapper)
                && (SYA_APPEAL_CREATED_NOTIFICATION.equals(getNotificationType())
                    || ADJOURNED_NOTIFICATION.equals(getNotificationType())
                    || APPEAL_RECEIVED_NOTIFICATION.equals(getNotificationType())
                    || APPEAL_LAPSED_NOTIFICATION.equals(getNotificationType())
                    || CASE_UPDATED.equals(getNotificationType())
                    || DWP_RESPONSE_RECEIVED_NOTIFICATION.equals(getNotificationType()) && ORAL.equals(getHearingType())
                    || DWP_RESPONSE_RECEIVED_NOTIFICATION.equals(getNotificationType()) && PAPER.equals(getHearingType())
                    || APPEAL_WITHDRAWN_NOTIFICATION.equals(getNotificationType())
                    || EVIDENCE_RECEIVED_NOTIFICATION.equals(getNotificationType())
                    || HEARING_BOOKED_NOTIFICATION.equals(getNotificationType())
                    || POSTPONEMENT_NOTIFICATION.equals(getNotificationType())
                    || SUBSCRIPTION_UPDATED_NOTIFICATION.equals(getNotificationType())
                    || EVIDENCE_REMINDER_NOTIFICATION.equals(getNotificationType())
                    || HEARING_REMINDER_NOTIFICATION.equals(getNotificationType())
                )
        ) {
            subscriptionWithTypeList.add(new SubscriptionWithType(getAppointeeSubscription(), APPOINTEE));
        } else {
            subscriptionWithTypeList.add(new SubscriptionWithType(getAppellantSubscription(), APPELLANT));
        }

        if (hasRepresentative(responseWrapper) && hasRepresentativeSubscription(responseWrapper)
            && (APPEAL_LAPSED_NOTIFICATION.equals(getNotificationType())
                || APPEAL_WITHDRAWN_NOTIFICATION.equals(getNotificationType())
                || EVIDENCE_RECEIVED_NOTIFICATION.equals(getNotificationType())
                || SYA_APPEAL_CREATED_NOTIFICATION.equals(getNotificationType())
                || RESEND_APPEAL_CREATED_NOTIFICATION.equals(getNotificationType())
                || APPEAL_DORMANT_NOTIFICATION.equals(getNotificationType())
                || ADJOURNED_NOTIFICATION.equals(getNotificationType())
                || APPEAL_RECEIVED_NOTIFICATION.equals(getNotificationType())
                || POSTPONEMENT_NOTIFICATION.equals(getNotificationType())
                || HEARING_BOOKED_NOTIFICATION.equals(getNotificationType())
                || SUBSCRIPTION_UPDATED_NOTIFICATION.equals(getNotificationType())
                || CASE_UPDATED.equals(getNotificationType()))) {
            subscriptionWithTypeList.add(new SubscriptionWithType(getRepresentativeSubscription(), REPRESENTATIVE));
        }
        return subscriptionWithTypeList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CcdNotificationWrapper that = (CcdNotificationWrapper) o;
        return Objects.equals(responseWrapper, that.responseWrapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(responseWrapper);
    }
}
