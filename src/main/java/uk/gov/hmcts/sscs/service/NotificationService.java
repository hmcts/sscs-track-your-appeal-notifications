package uk.gov.hmcts.sscs.service;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.notify.Notification;
import uk.gov.hmcts.sscs.exception.NotificationClientRuntimeException;
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

    public void createAndSendNotification(CcdResponse response) throws Exception {

        Notification notification = factory.create(response);

        try {
            if (notification.isEmail() && isNotBlank(notification.getEmailTemplate())) {
                client.sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference());
            }
            if (notification.isSms() && isNotBlank(notification.getSmsTemplate())) {
                client.sendSms(notification.getSmsTemplate(), notification.getMobile(), notification.getPlaceholders(), notification.getReference());
            }
        } catch (Exception ex) {
            if (ex.getCause() instanceof UnknownHostException) {
                throw new NotificationClientRuntimeException(ex);
            }
            String errorMessage = "Error on GovUKNotify for AppealNumber " +  notification.getAppealNumber() + ", " + ex.getStackTrace();
            LOG.error(errorMessage);
            throw new Exception(errorMessage);
        }
    }
}
