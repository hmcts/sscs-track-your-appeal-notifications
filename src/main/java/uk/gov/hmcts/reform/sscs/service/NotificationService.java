package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.getBenefitByCode;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SUBSCRIPTION_UPDATED;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPOINTEE;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ADMIN_APPEAL_WITHDRAWN;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_WITHDRAWN_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DECISION_ISSUED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DIRECTION_ISSUED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DWP_UPLOAD_RESPONSE_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.HEARING_BOOKED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.HEARING_REMINDER_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.REQUEST_INFO_INCOMPLETE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.STRUCK_OUT;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SUBSCRIPTION_UPDATED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.getNotificationByCcdEvent;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.getSubscription;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.isFallbackLetterRequired;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.isOkToSendNotification;
import static uk.gov.hmcts.reform.sscs.service.NotificationValidService.isMandatoryLetterEventType;

import java.time.ZonedDateTime;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.domain.notify.Destination;
import uk.gov.hmcts.reform.sscs.domain.notify.Notification;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.domain.notify.Reference;
import uk.gov.hmcts.reform.sscs.domain.notify.Template;
import uk.gov.hmcts.reform.sscs.factory.NotificationFactory;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.utility.PhoneNumbersUtil;

@Service
@Slf4j
public class NotificationService {
    private final NotificationFactory notificationFactory;
    private final ReminderService reminderService;
    private final NotificationValidService notificationValidService;
    private final NotificationHandler notificationHandler;
    private final OutOfHoursCalculator outOfHoursCalculator;
    private final NotificationConfig notificationConfig;
    private final long delayTimeInSeconds;

    @SuppressWarnings("squid:S107")
    @Autowired
    public NotificationService(
            NotificationFactory notificationFactory,
            ReminderService reminderService,
            NotificationValidService notificationValidService,
            NotificationHandler notificationHandler,
            OutOfHoursCalculator outOfHoursCalculator,
            NotificationConfig notificationConfig,
            SendNotificationService sendNotificationService,
            @Value("${feature.covid19}") boolean covid19Feature,
            @Value("${delayEvent.timeInSeconds}") long delayTimeInSeconds) {

        this.notificationFactory = notificationFactory;
        this.reminderService = reminderService;
        this.notificationValidService = notificationValidService;
        this.notificationHandler = notificationHandler;
        this.outOfHoursCalculator = outOfHoursCalculator;
        this.notificationConfig = notificationConfig;
        this.sendNotificationService = sendNotificationService;
        this.covid19Feature = covid19Feature;
        this.delayTimeInSeconds = delayTimeInSeconds;
    }

    private final SendNotificationService sendNotificationService;

    private final boolean covid19Feature;

    public void manageNotificationAndSubscription(NotificationWrapper notificationWrapper) {
        NotificationEventType notificationType = notificationWrapper.getNotificationType();
        final String caseId = notificationWrapper.getCaseId();

        log.info("Checking if notification event {} is valid for case id {}", notificationType.getId(), caseId);
        if (!isEventAllowedToProceedWithValidData(notificationWrapper, notificationType)) {
            return;
        }

        log.info("Notification event triggered {} for case id {}", notificationType.getId(), caseId);

        if (notificationWrapper.getNotificationType().isToBeDelayed()
                && (CollectionUtils.isNotEmpty(notificationWrapper.getNewSscsCaseData().getEvents())
                && notificationWrapper.getNewSscsCaseData().getEvents().get(0).getValue().getDateTime().plusSeconds(delayTimeInSeconds).isAfter(ZonedDateTime.now()))) {
            notificationHandler.scheduleNotification(notificationWrapper, ZonedDateTime.now().plusSeconds(delayTimeInSeconds));
        } else if (notificationWrapper.getNotificationType().isAllowOutOfHours() || !outOfHoursCalculator.isItOutOfHours()) {
            sendNotificationPerSubscription(notificationWrapper, notificationType);
            reminderService.createReminders(notificationWrapper);
        } else {
            notificationHandler.scheduleNotification(notificationWrapper);
        }
    }

