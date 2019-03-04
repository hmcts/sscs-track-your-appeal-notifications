package uk.gov.hmcts.reform.sscs.personalisation;

import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DO_NOT_SEND;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.getNotificationByCcdEvent;

import java.util.Map;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;

@Component
public class SubscriptionPersonalisation extends Personalisation<CcdNotificationWrapper> {

    @Override
    protected Map<String, String> create(SscsCaseDataWrapper responseWrapper, SubscriptionType subscriptionType) {
        setSendSmsSubscriptionConfirmation(shouldSendSmsSubscriptionConfirmation(responseWrapper.getNewSscsCaseData(),
            responseWrapper.getOldSscsCaseData(), subscriptionType));
        NotificationEventType eventType = getNotificationEventTypeNotification(responseWrapper, subscriptionType);

        responseWrapper.setNotificationEventType(eventType);

        return super.create(responseWrapper, subscriptionType);
    }

    public Boolean shouldSendSmsSubscriptionConfirmation(SscsCaseData newSscsCaseData, SscsCaseData oldSscsCaseData,
                                                         SubscriptionType subscriptionType) {
        return getSubscriptionByType(oldSscsCaseData,subscriptionType) != null
                && !getSubscriptionByType(oldSscsCaseData,subscriptionType).isSmsSubscribed()
                && getSubscriptionByType(newSscsCaseData,subscriptionType) != null
                && getSubscriptionByType(newSscsCaseData,subscriptionType).isSmsSubscribed();
    }

    private Subscription getSubscriptionByType(SscsCaseData sscsCaseData, SubscriptionType subscriptionType) {
        Subscription subscription = sscsCaseData.getSubscriptions().getAppellantSubscription();
        if (SubscriptionType.APPELLANT.equals(subscriptionType)) {
            subscription = sscsCaseData.getSubscriptions().getAppellantSubscription();
        } else if (SubscriptionType.APPOINTEE.equals(subscriptionType)) {
            subscription = sscsCaseData.getSubscriptions().getAppointeeSubscription();
        } else if (SubscriptionType.REPRESENTATIVE.equals(subscriptionType)) {
            subscription = sscsCaseData.getSubscriptions().getRepresentativeSubscription();
        }
        return subscription;
    }

    public NotificationEventType getNotificationEventTypeNotification(SscsCaseDataWrapper responseWrapper, SubscriptionType subscriptionType) {
        SscsCaseData newSscsCaseData = responseWrapper.getNewSscsCaseData();
        SscsCaseData oldSscsCaseData = responseWrapper.getOldSscsCaseData();
        if (doNotSendEmailUpdatedNotificationWhenEmailNotChanged(newSscsCaseData, oldSscsCaseData, subscriptionType)) {
            return DO_NOT_SEND;
        } else if (!isPaperCase(newSscsCaseData.getAppeal().getHearingType())
            && shouldSetMostRecentNotificationEventTypeNotification(newSscsCaseData, oldSscsCaseData, subscriptionType)) {
            return getNotificationByCcdEvent(newSscsCaseData.getEvents().get(0).getValue().getEventType());
        } else {
            return responseWrapper.getNotificationEventType();
        }
    }

    protected Boolean isPaperCase(String hearingType) {
        return AppealHearingType.PAPER.name().equalsIgnoreCase(hearingType);
    }

    private Boolean shouldSetMostRecentNotificationEventTypeNotification(SscsCaseData newSscsCaseData, SscsCaseData oldSscsCaseData,
                                                                         SubscriptionType subscriptionType) {
        return hasCaseJustSubscribed(getSubscriptionByType(oldSscsCaseData, subscriptionType), getSubscriptionByType(newSscsCaseData, subscriptionType))
            && newSscsCaseData.getEvents() != null
            && !newSscsCaseData.getEvents().isEmpty()
            && newSscsCaseData.getEvents().get(0).getValue().getEventType() != null;
    }

    private Boolean hasCaseJustSubscribed(Subscription oldSubscription, Subscription newSubscription) {
        return oldSubscription != null && newSubscription != null
            && (!oldSubscription.isEmailSubscribed() && newSubscription.isEmailSubscribed()
                || (!oldSubscription.isSmsSubscribed() && newSubscription.isSmsSubscribed()));
    }

    public Boolean doNotSendEmailUpdatedNotificationWhenEmailNotChanged(SscsCaseData newSscsCaseData,
                                                                        SscsCaseData oldSscsCaseData,
                                                                        SubscriptionType subscriptionType) {
        return getSubscriptionByType(oldSscsCaseData, subscriptionType) != null
            && getSubscriptionByType(oldSscsCaseData, subscriptionType).isEmailSubscribed()
            && getSubscriptionByType(newSscsCaseData, subscriptionType) != null
            && getSubscriptionByType(newSscsCaseData, subscriptionType).isEmailSubscribed()
            && getSubscriptionByType(oldSscsCaseData, subscriptionType).getEmail().equals(getSubscriptionByType(newSscsCaseData, subscriptionType).getEmail());
    }
}
