package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.getBenefitByCode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.domain.notify.*;
import uk.gov.hmcts.reform.sscs.factory.NotificationFactory;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;

@Service
@Slf4j
public class NotificationService {

    private final NotificationSender notificationSender;
    private final NotificationFactory factory;
    private final ReminderService reminderService;
    private final NotificationValidService notificationValidService;
    private final NotificationHandler notificationHandler;
    private final OutOfHoursCalculator outOfHoursCalculator;
    private final NotificationConfig notificationConfig;


    @Autowired
    public NotificationService(NotificationSender notificationSender, NotificationFactory factory, ReminderService reminderService,
                               NotificationValidService notificationValidService, NotificationHandler notificationHandler,
                               OutOfHoursCalculator outOfHoursCalculator, NotificationConfig notificationConfig) {
        this.factory = factory;
        this.notificationSender = notificationSender;
        this.reminderService = reminderService;
        this.notificationValidService = notificationValidService;
        this.notificationHandler = notificationHandler;
        this.outOfHoursCalculator = outOfHoursCalculator;
        this.notificationConfig = notificationConfig;
    }

    public void createAndSendNotification(NotificationWrapper wrapper) {

        final Subscription appellantSubscription = wrapper.getAppellantSubscription();
        NotificationEventType notificationType = wrapper.getNotificationType();
        final String caseId = wrapper.getCaseId();

        log.info("Notification event triggered {} for case id {}", notificationType.getId(), caseId);

        if (appellantSubscription != null && appellantSubscription.doesCaseHaveSubscriptions()
                && notificationValidService.isNotificationStillValidToSend(wrapper.getNewSscsCaseData().getHearings(), notificationType)
                && notificationValidService.isHearingTypeValidToSendNotification(wrapper.getNewSscsCaseData(), notificationType)) {

            Notification notification = factory.create(wrapper);

            if (wrapper.getNotificationType().isAllowOutOfHours() || !outOfHoursCalculator.isItOutOfHours()) {
                sendEmailSmsNotification(wrapper, appellantSubscription, notification);
                processOldSubscriptionNotifications(wrapper, notification);
            } else {
                notificationHandler.scheduleNotification(wrapper);
            }

            reminderService.createReminders(wrapper);
        }
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
                smsNumber =  oldSubscription.getMobile();
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

    private void sendEmailSmsNotification(NotificationWrapper wrapper, Subscription appellantSubscription, Notification notification) {
        if (appellantSubscription.isEmailSubscribed() && notification.isEmail() && notification.getEmailTemplate() != null) {
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
        if (appellantSubscription.isSmsSubscribed() && notification.isSms() && notification.getSmsTemplate() != null) {
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