    private void sendNotificationPerSubscription(NotificationWrapper notificationWrapper,
                                                 NotificationEventType notificationType) {
        for (SubscriptionWithType subscriptionWithType : notificationWrapper.getSubscriptionsBasedOnNotificationType()) {
            if (isValidNotification(notificationWrapper, subscriptionWithType, notificationType)) {
                sendNotification(notificationWrapper, subscriptionWithType, notificationType);
                resendLastNotification(notificationWrapper, subscriptionWithType, notificationType);
            } else {
                log.error("Is not a valid notification event {} for case id {}, not sending notification.", notificationType.getId(), notificationWrapper.getCaseId());
            }
        }
    }

    private void resendLastNotification(NotificationWrapper notificationWrapper, SubscriptionWithType subscriptionWithType,
                                        NotificationEventType notificationType) {
        if (subscriptionWithType.getSubscription() != null && shouldProcessLastNotification(notificationWrapper, subscriptionWithType)) {
            NotificationEventType lastEvent = getNotificationByCcdEvent(notificationWrapper.getNewSscsCaseData().getEvents().get(0)
                .getValue().getEventType());
            log.info("Resending the last notification for event {} and case id {}.", lastEvent.getId(), notificationWrapper.getCaseId());
            scrubEmailAndSmsIfSubscribedBefore(notificationWrapper, subscriptionWithType);
            notificationWrapper.getSscsCaseDataWrapper().setNotificationEventType(lastEvent);
            sendNotification(notificationWrapper, subscriptionWithType, notificationType);
            notificationWrapper.getSscsCaseDataWrapper().setNotificationEventType(SUBSCRIPTION_UPDATED_NOTIFICATION);
        }
    }

    private void scrubEmailAndSmsIfSubscribedBefore(NotificationWrapper notificationWrapper, SubscriptionWithType subscriptionWithType) {
        Subscription oldSubscription = getSubscription(notificationWrapper.getOldSscsCaseData(), subscriptionWithType.getSubscriptionType());
        Subscription newSubscription = subscriptionWithType.getSubscription();
        String email = oldSubscription != null && oldSubscription.isEmailSubscribed() ? null : newSubscription.getEmail();
        String mobile = oldSubscription != null && oldSubscription.isSmsSubscribed() ? null : newSubscription.getMobile();
        subscriptionWithType.setSubscription(newSubscription.toBuilder().email(email).mobile(mobile).build());
    }

    private boolean shouldProcessLastNotification(NotificationWrapper notificationWrapper, SubscriptionWithType subscriptionWithType) {
        return SUBSCRIPTION_UPDATED_NOTIFICATION.equals(notificationWrapper.getSscsCaseDataWrapper().getNotificationEventType())
            && hasCaseJustSubscribed(subscriptionWithType.getSubscription(), getSubscription(notificationWrapper.getOldSscsCaseData(), subscriptionWithType.getSubscriptionType()))
            && thereIsALastEventThatIsNotSubscriptionUpdated(notificationWrapper.getNewSscsCaseData());
    }

    static Boolean hasCaseJustSubscribed(Subscription newSubscription, Subscription oldSubscription) {
        return ((oldSubscription == null || !oldSubscription.isEmailSubscribed()) && newSubscription.isEmailSubscribed()
                || ((oldSubscription == null || !oldSubscription.isSmsSubscribed()) && newSubscription.isSmsSubscribed()));
    }

