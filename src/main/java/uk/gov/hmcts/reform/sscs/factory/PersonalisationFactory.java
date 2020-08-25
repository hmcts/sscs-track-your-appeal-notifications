package uk.gov.hmcts.reform.sscs.factory;

import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ADJOURNED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ADMIN_APPEAL_WITHDRAWN;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_DORMANT_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_LAPSED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_RECEIVED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_WITHDRAWN_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.CASE_UPDATED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DECISION_ISSUED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DECISION_ISSUED_WELSH;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DIRECTION_ISSUED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DIRECTION_ISSUED_WELSH;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DWP_APPEAL_LAPSED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DWP_RESPONSE_RECEIVED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DWP_UPLOAD_RESPONSE_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.EVIDENCE_RECEIVED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.EVIDENCE_REMINDER_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.HEARING_BOOKED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.HEARING_REMINDER_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.HMCTS_APPEAL_LAPSED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ISSUE_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ISSUE_FINAL_DECISION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.POSTPONEMENT_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.REQUEST_INFO_INCOMPLETE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.RESEND_APPEAL_CREATED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.STRUCK_OUT;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SUBSCRIPTION_UPDATED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SYA_APPEAL_CREATED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.VALID_APPEAL_CREATED;

import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
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
                    || ADMIN_APPEAL_WITHDRAWN.equals(notificationType)
                    || EVIDENCE_RECEIVED_NOTIFICATION.equals(notificationType)
                    || EVIDENCE_REMINDER_NOTIFICATION.equals(notificationType)
                    || HEARING_REMINDER_NOTIFICATION.equals(notificationType)
                    || APPEAL_DORMANT_NOTIFICATION.equals(notificationType)
                    || ADJOURNED_NOTIFICATION.equals(notificationType)
                    || POSTPONEMENT_NOTIFICATION.equals(notificationType)
                    || DWP_RESPONSE_RECEIVED_NOTIFICATION.equals(notificationType)
                    || DWP_UPLOAD_RESPONSE_NOTIFICATION.equals(notificationType)
                    || DIRECTION_ISSUED.equals(notificationType)
                    || DECISION_ISSUED.equals(notificationType)
                    || DIRECTION_ISSUED_WELSH.equals(notificationType)
                    || DECISION_ISSUED_WELSH.equals(notificationType)
                    || REQUEST_INFO_INCOMPLETE.equals(notificationType)
                    || ISSUE_FINAL_DECISION.equals(notificationType)
                    || ISSUE_ADJOURNMENT_NOTICE.equals(notificationType)
                    || STRUCK_OUT.equals(notificationType)
                    || HEARING_BOOKED_NOTIFICATION.equals(notificationType)) {
                selectedPersonalisation = withRepresentativePersonalisation;
            } else if (SUBSCRIPTION_UPDATED_NOTIFICATION.equals(notificationType)) {
                selectedPersonalisation = subscriptionPersonalisation;
            } else {
                selectedPersonalisation = this.personalisation;
            }
        }
        return selectedPersonalisation;
    }
}
