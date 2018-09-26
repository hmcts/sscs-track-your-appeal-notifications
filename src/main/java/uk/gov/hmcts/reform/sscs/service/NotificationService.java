package uk.gov.hmcts.reform.sscs.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.domain.notify.Notification;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
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

    @Autowired
    public NotificationService(NotificationSender notificationSender, NotificationFactory factory, ReminderService reminderService,
                               NotificationValidService notificationValidService, NotificationHandler notificationHandler) {
        this.factory = factory;
        this.notificationSender = notificationSender;
        this.reminderService = reminderService;
        this.notificationValidService = notificationValidService;
        this.notificationHandler = notificationHandler;
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

            if (appellantSubscription.isEmailSubscribed() && notification.isEmail() && notification.getEmailTemplate() != null) {
                NotificationHandler.SendNotification sendNotification = () ->
                        notificationSender.sendEmail(
                                notification.getEmailTemplate(),
                                notification.getEmail(),
                                notification.getPlaceholders(),
                                notification.getReference()
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
                                notification.getSmsSenderTemplate()
                        );
                notificationHandler.sendNotification(wrapper, notification.getSmsTemplate(), "SMS", sendNotification);
            }

            reminderService.createReminders(wrapper);
        }
    }
}