    private static boolean thereIsALastEventThatIsNotSubscriptionUpdated(final SscsCaseData newSscsCaseData) {
        boolean thereIsALastEventThatIsNotSubscriptionUpdated = newSscsCaseData.getEvents() != null
            && !newSscsCaseData.getEvents().isEmpty()
            && newSscsCaseData.getEvents().get(0).getValue().getEventType() != null
            && !SUBSCRIPTION_UPDATED.equals(newSscsCaseData.getEvents().get(0).getValue().getEventType());
        if (!thereIsALastEventThatIsNotSubscriptionUpdated) {
            log.info("Not re-sending the last subscription as there is no last event for ccdCaseId {}.", newSscsCaseData.getCcdCaseId());
        }
        return thereIsALastEventThatIsNotSubscriptionUpdated;
    }

    private void sendNotification(NotificationWrapper notificationWrapper, SubscriptionWithType subscriptionWithType,
                                  NotificationEventType notificationType) {
        Notification notification = notificationFactory.create(notificationWrapper, subscriptionWithType);
        sendNotificationService.sendEmailSmsLetterNotification(notificationWrapper, notification, subscriptionWithType, notificationType);
        processOldSubscriptionNotifications(notificationWrapper, notification, subscriptionWithType, notificationType);
    }

    private boolean isValidNotification(NotificationWrapper wrapper, SubscriptionWithType subscriptionWithType,
                                        NotificationEventType notificationType) {
        Subscription subscription = subscriptionWithType.getSubscription();

        return (isMandatoryLetterEventType(notificationType)
            || (isFallbackLetterRequired(wrapper, subscriptionWithType, subscription, notificationType, notificationValidService)
            && isOkToSendNotification(wrapper, notificationType, subscription, notificationValidService)));
    }

    private void processOldSubscriptionNotifications(NotificationWrapper wrapper, Notification notification, SubscriptionWithType subscriptionWithType, NotificationEventType eventType) {
        if (wrapper.getNotificationType() == SUBSCRIPTION_UPDATED_NOTIFICATION) {
            Subscription newSubscription;
            Subscription oldSubscription;
            if (REPRESENTATIVE.equals(subscriptionWithType.getSubscriptionType())) {
                newSubscription = wrapper.getNewSscsCaseData().getSubscriptions().getRepresentativeSubscription();
                oldSubscription = wrapper.getOldSscsCaseData().getSubscriptions().getRepresentativeSubscription();
            } else if (APPOINTEE.equals(subscriptionWithType.getSubscriptionType())) {
                newSubscription = wrapper.getNewSscsCaseData().getSubscriptions().getAppointeeSubscription();
                oldSubscription = wrapper.getOldSscsCaseData().getSubscriptions().getAppointeeSubscription();
            } else {
                newSubscription = wrapper.getNewSscsCaseData().getSubscriptions().getAppellantSubscription();
                oldSubscription = wrapper.getOldSscsCaseData().getSubscriptions().getAppellantSubscription();
            }

            String emailAddress = getSubscriptionDetails(newSubscription.getEmail(), oldSubscription.getEmail());
            String smsNumber = getSubscriptionDetails(newSubscription.getMobile(), oldSubscription.getMobile());

            Destination destination = Destination.builder().email(emailAddress)
                .sms(PhoneNumbersUtil.cleanPhoneNumber(smsNumber).orElse(smsNumber)).build();

            Benefit benefit = getBenefitByCode(wrapper.getSscsCaseDataWrapper()
                .getNewSscsCaseData().getAppeal().getBenefitType().getCode());

            Template template = notificationConfig.getTemplate(
                NotificationEventType.SUBSCRIPTION_OLD_NOTIFICATION.getId(),
                NotificationEventType.SUBSCRIPTION_OLD_NOTIFICATION.getId(),
                NotificationEventType.SUBSCRIPTION_OLD_NOTIFICATION.getId(),
                NotificationEventType.SUBSCRIPTION_OLD_NOTIFICATION.getId(),
                benefit,
                wrapper.getHearingType(),
                "validAppeal"
            );

            Notification oldNotification = Notification.builder().template(template).appealNumber(notification.getAppealNumber())
                .destination(destination)
                .reference(new Reference(wrapper.getOldSscsCaseData().getCaseReference()))
                .appealNumber(notification.getAppealNumber())
                .placeholders(notification.getPlaceholders()).build();

            SubscriptionWithType updatedSubscriptionWithType = new SubscriptionWithType(oldSubscription, subscriptionWithType.getSubscriptionType());
            sendNotificationService.sendEmailSmsLetterNotification(wrapper, oldNotification, updatedSubscriptionWithType, eventType);
        }
    }

