package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPOINTEE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.HEARING_BOOKED_NOTIFICATION;

import java.util.Arrays;
import java.util.List;

import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.Notification;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;

public class NotificationUtils {
    private static final List<NotificationEventType> MANDATORY_LETTERS = Arrays.asList(HEARING_BOOKED_NOTIFICATION);

    private NotificationUtils() {
        // empty
    }

    /* Sometimes the data for the appointee comes in with null populated objects */
    public static boolean hasAppointee(SscsCaseDataWrapper wrapper) {
        return (wrapper.getNewSscsCaseData().getAppeal() != null
            && wrapper.getNewSscsCaseData().getAppeal().getAppellant() != null
            && wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee() != null
            && wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee().getName() != null
            && wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee().getName().getFirstName() != null);
    }

    /* Sometimes the data for the appointee comes in with null populated objects */
    public static boolean hasRepresentative(SscsCaseDataWrapper wrapper) {
        return (wrapper.getNewSscsCaseData().getAppeal() != null
            && wrapper.getNewSscsCaseData().getAppeal().getRep() != null
            && wrapper.getNewSscsCaseData().getAppeal().getRep().getHasRepresentative() != null
            && wrapper.getNewSscsCaseData().getAppeal().getRep().getHasRepresentative().equalsIgnoreCase("yes"));
    }

    public static boolean hasLetterTemplate(Notification notification) {
        return notification.getLetterTemplate() != null;
    }

    public static boolean hasNoSubscriptions(Subscription subscription) {
        return !subscription.isSmsSubscribed() && !subscription.isEmailSubscribed();
    }

    public static final boolean isAppointeeOrAppellantSubscription(SubscriptionType subscriptionType) {
        return APPELLANT.equals(subscriptionType)
            || APPOINTEE.equals(subscriptionType);
    }

    public static boolean isMandatoryLetter(NotificationEventType eventType) {
        return MANDATORY_LETTERS.contains(eventType);
    }

}
