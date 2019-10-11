package uk.gov.hmcts.reform.sscs.factory;

import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.personalisation.CohPersonalisation;
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
    private Personalisation personalisation;

    @Autowired
    private CohPersonalisation cohPersonalisation;

    @Override
    public Personalisation apply(NotificationEventType notificationType) {
        Personalisation selectedPersonalisation = null;
        if (notificationType != null) {
            if (SYA_APPEAL_CREATED_NOTIFICATION.equals(notificationType)
                    || RESEND_APPEAL_CREATED_NOTIFICATION.equals(notificationType)
                    || APPEAL_RECEIVED_NOTIFICATION.equals(notificationType)
                    || CASE_UPDATED.equals(notificationType)
                    || VALID_APPEAL_CREATED.equals(notificationType)) {
                selectedPersonalisation = syaAppealCreatedAndReceivedPersonalisation;
            } else if (APPEAL_LAPSED_NOTIFICATION.equals(notificationType)
                || HMCTS_APPEAL_LAPSED_NOTIFICATION.equals(notificationType)
                || DWP_APPEAL_LAPSED_NOTIFICATION.equals(notificationType)
                || APPEAL_WITHDRAWN_NOTIFICATION.equals(notificationType)
                || EVIDENCE_RECEIVED_NOTIFICATION.equals(notificationType)
                || EVIDENCE_REMINDER_NOTIFICATION.equals(notificationType)
                || HEARING_REMINDER_NOTIFICATION.equals(notificationType)
                || APPEAL_DORMANT_NOTIFICATION.equals(notificationType)
                || ADJOURNED_NOTIFICATION.equals(notificationType)
                || POSTPONEMENT_NOTIFICATION.equals(notificationType)
                || DWP_RESPONSE_RECEIVED_NOTIFICATION.equals(notificationType)
                || DIRECTION_ISSUED.equals(notificationType)
                || DECISION_ISSUED.equals(notificationType)
                || HEARING_BOOKED_NOTIFICATION.equals(notificationType)) {
                selectedPersonalisation = withRepresentativePersonalisation;
            } else if (SUBSCRIPTION_UPDATED_NOTIFICATION.equals(notificationType)) {
                selectedPersonalisation = subscriptionPersonalisation;
            } else if (QUESTION_ROUND_ISSUED_NOTIFICATION.equals(notificationType)) {
                selectedPersonalisation = cohPersonalisation;
            } else {
                selectedPersonalisation = this.personalisation;
            }
        }
        return selectedPersonalisation;
    }
}
