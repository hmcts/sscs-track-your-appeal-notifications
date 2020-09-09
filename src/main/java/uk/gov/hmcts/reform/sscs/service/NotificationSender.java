package uk.gov.hmcts.reform.sscs.service;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Correspondence;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrespondenceDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrespondenceType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.config.NotificationBlacklist;
import uk.gov.hmcts.reform.sscs.domain.NotifyResponse;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.service.notify.LetterResponse;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendLetterResponse;
import uk.gov.service.notify.SendSmsResponse;

@Component
@Slf4j
public class NotificationSender {

    private static final String USING_TEST_GOV_NOTIFY_KEY_FOR = "Using test GovNotify key {} for {}";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("d MMM Y HH:mm");
    private static final ZoneId ZONE_ID_LONDON = ZoneId.of("Europe/London");

    private final NotificationClient notificationClient;
    private final NotificationClient testNotificationClient;
    private final NotificationBlacklist notificationBlacklist;
    private final CcdNotificationsPdfService ccdNotificationsPdfService;
    private final MarkdownTransformationService markdownTransformationService;
    private final SaveLetterCorrespondenceAsyncService saveLetterCorrespondenceAsyncService;
    private final Boolean saveCorrespondence;

    @Autowired
    public NotificationSender(@Qualifier("notificationClient") NotificationClient notificationClient,
                              @Qualifier("testNotificationClient") NotificationClient testNotificationClient,
                              NotificationBlacklist notificationBlacklist,
                              CcdNotificationsPdfService ccdNotificationsPdfService,
                              MarkdownTransformationService markdownTransformationService,
                              SaveLetterCorrespondenceAsyncService saveLetterCorrespondenceAsyncService,
                              @Value("${feature.save_correspondence}") Boolean saveCorrespondence
    ) {
        this.notificationClient = notificationClient;
        this.testNotificationClient = testNotificationClient;
        this.notificationBlacklist = notificationBlacklist;
        this.ccdNotificationsPdfService = ccdNotificationsPdfService;
        this.markdownTransformationService = markdownTransformationService;
        this.saveCorrespondence = saveCorrespondence;
        this.saveLetterCorrespondenceAsyncService = saveLetterCorrespondenceAsyncService;
    }

    public void sendEmail(String templateId, String emailAddress, Map<String, String> personalisation, String reference,
                          NotificationEventType notificationEventType,
                          SscsCaseData sscsCaseData) throws NotificationClientException {

        NotificationClient client;

        if (notificationBlacklist.getTestRecipients().contains(emailAddress)
                || emailAddress.matches("test[\\d]+@hmcts.net")) {
            log.info(USING_TEST_GOV_NOTIFY_KEY_FOR, testNotificationClient.getApiKey(), emailAddress);
            client = testNotificationClient;
        } else {
            client = notificationClient;
        }

        final SendEmailResponse sendEmailResponse;
        try {
            sendEmailResponse = client.sendEmail(templateId, emailAddress, personalisation, reference);
        } catch (NotificationClientException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationClientException(e);
        }

        if (saveCorrespondence) {
            NotifyResponse response = new NotifyResponse(
                    sendEmailResponse.getBody(),
                    sendEmailResponse.getSubject(),
                    sendEmailResponse.getFromEmail(),
                    emailAddress);

            saveCorrespondence(response, notificationEventType, sscsCaseData, CorrespondenceType.Email);
        }

        log.info("Email Notification send for case id : {}, Gov notify id: {} ", sscsCaseData.getCcdCaseId(),
                sendEmailResponse.getNotificationId());
    }

    public void sendSms(
            String templateId,
            String phoneNumber,
            Map<String, String> personalisation,
            String reference,
            String smsSender,
            NotificationEventType notificationEventType,
            SscsCaseData sscsCaseData
    ) throws NotificationClientException {

        NotificationClient client;

        if (notificationBlacklist.getTestRecipients().contains(phoneNumber)) {
            log.info(USING_TEST_GOV_NOTIFY_KEY_FOR, testNotificationClient.getApiKey(), phoneNumber);
            client = testNotificationClient;
        } else {
            client = notificationClient;
        }

        final SendSmsResponse sendSmsResponse;
        try {
            sendSmsResponse = client.sendSms(
                    templateId,
                    phoneNumber,
                    personalisation,
                    reference,
                    smsSender
            );
        } catch (NotificationClientException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationClientException(e);
        }

        if (saveCorrespondence) {
            NotifyResponse response = new NotifyResponse(
                    sendSmsResponse.getBody(),
                    "SMS correspondence",
                    sendSmsResponse.getFromNumber(),
                    phoneNumber);

            saveCorrespondence(response, notificationEventType, sscsCaseData, CorrespondenceType.Sms);
        }

        log.info("Sms Notification send for case id : {}, Gov notify id: {} ", sscsCaseData.getCcdCaseId(),
                sendSmsResponse.getNotificationId());
    }

