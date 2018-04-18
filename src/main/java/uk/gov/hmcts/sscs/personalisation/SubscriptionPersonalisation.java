package uk.gov.hmcts.sscs.personalisation;

import java.util.Map;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
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
        return (oldCcdResponse.getAppellantSubscription() != null
                && !oldCcdResponse.getAppellantSubscription().isSubscribeSms()
                && newCcdResponse.getAppellantSubscription() != null
                && newCcdResponse.getAppellantSubscription().isSubscribeSms());
    }

    public EventType setEventTypeNotification(CcdResponse newCcdResponse, CcdResponse oldCcdResponse) {
        if (doNotSendEmailUpdatedNotificationWhenEmailNotChanged(newCcdResponse, oldCcdResponse)) {
            return EventType.DO_NOT_SEND;
        } else if (shouldSetMostRecentEventTypeNotification(newCcdResponse, oldCcdResponse)) {
            return newCcdResponse.getEvents().get(0).getEventType();
        } else {
            return newCcdResponse.getNotificationType();
        }
    }

    private Boolean shouldSetMostRecentEventTypeNotification(CcdResponse newCcdResponse, CcdResponse oldCcdResponse) {
        return (oldCcdResponse.getAppellantSubscription() != null
                && !oldCcdResponse.getAppellantSubscription().isSubscribeEmail()
                && newCcdResponse.getAppellantSubscription() != null
                && newCcdResponse.getAppellantSubscription().isSubscribeEmail()
                && newCcdResponse.getEvents() != null
                && !newCcdResponse.getEvents().isEmpty()
                && newCcdResponse.getEvents().get(0).getEventType() != null);
    }

    public Boolean doNotSendEmailUpdatedNotificationWhenEmailNotChanged(CcdResponse newCcdResponse, CcdResponse oldCcdResponse) {
        return (oldCcdResponse.getAppellantSubscription() != null
                && oldCcdResponse.getAppellantSubscription().isSubscribeEmail()
                && newCcdResponse.getAppellantSubscription() != null
                && newCcdResponse.getAppellantSubscription().isSubscribeEmail()
                && oldCcdResponse.getAppellantSubscription().getEmail().equals(newCcdResponse.getAppellantSubscription().getEmail()));
    }
}
