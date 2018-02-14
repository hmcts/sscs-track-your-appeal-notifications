package uk.gov.hmcts.sscs.personalisation;

import static java.time.temporal.ChronoUnit.DAYS;
import static uk.gov.hmcts.sscs.config.AppConstants.*;
import static uk.gov.hmcts.sscs.domain.notify.NotificationType.SUBSCRIPTION_CREATED;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import uk.gov.hmcts.sscs.config.NotificationConfig;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.notify.NotificationType;
import uk.gov.hmcts.sscs.domain.notify.Template;

public class SubscriptionPersonalisation extends Personalisation {

    public SubscriptionPersonalisation(NotificationConfig config) {
        this.config = config;
    }

    @Override
    public Map<String, String> create(CcdResponse newCcdResponse) {
        super.create(newCcdResponse);
        //TODO: Access cache to retrieve oldCcdResponse

        return checkSubscriptionCreated(oldCcdResponse, newCcdResponse);
    }

    private CcdResponse checkSubscriptionCreated((CcdResponse oldCcdResponse, CcdResponse newCcdResponse) {
        if (oldCcdResponse.getAppellantSubscription() != null
                && !oldCcdResponse.getAppellantSubscription().getSubscribeSms()
                && newCcdResponse.getAppellantSubscription() != null
                && newCcdResponse.getAppellantSubscription().getSubscribeSms()) {

            newCcdResponse.setNotificationType(SUBSCRIPTION_CREATED);
        }
        return newCcdResponse;

    }
}
