package uk.gov.hmcts.sscs.service;

import static org.slf4j.LoggerFactory.getLogger;

import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

    @Autowired
    public NotificationService(NotificationClient client, NotificationFactory factory) {
        this.factory = factory;
        this.client = client;
    }

    public void createAndSendNotification(CcdResponseWrapper responseWrapper) throws NotificationServiceException {
        LOG.info("Start to create notification for case reference "  + responseWrapper.getNewCcdResponse().getCaseReference());

        Notification notification = factory.create(responseWrapper);

        try {
            if (responseWrapper.getNewCcdResponse().getAppellantSubscription().isSubscribeEmail() && notification.isEmail() && notification.getEmailTemplate() != null) {
                LOG.info("Sending email for case reference "  + responseWrapper.getNewCcdResponse().getCaseReference());
                client.sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
                LOG.info("Email sent for case reference "  + responseWrapper.getNewCcdResponse().getCaseReference());
            }
            if (responseWrapper.getNewCcdResponse().getAppellantSubscription().isSubscribeSms() && notification.isSms() && notification.getSmsTemplate() != null) {
                LOG.info("Sending SMS for case reference "  + responseWrapper.getNewCcdResponse().getCaseReference());
                client.sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference());
                LOG.info("SMS sent for case reference "  + responseWrapper.getNewCcdResponse().getCaseReference());
            }
        } catch (Exception ex) {
            LOG.error("Error on GovUKNotify for case reference " + responseWrapper.getNewCcdResponse().getCaseReference() + ", " + ex.getStackTrace());
            if (ex.getCause() instanceof UnknownHostException) {
                throw new NotificationClientRuntimeException(ex);
            }
            throw new NotificationServiceException(ex);
        }
    }
}
