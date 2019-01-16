package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.getBenefitByCode;
import static uk.gov.hmcts.reform.sscs.service.NotificationValidService.isMandatoryLetter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
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
        for (SubscriptionWithType subscriptionWithType :
                notificationWrapper.getSubscriptionsBasedOnNotificationType()) {
            sendNotificationPerSubscription(notificationWrapper, subscriptionWithType, notificationType);
        }
    }

    private void sendNotificationPerSubscription(NotificationWrapper notificationWrapper,
                                                 SubscriptionWithType subscriptionWithType,
                                                 NotificationEventType notificationType) {
        if (isValidNotification(notificationWrapper, subscriptionWithType.getSubscription(), notificationType)) {
            Notification notification = notificationFactory.create(notificationWrapper,
                    subscriptionWithType.getSubscriptionType());
            if (notificationWrapper.getNotificationType().isAllowOutOfHours() || !outOfHoursCalculator.isItOutOfHours()) {
                sendNotificationService.sendEmailSmsLetterNotification(notificationWrapper, subscriptionWithType.getSubscription(), notification);
                processOldSubscriptionNotifications(notificationWrapper, notification);
            } else {
                notificationHandler.scheduleNotification(notificationWrapper);
            }
            reminderService.createReminders(notificationWrapper);
        }
    }

    private boolean isValidNotification(NotificationWrapper wrapper, Subscription
            subscription, NotificationEventType notificationType) {
        return (isMandatoryLetter(notificationType) || (subscription != null && subscription.doesCaseHaveSubscriptions()
                && notificationValidService.isNotificationStillValidToSend(wrapper.getNewSscsCaseData().getHearings(), notificationType)
                && notificationValidService.isHearingTypeValidToSendNotification(wrapper.getNewSscsCaseData(), notificationType)));
    }

    private void processOldSubscriptionNotifications(NotificationWrapper wrapper, Notification notification) {
        if (wrapper.getNotificationType() == NotificationEventType.SUBSCRIPTION_UPDATED_NOTIFICATION
                && wrapper.getHearingType() == AppealHearingType.PAPER) {
            Subscription newSubscription = wrapper.getNewSscsCaseData().getSubscriptions().getAppellantSubscription();
            Subscription oldSubscription = wrapper.getOldSscsCaseData().getSubscriptions().getAppellantSubscription();

            String emailAddress = null;
            String smsNumber = null;

            if (null != newSubscription.getEmail() && null != oldSubscription.getEmail()) {
                emailAddress = newSubscription.getEmail().equals(oldSubscription.getEmail()) ? null : oldSubscription.getEmail();
            } else if (null == newSubscription.getEmail() && null != oldSubscription.getEmail()) {
                emailAddress = oldSubscription.getEmail();
            }

            if (null != newSubscription.getMobile() && null != oldSubscription.getMobile()) {
                smsNumber = newSubscription.getMobile().equals(oldSubscription.getMobile()) ? null : oldSubscription.getMobile();
            } else if (null == newSubscription.getMobile() && null != oldSubscription.getMobile()) {
                smsNumber = oldSubscription.getMobile();
            }


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

            sendNotificationService.sendEmailSmsLetterNotification(wrapper, oldSubscription, oldNotification);
        }
    }


}
