package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.config.AppConstants.*;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPOINTEE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.STRUCK_OUT;
import static uk.gov.hmcts.reform.sscs.service.LetterUtils.*;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.isOkToSendEmailNotification;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.isOkToSendSmsNotification;
import static uk.gov.hmcts.reform.sscs.service.NotificationValidService.isBundledLetter;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.AppConstants;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.domain.notify.Notification;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.exception.NotificationServiceException;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.service.notify.NotificationClientException;

@Service
@Slf4j
public class SendNotificationService {
    private static final String DIRECTION_TEXT = "Direction Text";
    static final String DM_STORE_USER_ID = "sscs";
    private static final String NOTIFICATION_TYPE_LETTER = "Letter";

    @Value("${feature.bundled_letters_on}")
    Boolean bundledLettersOn;

    @Value("${feature.letters_on}")
    Boolean lettersOn;

    private final NotificationSender notificationSender;
    private final EvidenceManagementService evidenceManagementService;
    private final SscsGeneratePdfService sscsGeneratePdfService;
    private final NotificationHandler notificationHandler;
    private final NotificationValidService notificationValidService;

    @Value("${noncompliantcaseletter.appeal.html.template.path}")
    String noncompliantcaseletterTemplate;

    @Autowired
    public SendNotificationService(
            NotificationSender notificationSender,
            EvidenceManagementService evidenceManagementService,
            SscsGeneratePdfService sscsGeneratePdfService,
            NotificationHandler notificationHandler,
            NotificationValidService notificationValidService
    ) {
        this.notificationSender = notificationSender;
        this.evidenceManagementService = evidenceManagementService;
        this.sscsGeneratePdfService = sscsGeneratePdfService;
        this.notificationHandler = notificationHandler;
        this.notificationValidService = notificationValidService;
    }

    void sendEmailSmsLetterNotification(
            NotificationWrapper wrapper,
            Subscription subscription,
            Notification notification,
            SubscriptionWithType subscriptionWithType) {
        sendEmailNotification(wrapper, subscription, notification);
        sendSmsNotification(wrapper, subscription, notification);

        if (lettersOn) {
            if (APPELLANT.equals(subscriptionWithType.getSubscriptionType())
                || APPOINTEE.equals(subscriptionWithType.getSubscriptionType())) {
                sendFallbackLetterNotificationToAppellant(wrapper, subscription, notification);
            } else {
                sendFallbackLetterNotificationToRepresentative(wrapper, subscription, notification);
            }
        }

        if (bundledLettersOn && isBundledLetter(wrapper.getNotificationType())) {
            sendBundledLetterNotificationToAppellant(wrapper, notification);
            sendBundledLetterNotificationToRepresentative(wrapper, notification);
        }
    }

    private void sendSmsNotification(NotificationWrapper wrapper, Subscription subscription, Notification notification) {
        if (isOkToSendSmsNotification(wrapper, subscription, notification, notificationValidService)) {
            NotificationHandler.SendNotification sendNotification = () ->
                    notificationSender.sendSms(
                            notification.getSmsTemplate(),
                            notification.getMobile(),
                            notification.getPlaceholders(),
                            notification.getReference(),
                            notification.getSmsSenderTemplate(),
                            wrapper.getCaseId()
                    );
            notificationHandler.sendNotification(wrapper, notification.getSmsTemplate(), "SMS", sendNotification);
        }
    }

    private void sendEmailNotification(NotificationWrapper wrapper, Subscription subscription, Notification notification) {
        if (isOkToSendEmailNotification(wrapper, subscription, notification, notificationValidService)) {
            NotificationHandler.SendNotification sendNotification = () ->
                    notificationSender.sendEmail(
                            notification.getEmailTemplate(),
                            notification.getEmail(),
                            notification.getPlaceholders(),
                            notification.getReference(),
                            wrapper.getCaseId()
                    );
            notificationHandler.sendNotification(wrapper, notification.getEmailTemplate(), "Email", sendNotification);
        }
    }

    private void sendFallbackLetterNotificationToAppellant(NotificationWrapper wrapper, Subscription subscription, Notification notification) {
        if (subscription != null && !subscription.isSmsSubscribed() && !subscription.isEmailSubscribed() && notification.getLetterTemplate() != null) {
            NotificationHandler.SendNotification sendNotification = () -> {
                Address addressToUse = getAddressToUseForLetter(wrapper);

                sendLetterNotification(wrapper, notification, addressToUse);
            };
            notificationHandler.sendNotification(wrapper, notification.getLetterTemplate(), NOTIFICATION_TYPE_LETTER, sendNotification);
        }
    }

    private void sendFallbackLetterNotificationToRepresentative(NotificationWrapper wrapper, Subscription subscription, Notification notification) {
        if (subscription != null && !subscription.isSmsSubscribed() && !subscription.isEmailSubscribed() && notification.getLetterTemplate() != null) {
            NotificationHandler.SendNotification sendNotification = () -> {
                Address addressToUse = wrapper.getNewSscsCaseData().getAppeal().getRep().getAddress();

                sendLetterNotification(wrapper, notification, addressToUse);
            };
            notificationHandler.sendNotification(wrapper, notification.getLetterTemplate(), NOTIFICATION_TYPE_LETTER, sendNotification);
        }
    }

