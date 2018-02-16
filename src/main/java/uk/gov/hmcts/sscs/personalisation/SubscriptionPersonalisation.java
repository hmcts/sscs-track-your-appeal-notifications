package uk.gov.hmcts.sscs.personalisation;

import static uk.gov.hmcts.sscs.domain.notify.NotificationType.SUBSCRIPTION_CREATED;

import java.util.Map;
import uk.gov.hmcts.sscs.config.NotificationConfig;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;

public class SubscriptionPersonalisation extends Personalisation {

    public SubscriptionPersonalisation(NotificationConfig config) {
        super(config);
    }

    @Override
    public Map<String, String> create(CcdResponseWrapper responseWrapper) {
        checkSubscriptionCreated(responseWrapper.getNewCcdResponse(), responseWrapper.getOldCcdResponse());

        return super.create(responseWrapper);
    }

    public CcdResponse checkSubscriptionCreated(CcdResponse newCcdResponse, CcdResponse oldCcdResponse) {
        if (oldCcdResponse.getAppellantSubscription() != null
                && !oldCcdResponse.getAppellantSubscription().isSubscribeSms()
                && newCcdResponse.getAppellantSubscription() != null
                && newCcdResponse.getAppellantSubscription().isSubscribeSms()) {

            newCcdResponse.setNotificationType(SUBSCRIPTION_CREATED);
        }
        return newCcdResponse;
    }
}
