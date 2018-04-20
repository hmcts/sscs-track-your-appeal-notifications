package uk.gov.hmcts.sscs.factory;

import static uk.gov.hmcts.sscs.domain.notify.EventType.SUBSCRIPTION_UPDATED;

import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.sscs.domain.notify.EventType;
import uk.gov.hmcts.sscs.personalisation.Personalisation;
import uk.gov.hmcts.sscs.personalisation.SubscriptionPersonalisation;

@Component
public class PersonalisationFactory implements Function<EventType, Personalisation> {

    @Autowired
    private SubscriptionPersonalisation subscriptionPersonalisation;

    @Autowired
    private Personalisation personalisation;

    @Override
    public Personalisation apply(EventType notificationType) {
        if (notificationType != null) {
            if (notificationType.equals(SUBSCRIPTION_UPDATED)) {
                return subscriptionPersonalisation;
            } else {
                return personalisation;
            }
        }
        return null;
    }
}
