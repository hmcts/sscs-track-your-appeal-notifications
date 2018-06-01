package uk.gov.hmcts.sscs.factory;

import static com.google.common.collect.Maps.newHashMap;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.domain.notify.*;
import uk.gov.hmcts.sscs.personalisation.Personalisation;

@Component
public class NotificationFactory {

    private final PersonalisationFactory personalisationFactory;

    private final Map<EventType, Personalisation> map = newHashMap();

    @Autowired
    public NotificationFactory(PersonalisationFactory personalisationFactory) {
        this.personalisationFactory = personalisationFactory;
    }

    public Notification create(CcdResponseWrapper responseWrapper) {
        CcdResponse ccdResponse = responseWrapper.getNewCcdResponse();
        Personalisation personalisation = map.computeIfAbsent(ccdResponse.getNotificationType(), personalisationFactory);
        if (personalisation == null) {
            return null;
        }

        Map<String, String> placeholders = personalisation.create(responseWrapper);
        if (null == placeholders) {
            return null;
        }

        Template template = personalisation.getTemplate(ccdResponse.getNotificationType());
        Destination destination = ccdResponse.getSubscriptions().getAppellantSubscription().getDestination();
        Reference reference = new Reference(ccdResponse.getCaseReference());
        String appealNumber = ccdResponse.getSubscriptions().getAppellantSubscription().getTya();
        return new Notification(template, destination, placeholders, reference, appealNumber);
    }
}
