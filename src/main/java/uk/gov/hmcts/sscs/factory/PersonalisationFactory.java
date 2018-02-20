package uk.gov.hmcts.sscs.factory;

import static uk.gov.hmcts.sscs.domain.notify.EventType.SUBSCRIPTION_UPDATED;

import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.sscs.config.NotificationConfig;
import uk.gov.hmcts.sscs.domain.notify.EventType;
import uk.gov.hmcts.sscs.personalisation.Personalisation;
import uk.gov.hmcts.sscs.personalisation.SubscriptionPersonalisation;
import uk.gov.hmcts.sscs.service.MessageAuthenticationServiceImpl;

@Component
public class PersonalisationFactory implements Function<EventType, Personalisation> {

    private final NotificationConfig config;
    private final MessageAuthenticationServiceImpl macService;

    @Autowired
    public PersonalisationFactory(NotificationConfig config, MessageAuthenticationServiceImpl macService) {
        this.config = config;
        this.macService = macService;
    }

    @Override
    public Personalisation apply(EventType notificationType) {
        if (notificationType != null) {
            if (notificationType.equals(SUBSCRIPTION_UPDATED)) {
                return new SubscriptionPersonalisation(config, macService);
            } else {
                return new Personalisation(config, macService);
            }
        }
        return null;
    }
}
