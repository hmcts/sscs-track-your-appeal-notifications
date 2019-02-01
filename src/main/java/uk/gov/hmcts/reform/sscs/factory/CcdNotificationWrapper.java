package uk.gov.hmcts.reform.sscs.factory;

import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPOINTEE;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.config.ReceivedVia;
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
        String hearingType = responseWrapper.getNewSscsCaseData().getAppeal().getHearingType();
        if (HEARING_TYPE_ONLINE_RESOLUTION.equalsIgnoreCase(hearingType)) {
            return AppealHearingType.ONLINE;
        }
        if (AppealHearingType.PAPER.name().equalsIgnoreCase(hearingType)) {
            return AppealHearingType.PAPER;
        }
        if (AppealHearingType.ORAL.name().equalsIgnoreCase(hearingType)) {
            return AppealHearingType.ORAL;
        } else {
            return AppealHearingType.REGULAR;
        }
    }

    @Override
    public ReceivedVia getReceivedVia() {
        String receivedVia = getNewSscsCaseData().getAppeal().getReceivedVia();
        if (ReceivedVia.PAPER.name().equalsIgnoreCase(receivedVia)) {
            return ReceivedVia.PAPER;
        }
        else {
            return ReceivedVia.ONLINE;
        }
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

        if (hasAppointee() && (SYA_APPEAL_CREATED_NOTIFICATION.equals(getNotificationType())
            || DWP_RESPONSE_RECEIVED_NOTIFICATION.equals(getNotificationType())
            || INTERLOC_VALID_APPEAL.equals(getNotificationType()))) {
            subscriptionWithTypeList.add(new SubscriptionWithType(getAppointeeSubscription(), APPOINTEE));
        } else {
            subscriptionWithTypeList.add(new SubscriptionWithType(getAppellantSubscription(), APPELLANT));
        }

        if (hasRepresentative() && (APPEAL_LAPSED_NOTIFICATION.equals(getNotificationType())
            || APPEAL_WITHDRAWN_NOTIFICATION.equals(getNotificationType())
            || EVIDENCE_RECEIVED_NOTIFICATION.equals(getNotificationType())
            || SYA_APPEAL_CREATED_NOTIFICATION.equals(getNotificationType())
            || RESEND_APPEAL_CREATED_NOTIFICATION.equals(getNotificationType())
            || APPEAL_DORMANT_NOTIFICATION.equals(getNotificationType())
            || ADJOURNED_NOTIFICATION.equals(getNotificationType())
            || APPEAL_RECEIVED_NOTIFICATION.equals(getNotificationType())
            || DWP_RESPONSE_RECEIVED_NOTIFICATION.equals(getNotificationType())
            || POSTPONEMENT_NOTIFICATION.equals(getNotificationType())
            || HEARING_BOOKED_NOTIFICATION.equals(getNotificationType())
            || INTERLOC_VALID_APPEAL.equals(getNotificationType()))) {
            subscriptionWithTypeList.add(new SubscriptionWithType(getRepresentativeSubscription(), REPRESENTATIVE));
        }
        return subscriptionWithTypeList;
    }

    private boolean hasAppointee() {
        return (responseWrapper.getNewSscsCaseData().getAppeal() != null
            && responseWrapper.getNewSscsCaseData().getAppeal().getAppellant() != null
            && responseWrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee() != null);
    }

    private boolean hasRepresentative() {
        return (responseWrapper.getNewSscsCaseData().getAppeal() != null
            && responseWrapper.getNewSscsCaseData().getAppeal().getRep() != null);
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
