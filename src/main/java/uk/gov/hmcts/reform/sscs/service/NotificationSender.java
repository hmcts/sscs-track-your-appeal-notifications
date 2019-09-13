package uk.gov.hmcts.reform.sscs.service;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.NotificationBlacklist;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.service.notify.*;

@Component
public class NotificationSender {

    private static final Logger LOG = getLogger(NotificationSender.class);
    public static final String USING_TEST_GOV_NOTIFY_KEY_FOR = "Using test GovNotify key {} for {}";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("d MMM Y HH:mm");
    private static final ZoneId ZONE_ID_LONDON = ZoneId.of("Europe/London");

    private final NotificationClient notificationClient;
    private final NotificationClient testNotificationClient;
    private final NotificationBlacklist notificationBlacklist;
    private final CcdNotificationsPdfService ccdNotificationsPdfService;
    private final MarkdownTransformationService markdownTransformationService;
    private final Boolean saveCorrespondence;

    @Autowired
    public NotificationSender(@Qualifier("notificationClient") NotificationClient notificationClient,
                              @Qualifier("testNotificationClient") NotificationClient testNotificationClient,
                              NotificationBlacklist notificationBlacklist,
                              CcdNotificationsPdfService ccdNotificationsPdfService,
                              MarkdownTransformationService markdownTransformationService,
                              @Value("${feature.save_correspondence}") Boolean saveCorrespondence
    ) {
        this.notificationClient = notificationClient;
        this.testNotificationClient = testNotificationClient;
        this.notificationBlacklist = notificationBlacklist;
        this.ccdNotificationsPdfService = ccdNotificationsPdfService;
        this.markdownTransformationService = markdownTransformationService;
        this.saveCorrespondence = saveCorrespondence;
    }

    public void sendEmail(String templateId, String emailAddress, Map<String, String> personalisation, String reference,
                          NotificationEventType notificationEventType,
                          SscsCaseData sscsCaseData) throws NotificationClientException {

        NotificationClient client;

        if (notificationBlacklist.getTestRecipients().contains(emailAddress)
                || emailAddress.matches("test[\\d]+@hmcts.net")) {
            LOG.info(USING_TEST_GOV_NOTIFY_KEY_FOR, testNotificationClient.getApiKey(), emailAddress);
            client = testNotificationClient;
        } else {
            client = notificationClient;
        }

        SendEmailResponse sendEmailResponse = client.sendEmail(templateId, emailAddress, personalisation, reference);

        if (saveCorrespondence) {
            Correspondence correspondence = Correspondence.builder().value(
                    CorrespondenceDetails.builder()
                            .body(markdownTransformationService.toHtml(sendEmailResponse.getBody()))
                            .subject(sendEmailResponse.getSubject())
                            .from(sendEmailResponse.getFromEmail().orElse(""))
                            .to(emailAddress)
                            .eventType(notificationEventType.getId())
                            .correspondenceType(CorrespondenceType.Email)
                            .sentOn(LocalDateTime.now(ZONE_ID_LONDON).format(DATE_TIME_FORMATTER))
                            .build()
            ).build();

            ccdNotificationsPdfService.mergeCorrespondenceIntoCcd(sscsCaseData, correspondence);
            LOG.info("Uploaded email pdf into ccd for case id {}.", sscsCaseData.getCcdCaseId());
        }

        LOG.info("Email Notification send for case id : {}, Gov notify id: {} ", sscsCaseData.getCcdCaseId(),
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
