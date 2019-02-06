package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.HEARING_BOOKED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.STRUCK_OUT;
import static uk.gov.hmcts.reform.sscs.service.NotificationValidService.isFallbackLetterRequiredForSubscriptionType;

import java.util.Arrays;
import java.util.List;

import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;

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
            && wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee().getName().getFirstName() != null
            && wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee().getName().getLastName() != null);
    }

    public static boolean hasRepresentative(SscsCaseDataWrapper wrapper) {
        return (wrapper.getNewSscsCaseData().getAppeal() != null
            && wrapper.getNewSscsCaseData().getAppeal().getRep() != null
            && wrapper.getNewSscsCaseData().getAppeal().getRep().getHasRepresentative() != null
            && wrapper.getNewSscsCaseData().getAppeal().getRep().getHasRepresentative().equalsIgnoreCase("yes"));
    }

    public static boolean hasAppointeeSubscription(SscsCaseDataWrapper wrapper) {
        return null != wrapper.getNewSscsCaseData().getSubscriptions().getAppointeeSubscription();
    }

    public static boolean hasRepresentativeSubscription(SscsCaseDataWrapper wrapper) {
        return null != wrapper.getNewSscsCaseData().getSubscriptions().getRepresentativeSubscription();
    }

    static boolean isMandatoryLetter(NotificationEventType eventType) {
        return STRUCK_OUT.equals(eventType);
    }

    public static boolean isMandatoryLetterEventType(NotificationEventType eventType) {
        return MANDATORY_LETTERS.contains(eventType);
    }

    static boolean isOkToSendNotification(NotificationWrapper wrapper, NotificationEventType notificationType, NotificationValidService notificationValidService) {
        return notificationValidService.isNotificationStillValidToSend(wrapper.getNewSscsCaseData().getHearings(), notificationType)
            && notificationValidService.isHearingTypeValidToSendNotification(wrapper.getNewSscsCaseData(), notificationType);
    }

    static boolean isFallbackLetterRequired(NotificationWrapper wrapper, SubscriptionWithType subscriptionWithType, Subscription subscription, NotificationEventType eventType) {
        return (subscription != null && subscription.doesCaseHaveSubscriptions()
            || (subscription != null && !subscription.doesCaseHaveSubscriptions() && isFallbackLetterRequiredForSubscriptionType(wrapper, subscriptionWithType.getSubscriptionType(), eventType)
            || subscription == null && isFallbackLetterRequiredForSubscriptionType(wrapper, subscriptionWithType.getSubscriptionType(), eventType)));
    }
}