    private String getSubscriptionDetails(String newSubscription, String oldSubscription) {
        String subscription = "";
        if (null != newSubscription && null != oldSubscription) {
            subscription = newSubscription.equals(oldSubscription) ? null : oldSubscription;
        } else if (null == newSubscription && null != oldSubscription) {
            subscription = oldSubscription;
        }
        return subscription;
    }

    private boolean isEventAllowedToProceedWithValidData(NotificationWrapper notificationWrapper,
                                                         NotificationEventType notificationType) {
        if (REQUEST_INFO_INCOMPLETE.equals(notificationType)) {
            if (StringUtils.isEmpty(notificationWrapper.getNewSscsCaseData().getInformationFromAppellant())
                || "No".equalsIgnoreCase(notificationWrapper.getNewSscsCaseData().getInformationFromAppellant())) {

                log.error("Request Incomplete Information with empty or no Information From Appellant for ccdCaseId {}.", notificationWrapper.getNewSscsCaseData().getCcdCaseId());
                return false;
            }
        }

        if (notificationWrapper.getSscsCaseDataWrapper().getState() != null
            && notificationWrapper.getSscsCaseDataWrapper().getState().equals(State.DORMANT_APPEAL_STATE)) {
            if (!(NotificationEventType.APPEAL_DORMANT_NOTIFICATION.equals(notificationType)
                    || NotificationEventType.APPEAL_LAPSED_NOTIFICATION.equals(notificationType)
                    || NotificationEventType.HMCTS_APPEAL_LAPSED_NOTIFICATION.equals(notificationType)
                    || NotificationEventType.DWP_APPEAL_LAPSED_NOTIFICATION.equals(notificationType)
                    || ADMIN_APPEAL_WITHDRAWN.equals(notificationType)
                    || APPEAL_WITHDRAWN_NOTIFICATION.equals(notificationType)
                    || STRUCK_OUT.equals(notificationType)
                    || DECISION_ISSUED.equals(notificationType)
                    || DIRECTION_ISSUED.equals(notificationType)
                    || NotificationEventType.DECISION_ISSUED_2.equals(notificationType))) {
                log.info(String.format("Cannot complete notification %s as the appeal was dormant for caseId %s.",
                    notificationType.getId(), notificationWrapper.getCaseId()));
                return false;
            }
        }

        if (!State.READY_TO_LIST.getId().equals(notificationWrapper.getSscsCaseDataWrapper().getNewSscsCaseData().getCreatedInGapsFrom())
                && DWP_UPLOAD_RESPONSE_NOTIFICATION.equals(notificationType)) {
            log.info(String.format("Cannot complete notification %s as the appeal was dwpUploadResponse for caseId %s.",
                    notificationType.getId(), notificationWrapper.getCaseId()));
            return false;
        }

        if (covid19Feature && (HEARING_BOOKED_NOTIFICATION.equals(notificationType) || HEARING_REMINDER_NOTIFICATION.equals(notificationType))) {
            log.info(String.format("Notification not valid to send as covid 19 feature flag on for case id %s and event %s in state %s", notificationWrapper.getCaseId(), notificationType.getId(), notificationWrapper.getSscsCaseDataWrapper().getState()));
            return false;
        }
        log.info(String.format("Notification valid to send for case id %s and event %s in state %s", notificationWrapper.getCaseId(), notificationType.getId(), notificationWrapper.getSscsCaseDataWrapper().getState()));
        return true;
    }
}
