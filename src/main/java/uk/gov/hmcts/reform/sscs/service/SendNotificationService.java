package uk.gov.hmcts.reform.sscs.service;

import static org.apache.commons.lang3.StringUtils.*;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.*;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.service.LetterUtils.*;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.*;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.isMandatoryLetterEventType;
import static uk.gov.hmcts.reform.sscs.service.NotificationValidService.*;

import java.io.IOException;
import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.domain.notify.Notification;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.exception.NotificationServiceException;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.docmosis.PdfLetterService;
import uk.gov.service.notify.NotificationClientException;

@Service
@Slf4j
public class SendNotificationService {
    static final String DM_STORE_USER_ID = "sscs";
    private static final String NOTIFICATION_TYPE_LETTER = "Letter";

    @Value("${reminder.dwpResponseLateReminder.delay.seconds}")
    private long delay;

    private final NotificationSender notificationSender;
    private final EvidenceManagementService evidenceManagementService;
    private final NotificationHandler notificationHandler;
    private final NotificationValidService notificationValidService;
    private final PdfLetterService pdfLetterService;

    @Autowired
    public SendNotificationService(
            NotificationSender notificationSender,
            EvidenceManagementService evidenceManagementService,
            NotificationHandler notificationHandler,
            NotificationValidService notificationValidService,
            PdfLetterService pdfLetterService
    ) {
        this.notificationSender = notificationSender;
        this.evidenceManagementService = evidenceManagementService;
        this.notificationHandler = notificationHandler;
        this.notificationValidService = notificationValidService;
        this.pdfLetterService = pdfLetterService;
    }

    boolean sendEmailSmsLetterNotification(
            NotificationWrapper wrapper,
            Notification notification,
            SubscriptionWithType subscriptionWithType,
            NotificationEventType eventType) {
        boolean emailSent = sendEmailNotification(wrapper, subscriptionWithType.getSubscription(), notification);
        boolean smsSent = sendSmsNotification(wrapper, subscriptionWithType.getSubscription(), notification, eventType);

        boolean isInterlocLetter = INTERLOC_LETTERS.contains(eventType);
        boolean isDocmosisLetter = DOCMOSIS_LETTERS.contains(eventType);

        boolean letterSent = false;
        if (allowNonInterlocLetterToBeSent(notification, isInterlocLetter, wrapper.getSscsCaseDataWrapper().getNewSscsCaseData().getCreatedInGapsFrom())
                || allowInterlocLetterToBeSent(notification, isInterlocLetter)
                || allowDocmosisLetterToBeSent(notification, isDocmosisLetter)) {
            letterSent = sendLetterNotification(wrapper, subscriptionWithType.getSubscription(), notification, subscriptionWithType, eventType);
        }

        boolean notificationSent = emailSent || smsSent || letterSent;

        if (!notificationSent) {
            log.error("Did not send a notification for event {} for case id {}.", eventType.getId(), wrapper.getCaseId());
        }

        return notificationSent;
    }

    private boolean allowDocmosisLetterToBeSent(Notification notification, boolean isDocmosisLetter) {
        return isDocmosisLetter && isNotBlank(notification.getDocmosisLetterTemplate());
    }

    private boolean allowInterlocLetterToBeSent(Notification notification, boolean isInterlocLetter) {
        return isInterlocLetter && isNotBlank(notification.getLetterTemplate());
    }

    private boolean allowNonInterlocLetterToBeSent(Notification notification, boolean isInterlocLetter, String createdInGapsFrom) {
        return !isInterlocLetter && isNotBlank(notification.getLetterTemplate()) && State.READY_TO_LIST.getId().equals(createdInGapsFrom);
    }

    private boolean sendSmsNotification(NotificationWrapper wrapper, Subscription subscription, Notification notification, NotificationEventType eventType) {
        if (isOkToSendSmsNotification(wrapper, subscription, notification, eventType, notificationValidService)) {

            NotificationHandler.SendNotification sendNotification = () ->
                    notificationSender.sendSms(
                            notification.getSmsTemplate(),
                            notification.getMobile(),
                            notification.getPlaceholders(),
                            notification.getReference(),
                            notification.getSmsSenderTemplate(),
                            wrapper.getNotificationType(),
                            wrapper.getNewSscsCaseData()
                    );
            log.info("In sendSmsNotification method notificationSender is available {} ", notificationSender != null);
            return notificationHandler.sendNotification(wrapper, notification.getSmsTemplate(), "SMS", sendNotification);
        }

        return false;
    }

