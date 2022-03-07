package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isNo;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPOINTEE;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.JOINT_PARTY;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.service.NotificationValidService.MANDATORY_LETTER_EVENT_TYPES;

import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.Notification;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;

public class NotificationUtils {

    private NotificationUtils() {
        // empty
    }

    /* Sometimes the data for the appointee comes in with null populated objects */
    public static boolean hasAppointee(SscsCaseDataWrapper wrapper) {
        return wrapper.getNewSscsCaseData().getAppeal() != null
            && wrapper.getNewSscsCaseData().getAppeal().getAppellant() != null
            && hasAppointee(wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee(),
                wrapper.getNewSscsCaseData().getAppeal().getAppellant().getIsAppointee());
    }

    public static boolean hasAppointee(Appointee appointee, YesNo isAppointee) {
        return !isNo(isAppointee) && appointee != null && appointee.getName() != null && appointee.getName().getFirstName() != null
            && appointee.getName().getLastName() != null;
    }

    /* Sometimes the data for the appointee comes in with null populated objects */
    public static boolean hasRepresentative(SscsCaseDataWrapper wrapper) {
        return wrapper.getNewSscsCaseData().getAppeal() != null
            && hasRepresentative(wrapper.getNewSscsCaseData().getAppeal());
    }

    public static boolean hasRepresentative(Appeal appeal) {
        return appeal.getRep() != null
            && isYes(appeal.getRep().getHasRepresentative());
    }

    public static boolean hasRepresentative(OtherParty otherParty) {
        return otherParty.getRep() != null
                && isYes(otherParty.getRep().getHasRepresentative());
    }

    public static boolean hasJointParty(SscsCaseData caseData) {
        return caseData.isThereAJointParty()
                && isNotBlank(trimToNull(caseData.getJointParty().getName().getFullName()));
    }

    public static boolean hasAppointeeSubscriptionOrIsMandatoryAppointeeLetter(SscsCaseDataWrapper wrapper) {
        Subscription subscription = getSubscription(wrapper.getNewSscsCaseData(), APPOINTEE);
        return hasAppointee(wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee(),
                wrapper.getNewSscsCaseData().getAppeal().getAppellant().getIsAppointee())
                && ((nonNull(subscription) && subscription.doesCaseHaveSubscriptions())
                || MANDATORY_LETTER_EVENT_TYPES.contains(wrapper.getNotificationEventType()));
    }

    public static boolean hasRepSubscriptionOrIsMandatoryRepLetter(SscsCaseDataWrapper wrapper) {
        Subscription subscription = getSubscription(wrapper.getNewSscsCaseData(), REPRESENTATIVE);
        return ((null != subscription && subscription.doesCaseHaveSubscriptions())
            || (hasRepresentative(wrapper.getNewSscsCaseData().getAppeal())
            && MANDATORY_LETTER_EVENT_TYPES.contains(wrapper.getNotificationEventType())));
    }

    public static boolean hasJointPartySubscription(SscsCaseDataWrapper wrapper) {
        Subscription subscription = getSubscription(wrapper.getNewSscsCaseData(), JOINT_PARTY);
        return ((null != subscription && subscription.doesCaseHaveSubscriptions()  && hasJointParty(wrapper.getNewSscsCaseData()))
                || (hasJointParty(wrapper.getNewSscsCaseData())
                && MANDATORY_LETTER_EVENT_TYPES.contains(wrapper.getNotificationEventType())));
    }

    public static boolean isValidSubscriptionOrIsMandatoryLetter(Subscription subscription, NotificationEventType eventType) {
        Subscription nullCheckedSubscription = getPopulatedSubscriptionOrNull(subscription);
        return ((null != nullCheckedSubscription && nullCheckedSubscription.doesCaseHaveSubscriptions())
                || MANDATORY_LETTER_EVENT_TYPES.contains(eventType));
    }

    public static Subscription getSubscription(SscsCaseData sscsCaseData, SubscriptionType subscriptionType) {
        if (REPRESENTATIVE.equals(subscriptionType)) {
            return getPopulatedSubscriptionOrNull(sscsCaseData.getSubscriptions().getRepresentativeSubscription());
        } else if (APPELLANT.equals(subscriptionType)) {
            return getPopulatedSubscriptionOrNull(sscsCaseData.getSubscriptions().getAppellantSubscription());
        } else if (JOINT_PARTY.equals(subscriptionType)) {
            return getPopulatedSubscriptionOrNull(sscsCaseData.getSubscriptions().getJointPartySubscription());
        } else {
            return getPopulatedSubscriptionOrNull(sscsCaseData.getSubscriptions().getAppointeeSubscription());
        }
    }

    private static Subscription getPopulatedSubscriptionOrNull(Subscription subscription) {
        if (null == subscription
            || (null == subscription.getWantSmsNotifications()
            && null == subscription.getTya()
            && null == subscription.getEmail()
            && null == subscription.getMobile()
            && null == subscription.getSubscribeEmail()
            && null == subscription.getSubscribeSms()
            && null == subscription.getReason())
        ) {
            return null;
        }

        return subscription;
    }

    static boolean isOkToSendNotification(NotificationWrapper wrapper, NotificationEventType notificationType,
                                          Subscription subscription,
                                          NotificationValidService notificationValidService) {
        return ((subscription != null
                && subscription.doesCaseHaveSubscriptions()))
            && notificationValidService.isNotificationStillValidToSend(wrapper.getNewSscsCaseData().getHearings(), notificationType)
            && notificationValidService.isHearingTypeValidToSendNotification(wrapper.getNewSscsCaseData(), notificationType);
    }


    protected static boolean isOkToSendSmsNotification(NotificationWrapper wrapper, Subscription subscription,
                                                       Notification notification, NotificationEventType notificationType,
                                                       NotificationValidService notificationValidService) {
        return subscription != null
            && subscription.isSmsSubscribed()
            && notification.isSms()
            && subscription.doesCaseHaveSubscriptions()
            && notificationValidService.isNotificationStillValidToSend(wrapper.getNewSscsCaseData().getHearings(), notificationType)
            && notificationValidService.isHearingTypeValidToSendNotification(wrapper.getNewSscsCaseData(), notificationType);
    }

    protected static boolean isOkToSendEmailNotification(NotificationWrapper wrapper, Subscription subscription,
                                                         Notification notification,
                                                         NotificationValidService notificationValidService) {
        return subscription != null
            && subscription.isEmailSubscribed()
            && notification.isEmail()
            && notification.getEmailTemplate() != null
            && isOkToSendNotification(wrapper, wrapper.getNotificationType(), subscription, notificationValidService);
    }

    static boolean hasLetterTemplate(Notification notification) {
        return notification.getLetterTemplate() != null;
    }

    static boolean hasNoSubscriptions(Subscription subscription) {
        return subscription == null || (!subscription.isSmsSubscribed() && !subscription.isEmailSubscribed());
    }

    static boolean hasSubscription(NotificationWrapper wrapper, SubscriptionType subscriptionType) {
        return APPELLANT.equals(subscriptionType)
            || APPOINTEE.equals(subscriptionType)
            || JOINT_PARTY.equals(subscriptionType)
            || (REPRESENTATIVE.equals(subscriptionType) && null != wrapper.getNewSscsCaseData().getAppeal().getRep());
    }
}
