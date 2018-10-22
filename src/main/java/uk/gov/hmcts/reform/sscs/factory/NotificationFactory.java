package uk.gov.hmcts.reform.sscs.factory;

import static com.google.common.collect.Maps.newHashMap;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.getBenefitByCode;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.domain.notify.*;
import uk.gov.hmcts.reform.sscs.personalisation.Personalisation;

@Component
public class NotificationFactory {

    private final PersonalisationFactory personalisationFactory;

    private final Map<NotificationEventType, Personalisation> map = newHashMap();

    @Autowired
    NotificationFactory(PersonalisationFactory personalisationFactory) {
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

        Benefit benefit = getBenefitByCode(notificationWrapper.getSscsCaseDataWrapper().getNewSscsCaseData().getAppeal().getBenefitType().getCode());
        Template template = personalisation.getTemplate(notificationWrapper, benefit);

        SscsCaseData ccdResponse = notificationWrapper.getSscsCaseDataWrapper().getNewSscsCaseData();

        Destination destination = getDestination(ccdResponse.getSubscriptions().getAppellantSubscription());
        Reference reference = new Reference(ccdResponse.getCaseReference());
        String appealNumber = ccdResponse.getSubscriptions().getAppellantSubscription().getTya();

        return new Notification(template, destination, placeholders, reference, appealNumber);
    }

    private <E extends NotificationWrapper> Personalisation<E> getPersonalisation(E notificationWrapper) {
        return map.computeIfAbsent(notificationWrapper.getNotificationType(), personalisationFactory);
    }

    private Destination getDestination(Subscription subscription) {
        return Destination.builder().email(subscription.getEmail()).sms(subscription.getMobile()).build();
    }
}
