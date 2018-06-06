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
import uk.gov.service.notify.NotificationClient;

@Service
public class NotificationService {
    private static final Logger LOG = getLogger(NotificationService.class);

    private final NotificationClient client;
    private final NotificationFactory factory;
    private final ReminderService reminderService;

    @Autowired
    public NotificationService(NotificationClient client, NotificationFactory factory, ReminderService reminderService) {
        this.factory = factory;
        this.client = client;
        this.reminderService = reminderService;
    }

    public void createAndSendNotification(CcdResponseWrapper responseWrapper) {

        final Subscription appellantSubscription = responseWrapper.getNewCcdResponse().getSubscriptions().getAppellantSubscription();
        final String notificationEventType = responseWrapper.getNewCcdResponse().getNotificationType().getId();
        final String caseId = responseWrapper.getNewCcdResponse().getCaseId();

        LOG.info("Notification event triggered {} for case id {}", notificationEventType, caseId);

        if (appellantSubscription != null && appellantSubscription.doesCaseHaveSubscriptions()) {

            Notification notification = factory.create(responseWrapper);

            try {

                if (appellantSubscription.isEmailSubscribed() && notification.isEmail() && notification.getEmailTemplate() != null) {
                    LOG.info("Sending email template {} for case id: {}", notification.getEmailTemplate(), caseId);
                    client.sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
                    LOG.info("Email template {} sent for case id: {}", notification.getEmailTemplate(), caseId);
                }

                if (appellantSubscription.isSmsSubscribed() && notification.isSms() && notification.getSmsTemplate() != null) {
                    LOG.info("Sending SMS template {} for case id: {}", notification.getSmsTemplate(), caseId);
                    client.sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference());
                    LOG.info("SMS template {} sent for case id: {}", notification.getSmsTemplate(), caseId);
                }

                reminderService.createReminders(responseWrapper.getNewCcdResponse());

            } catch (Exception ex) {
                if (ex.getCause() instanceof UnknownHostException) {
                    NotificationClientRuntimeException exception = new NotificationClientRuntimeException(ex);
                    LOG.error("Runtime error on GovUKNotify for case id: " + caseId, exception);
                    throw exception;
                } else {
                    NotificationServiceException exception = new NotificationServiceException(ex);
                    LOG.error("Error on GovUKNotify for case id: " + caseId, exception);
                    throw exception;
                }
            }
        }
    }
}
