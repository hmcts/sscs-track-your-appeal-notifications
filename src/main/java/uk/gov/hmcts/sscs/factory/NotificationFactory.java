package uk.gov.hmcts.sscs.factory;

import static com.google.common.collect.Maps.newHashMap;
import static uk.gov.hmcts.sscs.domain.Benefit.getBenefitByCode;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.sscs.domain.Benefit;
import uk.gov.hmcts.sscs.domain.CcdResponse;
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

    public <E extends NotificationWrapper> Notification create(E notificationWrapper) {
        Personalisation<E> personalisation = getPersonalisation(notificationWrapper);
        if (personalisation == null) {
            return null;
        }

        Map<String, String> placeholders = personalisation.create(notificationWrapper);
        if (null == placeholders) {
            return null;
        }

        Benefit benefit = getBenefitByCode(notificationWrapper.getCcdResponseWrapper().getNewCcdResponse().getAppeal().getBenefitType().getCode());
        Template template = personalisation.getTemplate(notificationWrapper, benefit);

        CcdResponse ccdResponse = notificationWrapper.getCcdResponseWrapper().getNewCcdResponse();
        Destination destination = ccdResponse.getSubscriptions().getAppellantSubscription().getDestination();
        Reference reference = new Reference(ccdResponse.getCaseReference());
        String appealNumber = ccdResponse.getSubscriptions().getAppellantSubscription().getTya();

        return new Notification(template, destination, placeholders, reference, appealNumber);
    }

    private <E extends NotificationWrapper> Personalisation<E> getPersonalisation(E notificationWrapper) {
        return map.computeIfAbsent(notificationWrapper.getNotificationType(), personalisationFactory);
    }
}
