package uk.gov.hmcts.sscs.personalisation;

import java.util.Map;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.domain.Subscription;
import uk.gov.hmcts.sscs.domain.notify.EventType;

@Component
public class SubscriptionPersonalisation extends Personalisation {

    @Override
    public Map<String, String> create(CcdResponseWrapper responseWrapper) {
        setSendSmsSubscriptionConfirmation(shouldSendSmsSubscriptionConfirmation(responseWrapper.getNewCcdResponse(), responseWrapper.getOldCcdResponse()));
        responseWrapper.getNewCcdResponse().setNotificationType(setEventTypeNotification(responseWrapper.getNewCcdResponse(), responseWrapper.getOldCcdResponse()));
        return super.create(responseWrapper);
    }

    public Boolean shouldSendSmsSubscriptionConfirmation(CcdResponse newCcdResponse, CcdResponse oldCcdResponse) {
        return (oldCcdResponse.getSubscriptions().getAppellantSubscription() != null
                && !oldCcdResponse.getSubscriptions().getAppellantSubscription().isSmsSubscribed()
                && newCcdResponse.getSubscriptions().getAppellantSubscription() != null
                && newCcdResponse.getSubscriptions().getAppellantSubscription().isSmsSubscribed());
    }

    public EventType setEventTypeNotification(CcdResponse newCcdResponse, CcdResponse oldCcdResponse) {
        if (doNotSendEmailUpdatedNotificationWhenEmailNotChanged(newCcdResponse, oldCcdResponse)) {
            return EventType.DO_NOT_SEND;
        } else if (shouldSetMostRecentEventTypeNotification(newCcdResponse, oldCcdResponse)) {
            return newCcdResponse.getEvents().get(0).getValue().getEventType();
        } else {
            return newCcdResponse.getNotificationType();
        }
    }

    private Boolean shouldSetMostRecentEventTypeNotification(CcdResponse newCcdResponse, CcdResponse oldCcdResponse) {
        return (hasCaseJustSubscribed(oldCcdResponse.getSubscriptions().getAppellantSubscription(), newCcdResponse.getSubscriptions().getAppellantSubscription())
                && newCcdResponse.getEvents() != null
                && !newCcdResponse.getEvents().isEmpty()
                && newCcdResponse.getEvents().get(0).getValue().getEventType() != null);
    }

    private Boolean hasCaseJustSubscribed(Subscription oldSubscription, Subscription newSubscription) {
        return oldSubscription != null && newSubscription != null
             && ((!oldSubscription.isEmailSubscribed() && newSubscription.isEmailSubscribed())
             || (!oldSubscription.isSmsSubscribed() && newSubscription.isSmsSubscribed()));
    }

    public Boolean doNotSendEmailUpdatedNotificationWhenEmailNotChanged(CcdResponse newCcdResponse, CcdResponse oldCcdResponse) {
        return (oldCcdResponse.getSubscriptions().getAppellantSubscription() != null
                && oldCcdResponse.getSubscriptions().getAppellantSubscription().isEmailSubscribed()
                && newCcdResponse.getSubscriptions().getAppellantSubscription() != null
                && newCcdResponse.getSubscriptions().getAppellantSubscription().isEmailSubscribed()
                && oldCcdResponse.getSubscriptions().getAppellantSubscription().getEmail().equals(newCcdResponse.getSubscriptions().getAppellantSubscription().getEmail()));
    }
}
