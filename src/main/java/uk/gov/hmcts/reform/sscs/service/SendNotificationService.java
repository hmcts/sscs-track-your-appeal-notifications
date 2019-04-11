package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.config.AppConstants.*;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_LODGED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DIRECTION_ISSUED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.STRUCK_OUT;
import static uk.gov.hmcts.reform.sscs.service.LetterUtils.*;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.*;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.hasAppointee;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.isOkToSendEmailNotification;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.isOkToSendSmsNotification;
import static uk.gov.hmcts.reform.sscs.service.NotificationValidService.isBundledLetter;

import java.io.IOException;
import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.AppConstants;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.domain.notify.Notification;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.exception.NotificationServiceException;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.service.notify.NotificationClientException;

@Service
@Slf4j
public class SendNotificationService {
    protected static final String STRIKE_OUT_NOTICE = "Strike Out Notice";
    protected static final String DIRECTION_TEXT = "Direction Text";
    static final String DM_STORE_USER_ID = "sscs";
    private static final String NOTIFICATION_TYPE_LETTER = "Letter";

    @Value("${feature.bundled_letters_on}")
    Boolean bundledLettersOn;

    @Value("${feature.letters_on}")
    Boolean lettersOn;

    @Value("${reminder.dwpResponseLateReminder.delay.seconds}")
    long delay;

    private final NotificationSender notificationSender;
    private final EvidenceManagementService evidenceManagementService;
    private final SscsGeneratePdfService sscsGeneratePdfService;
    private final NotificationHandler notificationHandler;
    private final NotificationValidService notificationValidService;

    @Value("${strikeOutLetterTemplate.appeal.html.template.path}")
    String strikeOutLetterTemplate;

