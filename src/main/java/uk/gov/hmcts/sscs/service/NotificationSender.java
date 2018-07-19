package uk.gov.hmcts.sscs.service;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.sscs.config.NotificationBlacklist;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

@Component
public class NotificationSender {

    private static final Logger LOG = getLogger(NotificationSender.class);

    private final NotificationClient notificationClient;
    private final NotificationClient testNotificationClient;
    private final NotificationBlacklist notificationBlacklist;

    @Value("${gov.uk.notification.api.key}")
    private String apiKey;

    @Value("${gov.uk.notification.api.testKey}")
    private String testApiKey;

    @Autowired
    public NotificationSender(
        @Qualifier("notificationClient") NotificationClient notificationClient,
        @Qualifier("testNotificationClient") NotificationClient testNotificationClient,
        NotificationBlacklist notificationBlacklist
    ) {
        this.notificationClient = notificationClient;
        this.testNotificationClient = testNotificationClient;
        this.notificationBlacklist = notificationBlacklist;
    }

    public void sendEmail(
        String templateId,
        String emailAddress,
        Map<String, String> personalisation,
        String reference
    ) throws NotificationClientException {

        if (notificationBlacklist.getTestRecipients().contains(emailAddress)) {
            LOG.info("Using test GovNotify key {} for {}", testApiKey, emailAddress);
            (new NotificationClient(testApiKey)).sendEmail(templateId, emailAddress, personalisation, reference);
        } else {
            LOG.info("Using real GovNotify key {} for {}", apiKey, emailAddress);
            (new NotificationClient(apiKey)).sendEmail(templateId, emailAddress, personalisation, reference);
        }
    }

    public void sendSms(
        String templateId,
        String phoneNumber,
        Map<String, String> personalisation,
        String reference
    ) throws NotificationClientException {

        if (notificationBlacklist.getTestRecipients().contains(phoneNumber)) {
            LOG.info("Using test GovNotify key {} for {}", testApiKey, phoneNumber);
            (new NotificationClient(testApiKey)).sendSms(templateId, phoneNumber, personalisation, reference);
        } else {
            LOG.info("Using real GovNotify key {} for {}", apiKey, phoneNumber);
            (new NotificationClient(apiKey)).sendSms(templateId, phoneNumber, personalisation, reference);
        }
    }
}
