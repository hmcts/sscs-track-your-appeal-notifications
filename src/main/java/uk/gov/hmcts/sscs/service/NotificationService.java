package uk.gov.hmcts.sscs.service;

import static org.slf4j.LoggerFactory.getLogger;

import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.domain.Subscription;
import uk.gov.hmcts.sscs.domain.notify.Notification;
import uk.gov.hmcts.sscs.exception.NotificationClientRuntimeException;
import uk.gov.hmcts.sscs.exception.NotificationServiceException;
import uk.gov.hmcts.sscs.factory.NotificationFactory;

@Service
public class NotificationService {
    private static final Logger LOG = getLogger(NotificationService.class);

    private final NotificationSender notificationSender;
    private final NotificationFactory factory;
    private final ReminderService reminderService;

    @Autowired
    public NotificationService(NotificationSender notificationSender, NotificationFactory factory, ReminderService reminderService) {
        this.factory = factory;
        this.notificationSender = notificationSender;
        this.reminderService = reminderService;
    }

    public void createAndSendNotification(CcdResponseWrapper responseWrapper) {

        final Subscription appellantSubscription = responseWrapper.getNewCcdResponse().getSubscriptions().getAppellantSubscription();
        final String notificationEventType = responseWrapper.getNewCcdResponse().getNotificationType().getId();
        final String caseId = responseWrapper.getNewCcdResponse().getCaseId();

        LOG.info("Notification event triggered {} for case id {}", notificationEventType, caseId);

        if (appellantSubscription != null && appellantSubscription.doesCaseHaveSubscriptions()) {

            Notification notification = factory.create(responseWrapper);

            if (appellantSubscription.isEmailSubscribed() && notification.isEmail() && notification.getEmailTemplate() != null) {

                try {
                    LOG.info("Sending email template {} for case id: {}", notification.getEmailTemplate(), caseId);
                    notificationSender.sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
                    LOG.info("Email template {} sent for case id: {}", notification.getEmailTemplate(), caseId);
                } catch (Exception ex) {
                    wrapAndThrowNotificationException(caseId, notification.getEmailTemplate(), ex);
                }
            }

            if (appellantSubscription.isSmsSubscribed() && notification.isSms() && notification.getSmsTemplate() != null) {

                try {
                    LOG.info("Sending SMS template {} for case id: {}", notification.getSmsTemplate(), caseId);
                    notificationSender.sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference());
                    LOG.info("SMS template {} sent for case id: {}", notification.getSmsTemplate(), caseId);
                } catch (Exception ex) {
                    wrapAndThrowNotificationException(caseId, notification.getSmsTemplate(), ex);
                }
            }

            reminderService.createReminders(responseWrapper.getNewCcdResponse());
        }
    }

    private void wrapAndThrowNotificationException(String caseId, String templateId, Exception ex) {
        if (ex.getCause() instanceof UnknownHostException) {
            NotificationClientRuntimeException exception = new NotificationClientRuntimeException(ex);
            LOG.error("Runtime error on GovUKNotify for case id: " + caseId + ", template: " + templateId, exception);
            throw exception;
        } else {
            NotificationServiceException exception = new NotificationServiceException(ex);
            LOG.error("Error on GovUKNotify for case id: " + caseId + ", template: " + templateId, exception);
            throw exception;
        }
    }
}
