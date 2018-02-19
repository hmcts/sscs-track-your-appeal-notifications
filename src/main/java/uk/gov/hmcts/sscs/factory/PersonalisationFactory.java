package uk.gov.hmcts.sscs.factory;

import static uk.gov.hmcts.sscs.domain.notify.EventType.SUBSCRIPTION_UPDATED;

import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.sscs.config.NotificationConfig;
import uk.gov.hmcts.sscs.domain.notify.EventType;
import uk.gov.hmcts.sscs.personalisation.Personalisation;
import uk.gov.hmcts.sscs.personalisation.SubscriptionPersonalisation;

@Component
public class PersonalisationFactory implements Function<EventType, Personalisation> {

    private final NotificationConfig config;

    @Autowired
    public PersonalisationFactory(NotificationConfig config) {
        this.config = config;
    }

    @Override
    public Personalisation apply(EventType notificationType) {
        if (notificationType != null) {
            if (notificationType.equals(SUBSCRIPTION_UPDATED)) {
                return new SubscriptionPersonalisation(config);
            } else {
                return new Personalisation(config);
            }
        }
        return null;
    }
}