    private boolean sendEmailNotification(NotificationWrapper wrapper, Subscription subscription, Notification notification) {
        if (isOkToSendEmailNotification(wrapper, subscription, notification, notificationValidService)) {

            NotificationHandler.SendNotification sendNotification = () ->
                    notificationSender.sendEmail(
                            notification.getEmailTemplate(),
                            notification.getEmail(),
                            notification.getPlaceholders(),
                            notification.getReference(),
                            wrapper.getNotificationType(),
                            wrapper.getNewSscsCaseData()

                    );
            log.info("In sendEmailNotification method notificationSender is available {} ", notificationSender != null);
            return notificationHandler.sendNotification(wrapper, notification.getEmailTemplate(), "Email", sendNotification);
        }

        return false;
    }

    protected boolean sendLetterNotification(NotificationWrapper wrapper, Subscription subscription, Notification notification, SubscriptionWithType subscriptionWithType, NotificationEventType eventType) {

        log.info("Sending the letter for event {} and case id {}.", eventType.getId(), wrapper.getCaseId());
        Address addressToUse = getAddressToUseForLetter(wrapper, subscriptionWithType.getSubscriptionType());

        if (isValidLetterAddress(addressToUse)) {
            // mandatory letters should always be sent
            // fallback letters are sent only if there's no email or SMS subscription
            boolean mandatoryLetterSent = sendMandatoryLetterNotification(wrapper, notification, subscriptionWithType.getSubscriptionType(), addressToUse);
            boolean fallbackLetterSent = sendFallbackLetterNotification(wrapper, subscription, notification, subscriptionWithType, eventType, addressToUse);

            return mandatoryLetterSent || fallbackLetterSent;
        } else {
            log.error("Failed to send letter for event id: {} for case id: {}, no address present", wrapper.getNotificationType().getId(), wrapper.getCaseId());

            return false;
        }
    }

    private boolean sendMandatoryLetterNotification(NotificationWrapper wrapper, Notification notification, SubscriptionType subscriptionType, Address addressToUse) {
        if (isMandatoryLetterEventType(wrapper)) {
            if (isBundledLetter(wrapper.getNotificationType()) || (isNotBlank(notification.getDocmosisLetterTemplate()))) {
                return sendBundledLetterNotification(wrapper, notification, getNameToUseForLetter(wrapper, subscriptionType), subscriptionType);
            } else if (hasLetterTemplate(notification)) {
                NotificationHandler.SendNotification sendNotification = () ->
                    sendLetterNotificationToAddress(wrapper, notification, addressToUse, subscriptionType);

                return notificationHandler.sendNotification(wrapper, notification.getLetterTemplate(), NOTIFICATION_TYPE_LETTER, sendNotification);
            }
        }

        return false;
    }

    private boolean sendFallbackLetterNotification(NotificationWrapper wrapper, Subscription subscription, Notification notification, SubscriptionWithType subscriptionWithType, NotificationEventType eventType, Address addressToUse) {
        if (hasNoSubscriptions(subscription) && hasLetterTemplate(notification) && isFallbackLetterRequired(wrapper, subscriptionWithType, subscription, eventType, notificationValidService)) {
            if (isBundledLetter(wrapper.getNotificationType()) || (isNotBlank(notification.getDocmosisLetterTemplate()))) {
                return sendBundledLetterNotification(wrapper, notification, getNameToUseForLetter(wrapper, subscriptionWithType.getSubscriptionType()), subscriptionWithType.getSubscriptionType());
            } else {
                NotificationHandler.SendNotification sendNotification = () ->
                    sendLetterNotificationToAddress(wrapper, notification, addressToUse, subscriptionWithType.getSubscriptionType());

                return notificationHandler.sendNotification(wrapper, notification.getLetterTemplate(), NOTIFICATION_TYPE_LETTER, sendNotification);
            }
        }

        return false;
    }

    protected void sendLetterNotificationToAddress(NotificationWrapper wrapper, Notification notification, final Address addressToUse, SubscriptionType subscriptionType) throws NotificationClientException {
        if (addressToUse != null) {
            Map<String, String> placeholders = notification.getPlaceholders();
            String fullNameNoTitle = getNameToUseForLetter(wrapper, subscriptionType);

            placeholders.put(ADDRESS_LINE_1, fullNameNoTitle);
            placeholders.put(ADDRESS_LINE_2, addressToUse.getLine1());
            placeholders.put(ADDRESS_LINE_3, isEmpty(addressToUse.getLine2()) ? " " : addressToUse.getLine2());
            placeholders.put(ADDRESS_LINE_4, addressToUse.getTown() == null ? " " : addressToUse.getTown());
            placeholders.put(ADDRESS_LINE_5, addressToUse.getCounty() == null ? " " : addressToUse.getCounty());
            placeholders.put(POSTCODE_LITERAL, addressToUse.getPostcode());

            placeholders.put(NAME, fullNameNoTitle);
            if (SubscriptionType.REPRESENTATIVE.equals(subscriptionType)) {
                placeholders.put(REPRESENTATIVE_NAME, fullNameNoTitle);
                placeholders.put(APPELLANT_NAME, wrapper.getNewSscsCaseData().getAppeal().getAppellant().getName().getFullNameNoTitle());
            }

            placeholders.put(CLAIMANT_NAME, wrapper.getNewSscsCaseData().getAppeal().getAppellant().getName().getFullNameNoTitle());


            if (!placeholders.containsKey(APPEAL_RESPOND_DATE)) {
                ZonedDateTime appealReceivedDate = ZonedDateTime.now().plusSeconds(delay);
                placeholders.put(APPEAL_RESPOND_DATE, appealReceivedDate.format(DateTimeFormatter.ofPattern(RESPONSE_DATE_FORMAT)));
            }

            log.info("In sendLetterNotificationToAddress method notificationSender is available {} ", notificationSender != null);

            notificationSender.sendLetter(
                    notification.getLetterTemplate(),
                    addressToUse,
                    notification.getPlaceholders(),
                    wrapper.getNotificationType(),
                    fullNameNoTitle,
                    wrapper.getCaseId()
            );
        }
    }

