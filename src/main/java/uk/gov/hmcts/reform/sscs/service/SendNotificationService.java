package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.config.AppConstants.*;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.service.LetterUtils.*;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.*;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.hasAppointee;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.isOkToSendEmailNotification;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.isOkToSendSmsNotification;
import static uk.gov.hmcts.reform.sscs.service.NotificationValidService.INTERLOC_LETTERS;
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
    static final String DM_STORE_USER_ID = "sscs";
    private static final String NOTIFICATION_TYPE_LETTER = "Letter";

    @Value("${feature.bundled_letters_on}")
    Boolean bundledLettersOn;

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

    @Autowired
    public SendNotificationService(
            NotificationSender notificationSender,
            EvidenceManagementService evidenceManagementService,
            SscsGeneratePdfService sscsGeneratePdfService,
            NotificationHandler notificationHandler,
            NotificationValidService notificationValidService,
            BundledLetterTemplateUtil bundledLetterTemplateUtil
    ) {
        this.notificationSender = notificationSender;
        this.evidenceManagementService = evidenceManagementService;
        this.sscsGeneratePdfService = sscsGeneratePdfService;
        this.notificationHandler = notificationHandler;
        this.notificationValidService = notificationValidService;
        this.bundledLetterTemplateUtil = bundledLetterTemplateUtil;
    }

    void sendEmailSmsLetterNotification(
            NotificationWrapper wrapper,
            Notification notification,
            SubscriptionWithType subscriptionWithType,
            NotificationEventType eventType) {
        sendEmailNotification(wrapper, subscriptionWithType.getSubscription(), notification);
        sendSmsNotification(wrapper, subscriptionWithType.getSubscription(), notification, eventType);

        boolean isInterlocLetter = INTERLOC_LETTERS.contains(eventType);
        if ((lettersOn && !isInterlocLetter) || (interlocLettersOn && isInterlocLetter)) {
            sendLetterNotification(wrapper, subscriptionWithType.getSubscription(), notification, subscriptionWithType, eventType);
        }
    }

    private void sendSmsNotification(NotificationWrapper wrapper, Subscription subscription, Notification notification, NotificationEventType eventType) {
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
                sendBundledLetterNotification(wrapper, notification, getAddressToUseForLetter(wrapper, subscriptionType), getNameToUseForLetter(wrapper, subscriptionType), subscriptionType);
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
                sendBundledLetterNotification(wrapper, notification, getAddressToUseForLetter(wrapper, subscriptionWithType.getSubscriptionType()), getNameToUseForLetter(wrapper, subscriptionWithType.getSubscriptionType()), subscriptionWithType.getSubscriptionType());
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

    private void sendBundledLetterNotification(NotificationWrapper wrapper, Notification notification, Address addressToUse, Name nameToUse, SubscriptionType subscriptionType) {
        try {
            notification.getPlaceholders().put(AppConstants.LETTER_ADDRESS_LINE_1, addressToUse.getLine1());
            notification.getPlaceholders().put(AppConstants.LETTER_ADDRESS_LINE_2, addressToUse.getLine2());
            notification.getPlaceholders().put(AppConstants.LETTER_ADDRESS_LINE_3, addressToUse.getTown());
            notification.getPlaceholders().put(AppConstants.LETTER_ADDRESS_LINE_4, addressToUse.getCounty());
            notification.getPlaceholders().put(AppConstants.LETTER_ADDRESS_POSTCODE, addressToUse.getPostcode());
            notification.getPlaceholders().put(AppConstants.LETTER_NAME, nameToUse.getFullNameNoTitle());

            if (!notification.getPlaceholders().containsKey(APPEAL_RESPOND_DATE)) {
                ZonedDateTime appealReceivedDate = ZonedDateTime.now().plusSeconds(delay);
                notification.getPlaceholders().put(APPEAL_RESPOND_DATE, appealReceivedDate.format(DateTimeFormatter.ofPattern(RESPONSE_DATE_FORMAT)));
            }

            byte[] bundledLetter = buildBundledLetter(
                    generateCoveringLetter(wrapper, notification, subscriptionType),
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
                && (newSscsCaseData.getSscsDocument() != null
                && !newSscsCaseData.getSscsDocument().isEmpty())) {
            for (SscsDocument sscsDocument : newSscsCaseData.getSscsDocument()) {
                if (STRIKE_OUT_NOTICE.equalsIgnoreCase(sscsDocument.getValue().getDocumentType())) {
                    documentUrl = sscsDocument.getValue().getDocumentLink().getDocumentUrl();
                    break;
                }
            }
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