    @Value("${directionNoticeLetterTemplate.appeal.html.template.path}")
    String directionNoticeLetterTemplate;

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
            Notification notification,
            SubscriptionWithType subscriptionWithType,
            NotificationEventType eventType) {
        sendEmailNotification(wrapper, subscriptionWithType.getSubscription(), notification);
        sendSmsNotification(wrapper, subscriptionWithType.getSubscription(), notification, eventType);

        if (lettersOn) {
            sendLetterNotification(wrapper, subscriptionWithType.getSubscription(), notification, subscriptionWithType, eventType);
        }
    }

    private void sendSmsNotification(NotificationWrapper wrapper, Subscription subscription, Notification notification, NotificationEventType eventType) {
        if (isOkToSendSmsNotification(wrapper, subscription, notification, eventType, notificationValidService)) {
            if (APPEAL_LODGED.equals(wrapper.getNotificationType())) {
                ZonedDateTime appealReceivedDate = ZonedDateTime.now().plusSeconds(delay);
                notification.getPlaceholders().put(APPEAL_RESPOND_DATE, appealReceivedDate.format(DateTimeFormatter.ofPattern(RESPONSE_DATE_FORMAT)));
            }

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
            if (APPEAL_LODGED.equals(wrapper.getNotificationType())) {
                ZonedDateTime appealReceivedDate = ZonedDateTime.now().plusSeconds(delay);
                notification.getPlaceholders().put(APPEAL_RESPOND_DATE, appealReceivedDate.format(DateTimeFormatter.ofPattern(RESPONSE_DATE_FORMAT)));
            }

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

    private void sendLetterNotification(NotificationWrapper wrapper, Subscription subscription, Notification notification, SubscriptionWithType subscriptionWithType, NotificationEventType eventType) {
        sendMandatoryLetterNotification(wrapper, notification, subscriptionWithType.getSubscriptionType());
        sendFallbackLetterNotification(wrapper, subscription, notification, subscriptionWithType, eventType);
    }

    private void sendMandatoryLetterNotification(NotificationWrapper wrapper, Notification notification, SubscriptionType subscriptionType) {
        if (isMandatoryLetterEventType(wrapper)) {
            NotificationHandler.SendNotification sendNotification = () -> {
                Address addressToUse = getAddressToUseForLetter(wrapper, subscriptionType);

                sendLetterNotificationToAddress(wrapper, notification, addressToUse);
            };

            if (bundledLettersOn && isBundledLetter(wrapper.getNotificationType())) {
                sendBundledLetterNotification(wrapper, notification, getAddressToUseForLetter(wrapper, subscriptionType), getNameToUseForLetter(wrapper, subscriptionType));
            } else if (hasLetterTemplate(notification)) {
                notificationHandler.sendNotification(wrapper, notification.getLetterTemplate(), NOTIFICATION_TYPE_LETTER, sendNotification);
            }
        }
    }

    private void sendFallbackLetterNotification(NotificationWrapper wrapper, Subscription subscription, Notification notification, SubscriptionWithType subscriptionWithType, NotificationEventType eventType) {
        if (hasNoSubscriptions(subscription) && hasLetterTemplate(notification) && isFallbackLetterRequired(wrapper, subscriptionWithType, subscription, eventType, notificationValidService)) {
            NotificationHandler.SendNotification sendNotification = () -> {
                Address addressToUse = getAddressToUseForLetter(wrapper, subscriptionWithType.getSubscriptionType());

                sendLetterNotificationToAddress(wrapper, notification, addressToUse);
            };

            if (bundledLettersOn && isBundledLetter(wrapper.getNotificationType())) {
                sendBundledLetterNotification(wrapper, notification, getAddressToUseForLetter(wrapper, subscriptionWithType.getSubscriptionType()), getNameToUseForLetter(wrapper, subscriptionWithType.getSubscriptionType()));
            } else {
                notificationHandler.sendNotification(wrapper, notification.getLetterTemplate(), NOTIFICATION_TYPE_LETTER, sendNotification);
            }
        }
    }

    public static String getRepSalutation(Representative rep) {
        if (null == rep.getName()
            || null == rep.getName().getFirstName()
            || null == rep.getName().getLastName()) {
            return REP_SALUTATION;
        } else {
            return rep.getName().getFullNameNoTitle();
        }
    }

    protected void sendLetterNotificationToAddress(NotificationWrapper wrapper, Notification notification, final Address addressToUse) throws NotificationClientException {
        if (isValidLetterAddress(addressToUse)) {
            Map<String, String> placeholders = notification.getPlaceholders();
            placeholders.put(ADDRESS_LINE_1, addressToUse.getLine1());
            placeholders.put(ADDRESS_LINE_2, addressToUse.getLine2() == null ? " " : addressToUse.getLine2());
            placeholders.put(ADDRESS_LINE_3, addressToUse.getTown() == null ? " " : addressToUse.getTown());
            placeholders.put(ADDRESS_LINE_4, addressToUse.getCounty() == null ? " " : addressToUse.getCounty());
            placeholders.put(POSTCODE_LITERAL, addressToUse.getPostcode());
            if (hasRepresentative(wrapper.getSscsCaseDataWrapper())) {
                String repSalutation = getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep());
                placeholders.put(REPRESENTATIVE_NAME, repSalutation);
            }

            if (hasAppointee(wrapper.getSscsCaseDataWrapper())) {
                placeholders.put(APPOINTEE_NAME, wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee().getName().getFullNameNoTitle());
                placeholders.put(CLAIMANT_NAME, wrapper.getNewSscsCaseData().getAppeal().getAppellant().getName().getFullNameNoTitle());
            }

            if (!placeholders.containsKey(APPEAL_RESPOND_DATE)) {
                ZonedDateTime appealReceivedDate = ZonedDateTime.now().plusSeconds(delay);
                placeholders.put(APPEAL_RESPOND_DATE, appealReceivedDate.format(DateTimeFormatter.ofPattern(RESPONSE_DATE_FORMAT)));
            }

            notificationSender.sendLetter(
                notification.getLetterTemplate(),
                addressToUse,
                notification.getPlaceholders(),
                wrapper.getCaseId()
            );
        } else {
            log.warn("Attempting to send letter for case id: " + wrapper.getCaseId() + ", no address present");
        }
    }

    private static boolean isValidLetterAddress(Address addressToUse) {
        return null != addressToUse
            && null != addressToUse.getLine1()
            && null != addressToUse.getPostcode();
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
        return sscsGeneratePdfService.generatePdf(
            getBundledLetterTemplate(wrapper.getNotificationType(), wrapper.getNewSscsCaseData()),
            wrapper.getNewSscsCaseData(),
            Long.parseLong(wrapper.getNewSscsCaseData().getCcdCaseId()), notification.getPlaceholders()
        );
    }

    private byte[] downloadAssociatedCasePdf(NotificationWrapper wrapper) {
        NotificationEventType notificationEventType = wrapper.getSscsCaseDataWrapper().getNotificationEventType();
        SscsCaseData newSscsCaseData = wrapper.getNewSscsCaseData();

        byte[] associatedCasePdf = null;
        String filetype = getBundledLetterFileType(notificationEventType, newSscsCaseData);

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

    protected static String getBundledLetterFileType(NotificationEventType notificationEventType, SscsCaseData newSscsCaseData) {
        String filetype = null;
        if ((STRUCK_OUT.equals(notificationEventType))
                && (newSscsCaseData.getSscsDocument() != null
                && !newSscsCaseData.getSscsDocument().isEmpty())) {
            filetype = STRIKE_OUT_NOTICE;
        } else if ((DIRECTION_ISSUED.equals(notificationEventType))
            && (newSscsCaseData.getSscsDocument() != null
            && !newSscsCaseData.getSscsDocument().isEmpty())) {
            filetype = DIRECTION_TEXT;
        }
        return filetype;
    }

    protected String getBundledLetterTemplate(NotificationEventType notificationEventType, SscsCaseData newSscsCaseData) {
        String bundledLetterTemplate = null;
        if ((STRUCK_OUT.equals(notificationEventType))
            && (newSscsCaseData.getSscsDocument() != null
            && !newSscsCaseData.getSscsDocument().isEmpty())) {
            bundledLetterTemplate = strikeOutLetterTemplate;
        } else if ((DIRECTION_ISSUED.equals(notificationEventType))
            && (newSscsCaseData.getSscsDocument() != null
            && !newSscsCaseData.getSscsDocument().isEmpty())) {
            bundledLetterTemplate = directionNoticeLetterTemplate;
        }
        return bundledLetterTemplate;
    }
}
