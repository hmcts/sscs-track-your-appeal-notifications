package uk.gov.hmcts.sscs.service;

import static org.slf4j.LoggerFactory.getLogger;

import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.sscs.domain.AppealReason;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
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

        String notificationEventType = responseWrapper.getNewCcdResponse().getNotificationType().getId();

        LOG.info("Notification event triggered {} for case id {}", notificationEventType, responseWrapper.getNewCcdResponse().getCaseId());

        if (responseWrapper.getNewCcdResponse().getSubscriptions().getAppellantSubscription() != null && responseWrapper.getNewCcdResponse().getSubscriptions().getAppellantSubscription().doesCaseHaveSubscriptions()) {

            Notification notification = factory.create(responseWrapper);

            //TODO: Temporary logging to debug issue with pound symbol encoding
            if (responseWrapper.getNewCcdResponse().getAppeal() != null && responseWrapper.getNewCcdResponse().getAppeal().getAppealReasons() != null) {
                for (AppealReason reason : responseWrapper.getNewCcdResponse().getAppeal().getAppealReasons().getReasons()) {
                    LOG.info("An appeal reason for case id: {}, {}, {}", responseWrapper.getNewCcdResponse().getCaseId(), reason.getValue().getReason(), reason.getValue().getDescription());
                }
            }

            try {

                if (responseWrapper.getNewCcdResponse().getSubscriptions().getAppellantSubscription().isEmailSubscribed() && notification.isEmail() && notification.getEmailTemplate() != null) {
                    LOG.info("Sending email template {} for case id: {}", notification.getEmailTemplate(), responseWrapper.getNewCcdResponse().getCaseId());
                    client.sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
                    LOG.info("Email template {} sent for case id: {}", notification.getEmailTemplate(), responseWrapper.getNewCcdResponse().getCaseId());
                }
                if (responseWrapper.getNewCcdResponse().getSubscriptions().getAppellantSubscription().isSmsSubscribed() && notification.isSms() && notification.getSmsTemplate() != null) {
                    LOG.info("Sending SMS template {} for case id: {}", notification.getSmsTemplate(), responseWrapper.getNewCcdResponse().getCaseId());
                    client.sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference());
                    LOG.info("SMS template {} sent for case id: {}", notification.getSmsTemplate(), responseWrapper.getNewCcdResponse().getCaseId());
                }

                reminderService.createReminders(responseWrapper.getNewCcdResponse());

            } catch (Exception ex) {
                if (ex.getCause() instanceof UnknownHostException) {
                    NotificationClientRuntimeException exception = new NotificationClientRuntimeException(ex);
                    LOG.error("Runtime error on GovUKNotify for case id: " + responseWrapper.getNewCcdResponse().getCaseId(), exception);
                    throw exception;
                } else {
                    NotificationServiceException exception = new NotificationServiceException(ex);
                    LOG.error("Error on GovUKNotify for case id: " + responseWrapper.getNewCcdResponse().getCaseId(), exception);
                    throw exception;
                }
            }
        }
    }
}
