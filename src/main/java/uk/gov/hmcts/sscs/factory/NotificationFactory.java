package uk.gov.hmcts.sscs.factory;

import static com.google.common.collect.Maps.newHashMap;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.notify.*;
import uk.gov.hmcts.sscs.personalisation.Personalisation;

@Component
public class NotificationFactory {

    private final PersonalisationFactory personalisationFactory;

    private final Map<NotificationType, Personalisation> map = newHashMap();

    @Autowired
    public NotificationFactory(PersonalisationFactory personalisationFactory) {
        this.personalisationFactory = personalisationFactory;
    }

    public Notification create(CcdResponse ccdResponse)  {
        Personalisation personalisation = map.computeIfAbsent(ccdResponse.getNotificationType(), personalisationFactory);
        if (personalisation == null) {
            return null;
        }

        Map<String, String> placeholders = personalisation.create(ccdResponse);
        if (null == placeholders) {
            return null;
        }

        //TODO: If SubscriptionUpdated Notification then check what type of notification to send
        //subscription.generateSubscriptionNotificationType();

        Template template = personalisation.getTemplate(ccdResponse.getNotificationType());
        Destination destination = ccdResponse.getAppellantSubscription().getDestination();
        Reference reference = new Reference();
        String appealNumber = ccdResponse.getAppellantSubscription().getAppealNumber();
        return new Notification(template, destination, placeholders, reference, appealNumber);
    }
}
