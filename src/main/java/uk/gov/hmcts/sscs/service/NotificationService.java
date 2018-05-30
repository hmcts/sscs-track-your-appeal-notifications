package uk.gov.hmcts.sscs.service;

import static org.slf4j.LoggerFactory.getLogger;

import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.sscs.domain.CcdResponse;
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
        LOG.info("Start to create notification for case reference " + responseWrapper.getNewCcdResponse().getCaseReference());

        if (responseWrapper.getNewCcdResponse().getSubscriptions().getAppellantSubscription() != null && responseWrapper.getNewCcdResponse().getSubscriptions().getAppellantSubscription().doesCaseHaveSubscriptions()) {

            Notification notification = factory.create(responseWrapper);

            try {

                if (responseWrapper.getNewCcdResponse().getSubscriptions().getAppellantSubscription().isEmailSubscribed() && notification.isEmail() && notification.getEmailTemplate() != null) {
                    LOG.info("Sending email for case reference " + responseWrapper.getNewCcdResponse().getCaseReference());
                    client.sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
                    LOG.info("Email sent for case reference " + responseWrapper.getNewCcdResponse().getCaseReference());
                }
                if (responseWrapper.getNewCcdResponse().getSubscriptions().getAppellantSubscription().isSmsSubscribed() && notification.isSms() && notification.getSmsTemplate() != null) {
                    LOG.info("Sending SMS for case reference " + responseWrapper.getNewCcdResponse().getCaseReference());
                    client.sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference());
                    LOG.info("SMS sent for case reference " + responseWrapper.getNewCcdResponse().getCaseReference());
                }
                createReminders(responseWrapper.getNewCcdResponse());
            } catch (Exception ex) {
                if (ex.getCause() instanceof UnknownHostException) {
                    NotificationClientRuntimeException exception = new NotificationClientRuntimeException(ex);
                    LOG.error("Runtime error on GovUKNotify for case reference " + responseWrapper.getNewCcdResponse().getCaseReference(), exception);
                    throw exception;
                } else {
                    NotificationServiceException exception = new NotificationServiceException(ex);
                    LOG.error("Error on GovUKNotify for case reference " + responseWrapper.getNewCcdResponse().getCaseReference(), exception);
                    throw exception;
                }
            }
        }
    }

    public void createReminders(CcdResponse ccdResponse) {
        if (ccdResponse.getNotificationType().isScheduleReminder()) {
            reminderService.createJob(ccdResponse);
        }
    }
}
