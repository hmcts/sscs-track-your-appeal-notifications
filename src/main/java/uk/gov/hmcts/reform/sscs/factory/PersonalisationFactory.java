package uk.gov.hmcts.reform.sscs.factory;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.reform.sscs.config.NotificationEventTypeLists.EVENTS_FOR_REPRESENTATIVE_PERSONALISATION;
import static uk.gov.hmcts.reform.sscs.config.NotificationEventTypeLists.EVENTS_FOR_SYA_PERSONALISATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ISSUE_GENERIC_LETTER;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SUBSCRIPTION_UPDATED;

import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.personalisation.GenericLetterPersonalisation;
import uk.gov.hmcts.reform.sscs.personalisation.Personalisation;
import uk.gov.hmcts.reform.sscs.personalisation.SubscriptionPersonalisation;
import uk.gov.hmcts.reform.sscs.personalisation.SyaAppealCreatedAndReceivedPersonalisation;
import uk.gov.hmcts.reform.sscs.personalisation.WithRepresentativePersonalisation;

@Component
public class PersonalisationFactory implements Function<NotificationEventType, Personalisation> {

    @Autowired
    private SyaAppealCreatedAndReceivedPersonalisation syaAppealCreatedAndReceivedPersonalisation;

    @Autowired
    private WithRepresentativePersonalisation withRepresentativePersonalisation;

    @Autowired
    private SubscriptionPersonalisation subscriptionPersonalisation;

    @Autowired
    private GenericLetterPersonalisation genericLetterPersonalisation;

    @Autowired
    private Personalisation personalisation;

    @Override
    public Personalisation apply(NotificationEventType notificationType) {
        if (isNull(notificationType)) {
            return null;
        }

        if (EVENTS_FOR_SYA_PERSONALISATION.contains(notificationType)) {
            return syaAppealCreatedAndReceivedPersonalisation;
        } else if (EVENTS_FOR_REPRESENTATIVE_PERSONALISATION.contains(notificationType)) {
            return withRepresentativePersonalisation;
        } else if (SUBSCRIPTION_UPDATED.equals(notificationType)) {
            return subscriptionPersonalisation;
        } else if (ISSUE_GENERIC_LETTER.equals(notificationType)) {
            return genericLetterPersonalisation;
        } else {
            return this.personalisation;
        }
    }
}
