package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPOINTEE;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.HEARING_BOOKED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.STRUCK_OUT;
import static uk.gov.hmcts.reform.sscs.service.NotificationValidService.FALLBACK_LETTER_SUBSCRIPTION_TYPES;

import java.util.Arrays;
import java.util.List;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;

import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.domain.notify.Notification;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;

public class NotificationUtils {
    private static final List<NotificationEventType> MANDATORY_LETTERS = Arrays.asList(STRUCK_OUT, HEARING_BOOKED_NOTIFICATION);

    private NotificationUtils() {
        // empty
    }

    /* Sometimes the data for the appointee comes in with null populated objects */
    public static boolean hasAppointee(SscsCaseDataWrapper wrapper) {
        return wrapper.getNewSscsCaseData().getAppeal() != null
            && wrapper.getNewSscsCaseData().getAppeal().getAppellant() != null
            && hasAppointee(wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee());
    }

    public static boolean hasAppointee(Appointee appointee) {
        return appointee != null && appointee.getName() != null && appointee.getName().getFirstName() != null
                && appointee.getName().getLastName() != null;
    }

    /* Sometimes the data for the appointee comes in with null populated objects */
    public static boolean hasRepresentative(SscsCaseDataWrapper wrapper) {
        return wrapper.getNewSscsCaseData().getAppeal() != null
            && wrapper.getNewSscsCaseData().getAppeal().getRep() != null
            && wrapper.getNewSscsCaseData().getAppeal().getRep().getHasRepresentative() != null
            && wrapper.getNewSscsCaseData().getAppeal().getRep().getHasRepresentative().equalsIgnoreCase("yes");
    }

    public static boolean hasAppointeeSubscription(SscsCaseDataWrapper wrapper) {
        return null != wrapper.getNewSscsCaseData().getSubscriptions().getAppointeeSubscription();
    }

    public static boolean hasRepresentativeSubscription(SscsCaseDataWrapper wrapper) {
        return null != wrapper.getNewSscsCaseData().getSubscriptions().getRepresentativeSubscription();
    }

    public static Subscription getSubscription(SscsCaseData sscsCaseData, SubscriptionType subscriptionType) {
        if (REPRESENTATIVE.equals(subscriptionType)) {
            return sscsCaseData.getSubscriptions().getRepresentativeSubscription();
        } else if (APPELLANT.equals(subscriptionType)) {
            return sscsCaseData.getSubscriptions().getAppellantSubscription();
        } else {
            return sscsCaseData.getSubscriptions().getAppointeeSubscription();
        }
    }

    public static boolean isMandatoryLetterEventType(NotificationEventType eventType) {
        return MANDATORY_LETTERS.contains(eventType);
    }

    static boolean isOkToSendNotification(NotificationWrapper wrapper, NotificationEventType notificationType, Subscription subscription, NotificationValidService notificationValidService) {
        return ((subscription != null && subscription.doesCaseHaveSubscriptions()) || FALLBACK_LETTER_SUBSCRIPTION_TYPES.contains(notificationType))
            && notificationValidService.isNotificationStillValidToSend(wrapper.getNewSscsCaseData().getHearings(), notificationType)
            && notificationValidService.isHearingTypeValidToSendNotification(wrapper.getNewSscsCaseData(), notificationType);
    }

    static boolean isFallbackLetterRequired(NotificationWrapper wrapper, SubscriptionWithType subscriptionWithType, Subscription subscription, NotificationEventType eventType, NotificationValidService notificationValidService) {
        return (subscription != null && subscription.doesCaseHaveSubscriptions())
            || (subscription != null && !subscription.doesCaseHaveSubscriptions() && notificationValidService.isFallbackLetterRequiredForSubscriptionType(wrapper, subscriptionWithType.getSubscriptionType(), eventType))
            || (subscription == null && notificationValidService.isFallbackLetterRequiredForSubscriptionType(wrapper, subscriptionWithType.getSubscriptionType(), eventType));
    }

    protected static boolean isOkToSendSmsNotification(NotificationWrapper wrapper, Subscription subscription, Notification notification, NotificationEventType notificationType, NotificationValidService notificationValidService) {
        return subscription != null
            && subscription.isSmsSubscribed()
            && notification.isSms()
            && notification.getSmsTemplate() != null
            && subscription.doesCaseHaveSubscriptions()
            && notificationValidService.isNotificationStillValidToSend(wrapper.getNewSscsCaseData().getHearings(), notificationType)
            && notificationValidService.isHearingTypeValidToSendNotification(wrapper.getNewSscsCaseData(), notificationType);
    }

    protected static boolean isOkToSendEmailNotification(NotificationWrapper wrapper, Subscription subscription, Notification notification, NotificationEventType notificationType, NotificationValidService notificationValidService) {
        return subscription != null
            && subscription.isEmailSubscribed()
            && notification.isEmail()
            && notification.getEmailTemplate() != null
            && isOkToSendNotification(wrapper, wrapper.getNotificationType(), notificationValidService);
    }

    public static boolean hasLetterTemplate(Notification notification) {
        return notification.getLetterTemplate() != null;
    }

    public static boolean hasNoSubscriptions(Subscription subscription) {
        return subscription != null && !subscription.isSmsSubscribed() && !subscription.isEmailSubscribed();
    }

    public static final boolean hasSubscription(NotificationWrapper wrapper, SubscriptionType subscriptionType) {
        return APPELLANT.equals(subscriptionType)
            || APPOINTEE.equals(subscriptionType)
            || (REPRESENTATIVE.equals(subscriptionType) && null != wrapper.getNewSscsCaseData().getAppeal().getRep());
    }
}
