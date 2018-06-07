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

        Subscription appellantSubscription = responseWrapper.getNewCcdResponse().getSubscriptions().getAppellantSubscription();
        String caseReference = responseWrapper.getNewCcdResponse().getCaseReference();

        LOG.info("Start to create notification for case reference: {}", caseReference);

        if (appellantSubscription != null && appellantSubscription.doesCaseHaveSubscriptions()) {

            Notification notification = factory.create(responseWrapper);

            try {

                if (appellantSubscription.isEmailSubscribed() && notification.isEmail() && notification.getEmailTemplate() != null) {
                    LOG.info("Sending email for case reference: {}", caseReference);
                    client.sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
                    LOG.info("Email sent for case reference: {}", caseReference);
                }

                if (appellantSubscription.isSmsSubscribed() && notification.isSms() && notification.getSmsTemplate() != null) {
                    LOG.info("Sending SMS for case reference: {}", caseReference);
                    client.sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference());
                    LOG.info("SMS sent for case reference: {}", caseReference);
                }

                reminderService.createReminders(responseWrapper.getNewCcdResponse());

            } catch (Exception ex) {
                if (ex.getCause() instanceof UnknownHostException) {
                    NotificationClientRuntimeException exception = new NotificationClientRuntimeException(ex);
                    LOG.error("Runtime error on GovUKNotify for case reference " + caseReference, exception);
                    throw exception;
                } else {
                    NotificationServiceException exception = new NotificationServiceException(ex);
                    LOG.error("Error on GovUKNotify for case reference " + caseReference, exception);
                    throw exception;
                }
            }
        }
    }

}