    private static boolean isValidLetterAddress(Address addressToUse) {
        return null != addressToUse
                && isNotBlank(addressToUse.getLine1())
                && isNotBlank(addressToUse.getPostcode());
    }

    private boolean sendBundledLetterNotification(NotificationWrapper wrapper, Notification notification, String nameToUse, SubscriptionType subscriptionType) {
        try {
            byte[] bundledLetter;
            if (isNotBlank(notification.getDocmosisLetterTemplate())) {
                byte[] letter = pdfLetterService.generateLetter(wrapper, notification, subscriptionType);
                final byte[] associatedCasePdf = downloadAssociatedCasePdf(wrapper);
                if (ArrayUtils.isNotEmpty(associatedCasePdf)) {
                    letter = buildBundledLetter(addBlankPageAtTheEndIfOddPage(letter), associatedCasePdf);
                }
                byte[] coversheet = pdfLetterService.buildCoversheet(wrapper, subscriptionType);
                if (ArrayUtils.isNotEmpty(coversheet)) {
                    letter = buildBundledLetter(addBlankPageAtTheEndIfOddPage(letter), coversheet);
                }
                bundledLetter = letter;

                NotificationHandler.SendNotification sendNotification = () ->
                        notificationSender.sendBundledLetter(
                                wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAddress().getPostcode(),   // Used for whitelisting only
                                bundledLetter,
                                wrapper.getNotificationType(),
                                nameToUse,
                                wrapper.getCaseId()
                        );
                log.info("In sendBundledLetterNotification method notificationSender is available {} ", notificationSender != null);
                if (ArrayUtils.isNotEmpty(bundledLetter)) {
                    notificationHandler.sendNotification(wrapper, notification.getLetterTemplate(), NOTIFICATION_TYPE_LETTER, sendNotification);
                    return true;
                }
            }
        } catch (IOException ioe) {
            NotificationServiceException exception = new NotificationServiceException(wrapper.getCaseId(), ioe);
            log.error("Error on GovUKNotify for case id: " + wrapper.getCaseId() + ", sendBundledLetterNotification", exception);
            throw exception;
        }
        return false;
    }

    private byte[] downloadAssociatedCasePdf(NotificationWrapper wrapper) {
        NotificationEventType notificationEventType = wrapper.getSscsCaseDataWrapper().getNotificationEventType();
        SscsCaseData newSscsCaseData = wrapper.getNewSscsCaseData();

        byte[] associatedCasePdf = null;
        String documentUrl = getBundledLetterDocumentUrl(notificationEventType, newSscsCaseData);

        if (null != documentUrl) {

            associatedCasePdf = evidenceManagementService.download(URI.create(documentUrl), DM_STORE_USER_ID);

        }

        return associatedCasePdf;
    }

    protected static String getBundledLetterDocumentUrl(NotificationEventType notificationEventType, SscsCaseData newSscsCaseData) {
        if (DIRECTION_ISSUED.equals(notificationEventType)) {
            return getDocumentForType(newSscsCaseData.getLatestDocumentForDocumentType(DIRECTION_NOTICE));
        } else if (DECISION_ISSUED.equals(notificationEventType)) {
            return getDocumentForType(newSscsCaseData.getLatestDocumentForDocumentType(DECISION_NOTICE));
        } else if (ISSUE_FINAL_DECISION.equals(notificationEventType)) {
            return getDocumentForType(newSscsCaseData.getLatestDocumentForDocumentType(FINAL_DECISION_NOTICE));
        } else if (ISSUE_ADJOURNMENT.equals(notificationEventType)) {
            return getDocumentForType(newSscsCaseData.getLatestDocumentForDocumentType(ADJOURNMENT_NOTICE));
        }
        return null;
    }

    private static String getDocumentForType(SscsDocument sscsDocument) {
        if (sscsDocument != null) {
            return sscsDocument.getValue().getDocumentLink().getDocumentUrl();
        }
        return null;
    }

}
