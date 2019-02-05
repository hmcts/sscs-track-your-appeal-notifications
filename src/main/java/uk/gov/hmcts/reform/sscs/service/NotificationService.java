package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.getBenefitByCode;
import static uk.gov.hmcts.reform.sscs.service.NotificationValidService.isFallbackLetterRequiredForSubscriptionType;
import static uk.gov.hmcts.reform.sscs.service.NotificationValidService.isMandatoryLetter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.domain.notify.*;
import uk.gov.hmcts.reform.sscs.factory.NotificationFactory;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;

@Service
@Slf4j
public class NotificationService {
    private final NotificationFactory notificationFactory;
    private final ReminderService reminderService;
    private final NotificationValidService notificationValidService;
    private final NotificationHandler notificationHandler;
    private final OutOfHoursCalculator outOfHoursCalculator;
    private final NotificationConfig notificationConfig;
    private final SendNotificationService sendNotificationService;

    @Autowired
    public NotificationService(
            NotificationFactory notificationFactory,
            ReminderService reminderService,
            NotificationValidService notificationValidService,
            NotificationHandler notificationHandler,
            OutOfHoursCalculator outOfHoursCalculator,
            NotificationConfig notificationConfig,
            SendNotificationService sendNotificationService) {
        this.notificationFactory = notificationFactory;
        this.reminderService = reminderService;
        this.notificationValidService = notificationValidService;
        this.notificationHandler = notificationHandler;
        this.outOfHoursCalculator = outOfHoursCalculator;
        this.notificationConfig = notificationConfig;
        this.sendNotificationService = sendNotificationService;
    }

    public void manageNotificationAndSubscription(NotificationWrapper notificationWrapper) {
        NotificationEventType notificationType = notificationWrapper.getNotificationType();
        final String caseId = notificationWrapper.getCaseId();
        log.info("Notification event triggered {} for case id {}", notificationType.getId(), caseId);

        if (notificationWrapper.getNotificationType().isAllowOutOfHours() || !outOfHoursCalculator.isItOutOfHours()) {
            sendNotificationPerSubscription(notificationWrapper, notificationType);
        } else {
            notificationHandler.scheduleNotification(notificationWrapper);
        }
    }

    private void sendNotificationPerSubscription(NotificationWrapper notificationWrapper,
                                                 NotificationEventType notificationType) {
        for (SubscriptionWithType subscriptionWithType : notificationWrapper.getSubscriptionsBasedOnNotificationType()) {
            if (isValidNotification(notificationWrapper, subscriptionWithType, notificationType)) {
                Notification notification = notificationFactory.create(notificationWrapper, subscriptionWithType.getSubscriptionType());

                sendNotificationService.sendEmailSmsLetterNotification(notificationWrapper, subscriptionWithType.getSubscription(), notification, subscriptionWithType);
                processOldSubscriptionNotifications(notificationWrapper, notification, subscriptionWithType);
                reminderService.createReminders(notificationWrapper);
            }
        }
    }

    private boolean isValidNotification(NotificationWrapper wrapper, SubscriptionWithType
            subscriptionWithType, NotificationEventType notificationType) {
        Subscription subscription = subscriptionWithType.getSubscription();

        return (isMandatoryLetter(notificationType)
            || ((subscription != null && subscription.doesCaseHaveSubscriptions()
            || (subscription != null && !subscription.doesCaseHaveSubscriptions() && isFallbackLetterRequiredForSubscriptionType(wrapper, subscriptionWithType.getSubscriptionType(), notificationType)
            || subscription == null && isFallbackLetterRequiredForSubscriptionType(wrapper, subscriptionWithType.getSubscriptionType(), notificationType)))
            && notificationValidService.isNotificationStillValidToSend(wrapper.getNewSscsCaseData().getHearings(), notificationType)
            && notificationValidService.isHearingTypeValidToSendNotification(wrapper.getNewSscsCaseData(), notificationType)));
    }

    private void processOldSubscriptionNotifications(NotificationWrapper wrapper, Notification notification, SubscriptionWithType subscriptionWithType) {
        if (wrapper.getNotificationType() == NotificationEventType.SUBSCRIPTION_UPDATED_NOTIFICATION) {
            boolean hasAppointee = wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee() != null;

            Subscription newSubscription = hasAppointee
                ? wrapper.getNewSscsCaseData().getSubscriptions().getAppointeeSubscription()
                : wrapper.getNewSscsCaseData().getSubscriptions().getAppellantSubscription();

            Subscription oldSubscription = hasAppointee
                ? wrapper.getOldSscsCaseData().getSubscriptions().getAppointeeSubscription()
                : wrapper.getOldSscsCaseData().getSubscriptions().getAppellantSubscription();

            String emailAddress = getSubscriptionDetails(newSubscription.getEmail(), oldSubscription.getEmail());
            String smsNumber = getSubscriptionDetails(newSubscription.getMobile(), oldSubscription.getMobile());

            Destination destination = Destination.builder().email(emailAddress).sms(smsNumber).build();

            Benefit benefit = getBenefitByCode(wrapper.getSscsCaseDataWrapper()
                    .getNewSscsCaseData().getAppeal().getBenefitType().getCode());

            Template template = notificationConfig.getTemplate(
                    NotificationEventType.SUBSCRIPTION_OLD_NOTIFICATION.getId(),
                    NotificationEventType.SUBSCRIPTION_OLD_NOTIFICATION.getId(),
                    NotificationEventType.SUBSCRIPTION_OLD_NOTIFICATION.getId(),
                    benefit,
                    wrapper.getHearingType()
            );

            Notification oldNotification = Notification.builder().template(template).appealNumber(notification.getAppealNumber())
                    .destination(destination)
                    .reference(new Reference(wrapper.getOldSscsCaseData().getCaseReference()))
                    .appealNumber(notification.getAppealNumber())
                    .placeholders(notification.getPlaceholders()).build();

            sendNotificationService.sendEmailSmsLetterNotification(wrapper, oldSubscription, oldNotification, subscriptionWithType);
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
}
