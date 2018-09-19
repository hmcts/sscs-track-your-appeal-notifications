package uk.gov.hmcts.reform.sscs.service;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.domain.notify.Notification;
import uk.gov.hmcts.reform.sscs.factory.NotificationFactory;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;

@Service
public class NotificationService {
    private static final Logger LOG = getLogger(NotificationService.class);

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
        final String notificationEventType = wrapper.getNotificationType().getId();
        final String caseId = wrapper.getCaseId();

        LOG.info("Notification event triggered {} for case id {}", notificationEventType, caseId);


        if (appellantSubscription != null && appellantSubscription.doesCaseHaveSubscriptions()
                && notificationValidService.isNotificationStillValidToSend(wrapper.getNewSscsCaseData().getHearings(), wrapper.getNotificationType())
                && notificationValidService.isHearingTypeValidToSendNotification(wrapper.getNewSscsCaseData(), wrapper.getNotificationType())) {

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
