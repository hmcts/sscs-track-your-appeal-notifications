package uk.gov.hmcts.sscs.service;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

        NotificationClient client;

        if (notificationBlacklist.getTestRecipients().contains(emailAddress)) {
            LOG.info("Using test GovNotify key for: {}", emailAddress);
            client = testNotificationClient;
        } else {
            client = notificationClient;
        }

        client.sendEmail(
            templateId,
            emailAddress,
            personalisation,
            reference
        );
    }

    public void sendSms(
        String templateId,
        String phoneNumber,
        Map<String, String> personalisation,
        String reference
    ) throws NotificationClientException {

        NotificationClient client;

        if (notificationBlacklist.getTestRecipients().contains(phoneNumber)) {
            LOG.info("Using test GovNotify key for: {}", phoneNumber);
            client = testNotificationClient;
        } else {
            client = notificationClient;
        }

        client.sendSms(
            templateId,
            phoneNumber,
            personalisation,
            reference
        );
    }
}
