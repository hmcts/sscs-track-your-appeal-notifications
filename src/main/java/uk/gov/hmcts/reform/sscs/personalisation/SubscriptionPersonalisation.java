package uk.gov.hmcts.reform.sscs.personalisation;

import java.util.Map;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;

@Component
public class SubscriptionPersonalisation extends Personalisation<CcdNotificationWrapper> {

    @Override
    protected Map<String, String> create(SscsCaseDataWrapper responseWrapper) {
        setSendSmsSubscriptionConfirmation(shouldSendSmsSubscriptionConfirmation(responseWrapper.getNewSscsCaseData(), responseWrapper.getOldSscsCaseData()));
        responseWrapper.getNewSscsCaseData().setNotificationType(setEventTypeNotification(responseWrapper.getNewSscsCaseData(), responseWrapper.getOldSscsCaseData()));
        return super.create(responseWrapper);
    }

    public Boolean shouldSendSmsSubscriptionConfirmation(SscsCaseData newSscsCaseData, SscsCaseData oldSscsCaseData) {
        return (oldSscsCaseData.getSubscriptions().getAppellantSubscription() != null
                && !oldSscsCaseData.getSubscriptions().getAppellantSubscription().isSmsSubscribed()
                && newSscsCaseData.getSubscriptions().getAppellantSubscription() != null
                && newSscsCaseData.getSubscriptions().getAppellantSubscription().isSmsSubscribed());
    }

    public EventType setEventTypeNotification(SscsCaseData newSscsCaseData, SscsCaseData oldSscsCaseData) {
        if (doNotSendEmailUpdatedNotificationWhenEmailNotChanged(newSscsCaseData, oldSscsCaseData)) {
            return EventType.DO_NOT_SEND;
        } else if (shouldSetMostRecentEventTypeNotification(newSscsCaseData, oldSscsCaseData)) {
            return newSscsCaseData.getEvents().get(0).getValue().getEventType();
        } else {
            return newSscsCaseData.getNotificationType();
        }
    }

    private Boolean shouldSetMostRecentEventTypeNotification(SscsCaseData newSscsCaseData, SscsCaseData oldSscsCaseData) {
        return (hasCaseJustSubscribed(oldSscsCaseData.getSubscriptions().getAppellantSubscription(), newSscsCaseData.getSubscriptions().getAppellantSubscription())
                && newSscsCaseData.getEvents() != null
                && !newSscsCaseData.getEvents().isEmpty()
                && newSscsCaseData.getEvents().get(0).getValue().getEventType() != null);
    }

    private Boolean hasCaseJustSubscribed(Subscription oldSubscription, Subscription newSubscription) {
        return oldSubscription != null && newSubscription != null
             && ((!oldSubscription.isEmailSubscribed() && newSubscription.isEmailSubscribed())
             || (!oldSubscription.isSmsSubscribed() && newSubscription.isSmsSubscribed()));
    }

    public Boolean doNotSendEmailUpdatedNotificationWhenEmailNotChanged(SscsCaseData newSscsCaseData, SscsCaseData oldSscsCaseData) {
        return (oldSscsCaseData.getSubscriptions().getAppellantSubscription() != null
                && oldSscsCaseData.getSubscriptions().getAppellantSubscription().isEmailSubscribed()
                && newSscsCaseData.getSubscriptions().getAppellantSubscription() != null
                && newSscsCaseData.getSubscriptions().getAppellantSubscription().isEmailSubscribed()
                && oldSscsCaseData.getSubscriptions().getAppellantSubscription().getEmail().equals(newSscsCaseData.getSubscriptions().getAppellantSubscription().getEmail()));
    }
}
