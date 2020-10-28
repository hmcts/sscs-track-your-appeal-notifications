package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPOINTEE;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.JOINT_PARTY;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.service.NotificationValidService.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
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

    public static boolean hasAppointee(Appointee appointee, String isAppointee) {
        return !equalsIgnoreCase(isAppointee, "No") && appointee != null && appointee.getName() != null && appointee.getName().getFirstName() != null
            && appointee.getName().getLastName() != null;
    }

    /* Sometimes the data for the appointee comes in with null populated objects */
    public static boolean hasRepresentative(SscsCaseDataWrapper wrapper) {
        return wrapper.getNewSscsCaseData().getAppeal() != null
            && hasRepresentative(wrapper.getNewSscsCaseData().getAppeal());
    }

    public static boolean hasRepresentative(Appeal appeal) {
        return appeal.getRep() != null
            && appeal.getRep().getHasRepresentative() != null
            && appeal.getRep().getHasRepresentative().equalsIgnoreCase("yes");
    }

    public static boolean hasJointParty(SscsCaseData caseData) {
        return caseData.isThereAJointParty()
                && isNotBlank(trimToNull(caseData.getJointPartyName().getFullName()));
    }

    public static boolean hasAppointeeSubscriptionOrIsMandatoryAppointeeLetter(SscsCaseDataWrapper wrapper) {
        Subscription subscription = getSubscription(wrapper.getNewSscsCaseData(), APPOINTEE);
        return hasAppointee(wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee(),
                wrapper.getNewSscsCaseData().getAppeal().getAppellant().getIsAppointee())
                && ((nonNull(subscription) && subscription.doesCaseHaveSubscriptions())
                || LETTER_EVENT_TYPES.contains(wrapper.getNotificationEventType()));
    }

    public static boolean hasRepSubscriptionOrIsMandatoryRepLetter(SscsCaseDataWrapper wrapper) {
        Subscription subscription = getSubscription(wrapper.getNewSscsCaseData(), REPRESENTATIVE);
        return ((null != subscription && subscription.doesCaseHaveSubscriptions())
            || (hasRepresentative(wrapper.getNewSscsCaseData().getAppeal())
            && LETTER_EVENT_TYPES.contains(wrapper.getNotificationEventType())));
    }

    public static boolean hasJointPartySubscription(SscsCaseDataWrapper wrapper) {
        Subscription subscription = getSubscription(wrapper.getNewSscsCaseData(), JOINT_PARTY);
        return ((null != subscription && subscription.doesCaseHaveSubscriptions()  && hasJointParty(wrapper.getNewSscsCaseData()))
                || (hasJointParty(wrapper.getNewSscsCaseData())
                && LETTER_EVENT_TYPES.contains(wrapper.getNotificationEventType())));
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
                && subscription.doesCaseHaveSubscriptions())
                || FALLBACK_LETTER_SUBSCRIPTION_TYPES.contains(notificationType))
            && notificationValidService.isNotificationStillValidToSend(wrapper.getNewSscsCaseData().getHearings(), notificationType)
            && notificationValidService.isHearingTypeValidToSendNotification(wrapper.getNewSscsCaseData(), notificationType);
    }

    static boolean isFallbackLetterRequired(NotificationWrapper wrapper, SubscriptionWithType subscriptionWithType,
                                            Subscription subscription, NotificationEventType eventType,
                                            NotificationValidService notificationValidService) {
        return (subscription != null && subscription.doesCaseHaveSubscriptions())
            || (subscription != null && !subscription.doesCaseHaveSubscriptions()
            && notificationValidService.isFallbackLetterRequiredForSubscriptionType(wrapper, subscriptionWithType.getSubscriptionType(), eventType))
            || (subscription == null && notificationValidService.isFallbackLetterRequiredForSubscriptionType(wrapper, subscriptionWithType.getSubscriptionType(), eventType));
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

    public static Hearing getLatestHearing(SscsCaseData sscsCaseData) {

        if (sscsCaseData.getHearings() == null || sscsCaseData.getHearings().isEmpty()) {
            return null;
        }

        List<Hearing> hearings = new ArrayList<>(sscsCaseData.getHearings());

        Comparator<Hearing> compareByIdAndDate = (o1, o2) -> {
            HearingDetails hearingDetails = o1.getValue();
            HearingDetails nextHearingDetails = o2.getValue();
            int idCompare = 0;

            if (!isEmpty(hearingDetails.getHearingId()) && !isEmpty(nextHearingDetails.getHearingId())) {
                idCompare = hearingDetails.getHearingId().compareTo(nextHearingDetails.getHearingId());
            }

            if (idCompare != 0) {
                return -1 * idCompare;
            }
            return -1 * hearingDetails.getHearingDateTime().compareTo(nextHearingDetails.getHearingDateTime());
        };

        hearings.sort(compareByIdAndDate);

        return hearings.get(0);
    }
}