    protected void sendLetterNotification(NotificationWrapper wrapper, Notification notification, final Address addressToUse) throws NotificationClientException {
        Map<String, String> placeholders = notification.getPlaceholders();
        placeholders.put(ADDRESS_LINE_1, addressToUse.getLine1() == null ? " " : addressToUse.getLine1());
        placeholders.put(ADDRESS_LINE_2, addressToUse.getLine2() == null ? " " : addressToUse.getLine2());
        placeholders.put(ADDRESS_LINE_3, addressToUse.getTown() == null ? " " : addressToUse.getTown());
        placeholders.put(ADDRESS_LINE_4, addressToUse.getCounty() == null ? " " : addressToUse.getCounty());
        placeholders.put(POSTCODE_LITERAL, addressToUse.getPostcode());
        if (null != wrapper.getNewSscsCaseData().getAppeal().getRep()) {
            placeholders.put(REPRESENTATIVE_NAME, wrapper.getNewSscsCaseData().getAppeal().getRep().getName().getFullNameNoTitle());
        }

        notificationSender.sendLetter(
            notification.getLetterTemplate(),
            addressToUse,
            notification.getPlaceholders(),
            wrapper.getCaseId()
        );
    }

    private void sendBundledLetterNotificationToAppellant(NotificationWrapper wrapper, Notification notification) {
        if (notification.getLetterTemplate() != null) {
            sendBundledLetterNotification(wrapper, notification, getAddressToUseForLetter(wrapper), getNameToUseForLetter(wrapper));
        }
    }

    private void sendBundledLetterNotificationToRepresentative(NotificationWrapper wrapper, Notification notification) {
        if ((notification.getLetterTemplate() != null) && (null != wrapper.getNewSscsCaseData().getAppeal().getRep())) {
            sendBundledLetterNotification(wrapper, notification, wrapper.getNewSscsCaseData().getAppeal().getRep().getAddress(), wrapper.getNewSscsCaseData().getAppeal().getRep().getName());
        }
    }

    private void sendBundledLetterNotification(NotificationWrapper wrapper, Notification notification, Address addressToUse, Name nameToUse) {
        try {
            notification.getPlaceholders().put(AppConstants.LETTER_ADDRESS_LINE_1, addressToUse.getLine1());
            notification.getPlaceholders().put(AppConstants.LETTER_ADDRESS_LINE_2, addressToUse.getLine2());
            notification.getPlaceholders().put(AppConstants.LETTER_ADDRESS_LINE_3, addressToUse.getTown());
            notification.getPlaceholders().put(AppConstants.LETTER_ADDRESS_LINE_4, addressToUse.getCounty());
            notification.getPlaceholders().put(AppConstants.LETTER_ADDRESS_POSTCODE, addressToUse.getPostcode());
            notification.getPlaceholders().put(AppConstants.LETTER_NAME, nameToUse.getFullNameNoTitle());

            byte[] bundledLetter = buildBundledLetter(
                    generateCoveringLetter(wrapper, notification),
                    downloadAssociatedCasePdf(wrapper)
            );

            NotificationHandler.SendNotification sendNotification = () ->
                    notificationSender.sendBundledLetter(
                            wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAddress().getPostcode(),   // Used for whitelisting only
                            bundledLetter,
                            wrapper.getCaseId()
                    );
            notificationHandler.sendNotification(wrapper, notification.getLetterTemplate(), NOTIFICATION_TYPE_LETTER, sendNotification);
        } catch (IOException ioe) {
            NotificationServiceException exception = new NotificationServiceException(wrapper.getCaseId(), ioe);
            log.error("Error on GovUKNotify for case id: " + wrapper.getCaseId() + ", sendBundledLetterNotification", exception);
            throw exception;
        }
    }

    private byte[] generateCoveringLetter(NotificationWrapper wrapper, Notification notification) {
        return sscsGeneratePdfService.generatePdf(noncompliantcaseletterTemplate, wrapper.getNewSscsCaseData(),
                Long.parseLong(wrapper.getNewSscsCaseData().getCcdCaseId()), notification.getPlaceholders());
    }

    private byte[] downloadAssociatedCasePdf(NotificationWrapper wrapper) {
        NotificationEventType notificationEventType = wrapper.getSscsCaseDataWrapper().getNotificationEventType();
        SscsCaseData newSscsCaseData = wrapper.getNewSscsCaseData();

        byte[] associatedCasePdf = null;
        String filetype = null;
        if ((STRUCK_OUT.equals(notificationEventType))
                && (newSscsCaseData.getSscsDocument() != null
                && !newSscsCaseData.getSscsDocument().isEmpty())) {
            filetype = DIRECTION_TEXT;
        }

        if (null != filetype) {
            for (SscsDocument sscsDocument : newSscsCaseData.getSscsDocument()) {
                if (filetype.equalsIgnoreCase(sscsDocument.getValue().getDocumentType())) {
                    associatedCasePdf = evidenceManagementService.download(
                        URI.create(sscsDocument.getValue().getDocumentLink().getDocumentUrl()),
                        DM_STORE_USER_ID
                    );

                    break;
                }
            }
        }

        return associatedCasePdf;
    }
}
