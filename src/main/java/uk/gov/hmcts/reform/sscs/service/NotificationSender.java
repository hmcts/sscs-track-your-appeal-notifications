package uk.gov.hmcts.reform.sscs.service;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.ByteArrayInputStream;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.config.NotificationBlacklist;
import uk.gov.service.notify.*;

@Component
public class NotificationSender {

    private static final Logger LOG = getLogger(NotificationSender.class);
    public static final String USING_TEST_GOV_NOTIFY_KEY_FOR = "Using test GovNotify key {} for {}";

    private final NotificationClient notificationClient;
    private final NotificationClient testNotificationClient;
    private final NotificationBlacklist notificationBlacklist;

    @Autowired
    public NotificationSender(@Qualifier("notificationClient") NotificationClient notificationClient,
                              @Qualifier("testNotificationClient") NotificationClient testNotificationClient,
                              NotificationBlacklist notificationBlacklist) {
        this.notificationClient = notificationClient;
        this.testNotificationClient = testNotificationClient;
        this.notificationBlacklist = notificationBlacklist;
    }

    public void sendEmail(String templateId, String emailAddress, Map<String, String> personalisation, String reference,
                          String ccdCaseId) throws NotificationClientException {

        NotificationClient client;

        if (notificationBlacklist.getTestRecipients().contains(emailAddress)
                || emailAddress.matches("test[\\d]+@hmcts.net")) {
            LOG.info(USING_TEST_GOV_NOTIFY_KEY_FOR, testNotificationClient.getApiKey(), emailAddress);
            client = testNotificationClient;
        } else {
            client = notificationClient;
        }

        SendEmailResponse sendEmailResponse = client.sendEmail(templateId, emailAddress, personalisation, reference);

        LOG.info("Email Notification send for case id : {}, Gov notify id: {} ", ccdCaseId,
                sendEmailResponse.getNotificationId());
    }

    public void sendSms(
            String templateId,
            String phoneNumber,
            Map<String, String> personalisation,
            String reference,
            String smsSender,
            String ccdCaseId
    ) throws NotificationClientException {

        NotificationClient client;

        if (notificationBlacklist.getTestRecipients().contains(phoneNumber)) {
            LOG.info(USING_TEST_GOV_NOTIFY_KEY_FOR, testNotificationClient.getApiKey(), phoneNumber);
            client = testNotificationClient;
        } else {
            client = notificationClient;
        }

        SendSmsResponse sendSmsResponse = client.sendSms(
                templateId,
                phoneNumber,
                personalisation,
                reference,
                smsSender
        );

        LOG.info("Sms Notification send for case id : {}, Gov notify id: {} ", ccdCaseId, sendSmsResponse.getNotificationId());
    }

    public void sendLetter(String templateId, Address address, Map<String, String> personalisation,
                           String ccdCaseId) throws NotificationClientException {

        NotificationClient client = getLetterNotificationClient(address.getPostcode());

        SendLetterResponse sendLetterResponse = client.sendLetter(templateId, personalisation, ccdCaseId);

        LOG.info("Letter Notification send for case id : {}, Gov notify id: {} ", ccdCaseId,
            sendLetterResponse.getNotificationId());
    }

    public void sendBundledLetter(String appellantPostcode, byte[] directionText, String ccdCaseId) throws NotificationClientException {
        if (directionText != null) {
            NotificationClient client = getLetterNotificationClient(appellantPostcode);

            ByteArrayInputStream bis = new ByteArrayInputStream(directionText);

            LetterResponse sendLetterResponse = client.sendPrecompiledLetterWithInputStream(ccdCaseId, bis);

            LOG.info("Letter Notification send for case id : {}, Gov notify id: {} ", ccdCaseId,
                sendLetterResponse.getNotificationId());
        }
    }

    private NotificationClient getLetterNotificationClient(String postcode) {
        NotificationClient client;
        if (notificationBlacklist.getTestRecipients().contains(postcode)) {
            LOG.info(USING_TEST_GOV_NOTIFY_KEY_FOR, testNotificationClient.getApiKey(), postcode);
            client = testNotificationClient;
        } else {
            client = notificationClient;
        }
        return client;
    }
}
