package uk.gov.hmcts.reform.sscs.service;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.*;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.service.LetterUtils.*;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.*;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.isOkToSendEmailNotification;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.isOkToSendSmsNotification;
import static uk.gov.hmcts.reform.sscs.service.NotificationValidService.DOCMOSIS_LETTERS;
import static uk.gov.hmcts.reform.sscs.service.NotificationValidService.INTERLOC_LETTERS;
import static uk.gov.hmcts.reform.sscs.service.NotificationValidService.isBundledLetter;

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

    @Value("${feature.bundled_letters_on}")
    Boolean bundledLettersOn;

    @Value("${feature.docmosis_letters_on}")
    Boolean docmosisLettersOn;

    @Value("${feature.letters_on}")
    Boolean lettersOn;

    @Value("${feature.interloc_letters_on}")
    Boolean interlocLettersOn;

    @Value("${reminder.dwpResponseLateReminder.delay.seconds}")
    long delay;

    private final NotificationSender notificationSender;
    private final EvidenceManagementService evidenceManagementService;
    private final SscsGeneratePdfService sscsGeneratePdfService;
    private final NotificationHandler notificationHandler;
    private final NotificationValidService notificationValidService;
    private final BundledLetterTemplateUtil bundledLetterTemplateUtil;
    private final PdfLetterService pdfLetterService;

    @Autowired
    public SendNotificationService(
            NotificationSender notificationSender,
            EvidenceManagementService evidenceManagementService,
            SscsGeneratePdfService sscsGeneratePdfService,
            NotificationHandler notificationHandler,
            NotificationValidService notificationValidService,
            BundledLetterTemplateUtil bundledLetterTemplateUtil,
            PdfLetterService pdfLetterService
    ) {
        this.notificationSender = notificationSender;
        this.evidenceManagementService = evidenceManagementService;
        this.sscsGeneratePdfService = sscsGeneratePdfService;
        this.notificationHandler = notificationHandler;
        this.notificationValidService = notificationValidService;
        this.bundledLetterTemplateUtil = bundledLetterTemplateUtil;
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
        if (allowNonInterlocLetterToBeSent(isInterlocLetter)
                || allowInterlocLetterToBeSent(isInterlocLetter)
                || allowDocmosisLetterToBeSent(notification, isDocmosisLetter)) {
            letterSent = sendLetterNotification(wrapper, subscriptionWithType.getSubscription(), notification, subscriptionWithType, eventType);
        }

        boolean notificationSent = emailSent | smsSent | letterSent;

        if (!notificationSent) {
            log.error("Did not send a notification for event {} for case id {}.", eventType.getId(), wrapper.getCaseId());
        }

        return notificationSent;
    }

    private boolean allowDocmosisLetterToBeSent(Notification notification, boolean isDocmosisLetter) {
        return docmosisLettersOn && isDocmosisLetter && isNotBlank(notification.getDocmosisLetterTemplate());
    }

    private boolean allowInterlocLetterToBeSent(boolean isInterlocLetter) {
        return interlocLettersOn && isInterlocLetter;
    }

    private boolean allowNonInterlocLetterToBeSent(boolean isInterlocLetter) {
        return lettersOn && !isInterlocLetter;
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
                            wrapper.getCaseId()
                    );
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
                            wrapper.getCaseId()
                    );
            return notificationHandler.sendNotification(wrapper, notification.getEmailTemplate(), "Email", sendNotification);
        }

        return false;
    }

    private boolean sendLetterNotification(NotificationWrapper wrapper, Subscription subscription, Notification notification, SubscriptionWithType subscriptionWithType, NotificationEventType eventType) {

        log.info("Sending the letter for event {} and case id {}.", eventType.getId(), wrapper.getCaseId());
        boolean mandatoryLetterSent = sendMandatoryLetterNotification(wrapper, notification, subscriptionWithType.getSubscriptionType());
        boolean fallbackLetterSent = sendFallbackLetterNotification(wrapper, subscription, notification, subscriptionWithType, eventType);

        return mandatoryLetterSent | fallbackLetterSent;
    }

    private boolean sendMandatoryLetterNotification(NotificationWrapper wrapper, Notification notification, SubscriptionType subscriptionType) {
        if (isMandatoryLetterEventType(wrapper)) {
            Address addressToUse = getAddressToUseForLetter(wrapper, subscriptionType);
            if ((bundledLettersOn && isBundledLetter(wrapper.getNotificationType())) || (docmosisLettersOn && isNotBlank(notification.getDocmosisLetterTemplate()) && isDocmosisLetterValidToSend(wrapper))) {
                sendBundledLetterNotification(wrapper, notification, addressToUse, getNameToUseForLetter(wrapper, subscriptionType), subscriptionType);
            } else if (hasLetterTemplate(notification)) {
                NotificationHandler.SendNotification sendNotification = () ->
                    sendLetterNotificationToAddress(wrapper, notification, addressToUse, subscriptionType);

                return notificationHandler.sendNotification(wrapper, notification.getLetterTemplate(), NOTIFICATION_TYPE_LETTER, sendNotification);
            }
        }

        return false;
    }

    private boolean sendFallbackLetterNotification(NotificationWrapper wrapper, Subscription subscription, Notification notification, SubscriptionWithType subscriptionWithType, NotificationEventType eventType) {
        if (hasNoSubscriptions(subscription) && hasLetterTemplate(notification) && isFallbackLetterRequired(wrapper, subscriptionWithType, subscription, eventType, notificationValidService)) {
            Address addressToUse = getAddressToUseForLetter(wrapper, subscriptionWithType.getSubscriptionType());
            if (bundledLettersOn && isBundledLetter(wrapper.getNotificationType()) || (docmosisLettersOn && isNotBlank(notification.getDocmosisLetterTemplate())) && isDocmosisLetterValidToSend(wrapper)) {
                sendBundledLetterNotification(wrapper, notification, getAddressToUseForLetter(wrapper, subscriptionWithType.getSubscriptionType()), getNameToUseForLetter(wrapper, subscriptionWithType.getSubscriptionType()), subscriptionWithType.getSubscriptionType());
            } else {
                NotificationHandler.SendNotification sendNotification = () ->
                    sendLetterNotificationToAddress(wrapper, notification, addressToUse, subscriptionWithType.getSubscriptionType());

                return notificationHandler.sendNotification(wrapper, notification.getLetterTemplate(), NOTIFICATION_TYPE_LETTER, sendNotification);
            }
        }

        return false;
    }

    private boolean isDocmosisLetterValidToSend(NotificationWrapper wrapper) {
        if (NotificationEventType.APPEAL_RECEIVED_NOTIFICATION.equals(wrapper.getNotificationType())) {
            return equalsIgnoreCase(wrapper.getNewSscsCaseData().getAppeal().getBenefitType().getCode(), Benefit.PIP.name())
                    && equalsIgnoreCase(wrapper.getNewSscsCaseData().getAppeal().getReceivedVia(), "Online");
        }
        return true;
    }

    public static String getRepSalutation(Representative rep) {
        if (null == rep.getName() || null == rep.getName().getFirstName() || null == rep.getName().getLastName()) {
            return REP_SALUTATION;
        } else {
            return rep.getName().getFullNameNoTitle();
        }
    }

    public static String getRepSalutation(Name name) {
        if (null == name || null == name.getFirstName() || null == name.getLastName()) {
            return REP_SALUTATION;
        } else {
            return name.getFullNameNoTitle();
        }
    }

    protected void sendLetterNotificationToAddress(NotificationWrapper wrapper, Notification notification, final Address addressToUse, SubscriptionType subscriptionType) throws NotificationClientException {
        if (isValidLetterAddress(addressToUse)) {
            Map<String, String> placeholders = notification.getPlaceholders();
            placeholders.put(ADDRESS_LINE_1, addressToUse.getLine1());
            placeholders.put(ADDRESS_LINE_2, isEmpty(addressToUse.getLine2()) ? " " : addressToUse.getLine2());
            placeholders.put(ADDRESS_LINE_3, addressToUse.getTown() == null ? " " : addressToUse.getTown());
            placeholders.put(ADDRESS_LINE_4, addressToUse.getCounty() == null ? " " : addressToUse.getCounty());
            placeholders.put(POSTCODE_LITERAL, addressToUse.getPostcode());

            Name nameToUse = getNameToUseForLetter(wrapper, subscriptionType);

            String fullNameNoTitle = (nameToUse == null) ? getRepSalutation(nameToUse) : nameToUse.getFullNameNoTitle();

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

            notificationSender.sendLetter(
                    notification.getLetterTemplate(),
                    addressToUse,
                    notification.getPlaceholders(),
                    wrapper.getCaseId()
            );
        } else {
            log.error("Failed to send letter for event id: {} for case id: {}, no address present", wrapper.getNotificationType().getId(), wrapper.getCaseId());
        }
    }

    private static boolean isValidLetterAddress(Address addressToUse) {
        return null != addressToUse
            && isNotBlank(addressToUse.getLine1())
            && isNotBlank(addressToUse.getPostcode());
    }

    private void sendBundledLetterNotification(NotificationWrapper wrapper, Notification notification, Address addressToUse, Name nameToUse, SubscriptionType subscriptionType) {
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
            } else {
                notification.getPlaceholders().put(LETTER_ADDRESS_LINE_1, addressToUse.getLine1());
                notification.getPlaceholders().put(LETTER_ADDRESS_LINE_2, addressToUse.getLine2());
                notification.getPlaceholders().put(LETTER_ADDRESS_LINE_3, addressToUse.getTown());
                notification.getPlaceholders().put(LETTER_ADDRESS_LINE_4, addressToUse.getCounty());
                notification.getPlaceholders().put(LETTER_ADDRESS_POSTCODE, addressToUse.getPostcode());
                notification.getPlaceholders().put(LETTER_NAME, nameToUse.getFullNameNoTitle());
                if (!notification.getPlaceholders().containsKey(APPEAL_RESPOND_DATE)) {
                    ZonedDateTime appealReceivedDate = ZonedDateTime.now().plusSeconds(delay);
                    notification.getPlaceholders().put(APPEAL_RESPOND_DATE, appealReceivedDate.format(DateTimeFormatter.ofPattern(RESPONSE_DATE_FORMAT)));
                }
                bundledLetter = buildBundledLetter(
                        generateCoveringLetter(wrapper, notification, subscriptionType),
                        downloadAssociatedCasePdf(wrapper)
                );
            }

            NotificationHandler.SendNotification sendNotification = () ->
                    notificationSender.sendBundledLetter(
                            wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAddress().getPostcode(),   // Used for whitelisting only
                            bundledLetter,
                            wrapper.getCaseId()
                    );
            if (ArrayUtils.isNotEmpty(bundledLetter)) {
                notificationHandler.sendNotification(wrapper, notification.getLetterTemplate(), NOTIFICATION_TYPE_LETTER, sendNotification);
            }
        } catch (IOException ioe) {
            NotificationServiceException exception = new NotificationServiceException(wrapper.getCaseId(), ioe);
            log.error("Error on GovUKNotify for case id: " + wrapper.getCaseId() + ", sendBundledLetterNotification", exception);
            throw exception;
        }
    }

    private byte[] generateCoveringLetter(NotificationWrapper wrapper, Notification notification, SubscriptionType subscriptionType) {
        String bundledLetterTemplate = bundledLetterTemplateUtil.getBundledLetterTemplate(
                wrapper.getNotificationType(), wrapper.getNewSscsCaseData(), subscriptionType
        );
        return sscsGeneratePdfService.generatePdf(
                bundledLetterTemplate,
                wrapper.getNewSscsCaseData(),
                Long.parseLong(wrapper.getNewSscsCaseData().getCcdCaseId()), notification.getPlaceholders()
        );
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
        String documentUrl = null;
        if ((STRUCK_OUT.equals(notificationEventType))
                && (newSscsCaseData.getSscsStrikeOutDocument() != null)) {
            documentUrl = newSscsCaseData.getSscsStrikeOutDocument().getDocumentLink().getDocumentUrl();
        } else if ((DIRECTION_ISSUED.equals(notificationEventType))
                && (newSscsCaseData.getSscsInterlocDirectionDocument() != null)) {
            documentUrl = newSscsCaseData.getSscsInterlocDirectionDocument().getDocumentLink().getDocumentUrl();
        } else if ((JUDGE_DECISION_APPEAL_TO_PROCEED.equals(notificationEventType) || TCW_DECISION_APPEAL_TO_PROCEED.equals(notificationEventType))
                && (newSscsCaseData.getSscsInterlocDecisionDocument() != null)) {
            documentUrl = newSscsCaseData.getSscsInterlocDecisionDocument().getDocumentLink().getDocumentUrl();
        }
        return documentUrl;
    }

}
