package uk.gov.hmcts.sscs.personalisation;

import java.util.Map;
import uk.gov.hmcts.sscs.config.NotificationConfig;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.domain.notify.EventType;
import uk.gov.hmcts.sscs.service.MessageAuthenticationServiceImpl;

public class SubscriptionPersonalisation extends Personalisation {

    public SubscriptionPersonalisation(NotificationConfig config, MessageAuthenticationServiceImpl macService) {
        super(config, macService);
    }

    @Override
    public Map<String, String> create(CcdResponseWrapper responseWrapper) {
        setSendSmsSubscriptionConfirmation(shouldSendSmsSubscriptionConfirmation(responseWrapper.getNewCcdResponse(), responseWrapper.getOldCcdResponse()));
        setMostRecentEventTypeNotification(responseWrapper.getNewCcdResponse(), responseWrapper.getOldCcdResponse());
        doNotSendEmailUpdatedNotificationWhenEmailNotChanged(responseWrapper.getNewCcdResponse(), responseWrapper.getOldCcdResponse());

        return super.create(responseWrapper);
    }

    public Boolean shouldSendSmsSubscriptionConfirmation(CcdResponse newCcdResponse, CcdResponse oldCcdResponse) {
        return (oldCcdResponse.getAppellantSubscription() != null
                && !oldCcdResponse.getAppellantSubscription().isSubscribeSms()
                && newCcdResponse.getAppellantSubscription() != null
                && newCcdResponse.getAppellantSubscription().isSubscribeSms());
    }

    public CcdResponse setMostRecentEventTypeNotification(CcdResponse newCcdResponse, CcdResponse oldCcdResponse) {
        if (oldCcdResponse.getAppellantSubscription() != null
                && !oldCcdResponse.getAppellantSubscription().isSubscribeEmail()
                && newCcdResponse.getAppellantSubscription() != null
                && newCcdResponse.getAppellantSubscription().isSubscribeEmail()
                && newCcdResponse.getEvents() != null
                && !newCcdResponse.getEvents().isEmpty()) {

            newCcdResponse.setNotificationType(newCcdResponse.getEvents().get(0).getEventType());
        }
        return newCcdResponse;
    }

    public CcdResponse doNotSendEmailUpdatedNotificationWhenEmailNotChanged(CcdResponse newCcdResponse, CcdResponse oldCcdResponse) {
        if (oldCcdResponse.getAppellantSubscription() != null
                && oldCcdResponse.getAppellantSubscription().isSubscribeEmail()
                && newCcdResponse.getAppellantSubscription() != null
                && newCcdResponse.getAppellantSubscription().isSubscribeEmail()
                && oldCcdResponse.getAppellantSubscription().getEmail().equals(newCcdResponse.getAppellantSubscription().getEmail())) {
            newCcdResponse.setNotificationType(EventType.DO_NOT_SEND);
        }
        return newCcdResponse;
    }
}