    public void sendLetter(String templateId, Address address, Map<String, String> personalisation,
                           NotificationEventType notificationEventType,
                           String name, String ccdCaseId) throws NotificationClientException {

        NotificationClient client = getLetterNotificationClient(address.getPostcode());

        final SendLetterResponse sendLetterResponse;
        try {
            sendLetterResponse = client.sendLetter(templateId, personalisation, ccdCaseId);
        } catch (NotificationClientException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationClientException(e);
        }

        if (saveCorrespondence) {
            final Correspondence correspondence = getLetterCorrespondence(notificationEventType, name);
            saveLetterCorrespondenceAsyncService.saveLetter(client, sendLetterResponse.getNotificationId().toString(), correspondence, ccdCaseId);
        }

        log.info("Letter Notification send for case id : {}, Gov notify id: {} ", ccdCaseId, sendLetterResponse.getNotificationId());
    }

    private void saveCorrespondence(NotifyResponse response, NotificationEventType notificationEventType,
                                    SscsCaseData sscsCaseData, CorrespondenceType correspondenceType) {
        Correspondence correspondence = Correspondence.builder().value(
                CorrespondenceDetails.builder()
                        .body(markdownTransformationService.toHtml(response.getBody()))
                        .subject(response.getSubject())
                        .from(response.getFrom().orElse(""))
                        .to(response.getTo())
                        .eventType(notificationEventType.getId())
                        .correspondenceType(correspondenceType)
                        .sentOn(LocalDateTime.now(ZONE_ID_LONDON).format(DATE_TIME_FORMATTER))
                        .build()
        ).build();

        ccdNotificationsPdfService.mergeCorrespondenceIntoCcd(sscsCaseData, correspondence);
        log.info("Uploaded correspondence into ccd for case id {}.", sscsCaseData.getCcdCaseId());
    }

    private Correspondence getLetterCorrespondence(NotificationEventType notificationEventType, String name) {
        return Correspondence.builder().value(
                CorrespondenceDetails.builder()
                        .eventType(notificationEventType.getId())
                        .to(name)
                        .correspondenceType(CorrespondenceType.Letter)
                        .sentOn(LocalDateTime.now(ZONE_ID_LONDON).format(DATE_TIME_FORMATTER))
                        .build()
        ).build();
    }

    public void sendBundledLetter(String appellantPostcode, byte[] directionText, NotificationEventType notificationEventType, String name, String ccdCaseId) throws NotificationClientException {
        if (directionText != null) {
            NotificationClient client = getLetterNotificationClient(appellantPostcode);

            ByteArrayInputStream bis = new ByteArrayInputStream(directionText);

            final LetterResponse sendLetterResponse;
            try {
                log.info("Postcode = " + appellantPostcode);
                sendLetterResponse = client.sendPrecompiledLetterWithInputStream(ccdCaseId, bis);
            } catch (NotificationClientException e) {
                throw e;
            } catch (Exception e) {
                throw new NotificationClientException(e);
            }

            if (saveCorrespondence) {
                final Correspondence correspondence = getLetterCorrespondence(notificationEventType, name);
                saveLetterCorrespondenceAsyncService.saveLetter(client, sendLetterResponse.getNotificationId().toString(), correspondence, ccdCaseId);
            }

            log.info("Letter Notification send for case id : {}, Gov notify id: {} ", ccdCaseId, sendLetterResponse.getNotificationId());
        }
    }

    private NotificationClient getLetterNotificationClient(String postcode) {
        NotificationClient client;
        if (notificationBlacklist.getTestRecipients().contains(postcode)) {
            log.info(USING_TEST_GOV_NOTIFY_KEY_FOR, testNotificationClient.getApiKey(), postcode);
            client = testNotificationClient;
        } else {
            client = notificationClient;
        }
        return client;
    }
}
