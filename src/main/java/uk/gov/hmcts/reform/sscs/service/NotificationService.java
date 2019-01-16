package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.getBenefitByCode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
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

@Service
@Slf4j
public class NotificationService {

    private final NotificationSender notificationSender;
    private final NotificationFactory notificationFactory;
    private final ReminderService reminderService;
    private final NotificationValidService notificationValidService;
    private final NotificationHandler notificationHandler;
    private final OutOfHoursCalculator outOfHoursCalculator;
    private final NotificationConfig notificationConfig;


    @Autowired
    public NotificationService(NotificationSender notificationSender, NotificationFactory notificationFactory,
                               ReminderService reminderService, NotificationValidService notificationValidService,
                               NotificationHandler notificationHandler,
                               OutOfHoursCalculator outOfHoursCalculator, NotificationConfig notificationConfig) {
        this.notificationFactory = notificationFactory;
        this.notificationSender = notificationSender;
        this.reminderService = reminderService;
        this.notificationValidService = notificationValidService;
        this.notificationHandler = notificationHandler;
        this.outOfHoursCalculator = outOfHoursCalculator;
        this.notificationConfig = notificationConfig;
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
                sendEmailSmsNotification(notificationWrapper, subscriptionWithType.getSubscription(), notification);
                processOldSubscriptionNotifications(notificationWrapper, notification);
            } else {
                notificationHandler.scheduleNotification(notificationWrapper);
            }
            reminderService.createReminders(notificationWrapper);
        }
    }

    private boolean isValidNotification(NotificationWrapper wrapper, Subscription
            subscription, NotificationEventType notificationType) {
        return subscription != null && subscription.doesCaseHaveSubscriptions()
                && notificationValidService.isNotificationStillValidToSend(wrapper.getNewSscsCaseData().getHearings(), notificationType)
                && notificationValidService.isHearingTypeValidToSendNotification(wrapper.getNewSscsCaseData(), notificationType);
    }

    private void processOldSubscriptionNotifications(NotificationWrapper wrapper, Notification notification) {
        if (wrapper.getNotificationType() == NotificationEventType.SUBSCRIPTION_UPDATED_NOTIFICATION) {
            boolean hasAppointee = wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee() != null;

            Subscription newSubscription = hasAppointee
                ? wrapper.getNewSscsCaseData().getSubscriptions().getAppointeeSubscription()
                : wrapper.getNewSscsCaseData().getSubscriptions().getAppellantSubscription();

            Subscription oldSubscription = hasAppointee
                ? wrapper.getOldSscsCaseData().getSubscriptions().getAppointeeSubscription()
                : wrapper.getOldSscsCaseData().getSubscriptions().getAppellantSubscription();

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

            Template template = notificationConfig.getTemplate(NotificationEventType.SUBSCRIPTION_OLD_NOTIFICATION.getId(),
                    NotificationEventType.SUBSCRIPTION_OLD_NOTIFICATION.getId(), benefit, wrapper.getHearingType());

            Notification oldNotification = Notification.builder().template(template).appealNumber(notification.getAppealNumber())
                    .destination(destination)
                    .reference(new Reference(wrapper.getOldSscsCaseData().getCaseReference()))
                    .appealNumber(notification.getAppealNumber())
                    .placeholders(notification.getPlaceholders()).build();

            sendEmailSmsNotification(wrapper, oldSubscription, oldNotification);
        }
    }

    private void sendEmailSmsNotification(NotificationWrapper wrapper, Subscription subscription, Notification
            notification) {
        if (subscription.isEmailSubscribed() && notification.isEmail() && notification.getEmailTemplate() != null) {
            NotificationHandler.SendNotification sendNotification = () ->
                    notificationSender.sendEmail(
                            notification.getEmailTemplate(),
                            notification.getEmail(),
                            notification.getPlaceholders(),
                            notification.getReference(),
                            wrapper.getCaseId()
                    );
            notificationHandler.sendNotification(wrapper, notification.getEmailTemplate(), "Email", sendNotification);
        }
        if (subscription.isSmsSubscribed() && notification.isSms() && notification.getSmsTemplate() != null) {
            NotificationHandler.SendNotification sendNotification = () ->
                    notificationSender.sendSms(
                            notification.getSmsTemplate(),
                            notification.getMobile(),
                            notification.getPlaceholders(),
                            notification.getReference(),
                            notification.getSmsSenderTemplate(),
                            wrapper.getCaseId()
                    );
            notificationHandler.sendNotification(wrapper, notification.getSmsTemplate(), "SMS", sendNotification);
        }
    }
}
