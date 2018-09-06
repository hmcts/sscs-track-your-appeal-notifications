package uk.gov.hmcts.reform.sscs.factory;

import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.QUESTION_ROUND_ISSUED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SUBSCRIPTION_UPDATED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SYA_APPEAL_CREATED_NOTIFICATION;

import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.personalisation.CohPersonalisation;
import uk.gov.hmcts.reform.sscs.personalisation.Personalisation;
import uk.gov.hmcts.reform.sscs.personalisation.SubscriptionPersonalisation;
import uk.gov.hmcts.reform.sscs.personalisation.SyaAppealCreatedPersonalisation;

@Component
public class PersonalisationFactory implements Function<NotificationEventType, Personalisation> {

    @Autowired
    private SyaAppealCreatedPersonalisation syaAppealCreatedPersonalisation;

    @Autowired
    private SubscriptionPersonalisation subscriptionPersonalisation;

    @Autowired
    private Personalisation personalisation;

    @Autowired
    private CohPersonalisation cohPersonalisation;

    @Override
    public Personalisation apply(NotificationEventType notificationType) {
        if (notificationType != null) {
            if (notificationType.equals(SYA_APPEAL_CREATED_NOTIFICATION)) {
                return syaAppealCreatedPersonalisation;
            } else if (notificationType.equals(SUBSCRIPTION_UPDATED_NOTIFICATION)) {
                return subscriptionPersonalisation;
            } else if (notificationType.equals(QUESTION_ROUND_ISSUED_NOTIFICATION)) {
                return cohPersonalisation;
            } else {
                return personalisation;
            }
        }
        return null;
    }
}
