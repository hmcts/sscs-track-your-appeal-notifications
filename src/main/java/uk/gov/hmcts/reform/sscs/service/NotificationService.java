package uk.gov.hmcts.reform.sscs.service;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.getBenefitByCode;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.STRUCK_OUT;
import static uk.gov.hmcts.reform.sscs.service.NotificationValidService.isMandatoryLetter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.AppConstants;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.domain.notify.Destination;
import uk.gov.hmcts.reform.sscs.domain.notify.Notification;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.domain.notify.Reference;
import uk.gov.hmcts.reform.sscs.domain.notify.Template;
import uk.gov.hmcts.reform.sscs.exception.NotificationClientRuntimeException;
import uk.gov.hmcts.reform.sscs.exception.NotificationServiceException;
import uk.gov.hmcts.reform.sscs.factory.NotificationFactory;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
@Slf4j
public class NotificationService {
    private static final Logger LOG = getLogger(NotificationService.class);

    public static final String DM_STORE_USER_ID = "sscs";
    public static final String DIRECTION_TEXT = "Direction Text";

    private final String noncompliantcaseletterTemplate;
    private final NotificationSender notificationSender;
    private final NotificationFactory notificationFactory;
    private final ReminderService reminderService;
    private final NotificationValidService notificationValidService;
    private final NotificationHandler notificationHandler;
    private final OutOfHoursCalculator outOfHoursCalculator;
    private final NotificationConfig notificationConfig;
    private final EvidenceManagementService evidenceManagementService;
    private final SscsGeneratePdfService sscsGeneratePdfService;
    private final IdamService idamService;

    @Autowired
    public NotificationService(@Value("${noncompliantcaseletter.appeal.html.template.path}") String noncompliantcaseletterTemplate,
                               NotificationSender notificationSender, NotificationFactory notificationFactory,
                               ReminderService reminderService, NotificationValidService notificationValidService,
                               NotificationHandler notificationHandler,
                               OutOfHoursCalculator outOfHoursCalculator, NotificationConfig notificationConfig,
                               EvidenceManagementService evidenceManagementService,
                               SscsGeneratePdfService sscsGeneratePdfService,
                               IdamService idamService) {
        this.noncompliantcaseletterTemplate = noncompliantcaseletterTemplate;
        this.notificationFactory = notificationFactory;
        this.notificationSender = notificationSender;
        this.reminderService = reminderService;
        this.notificationValidService = notificationValidService;
        this.notificationHandler = notificationHandler;
        this.outOfHoursCalculator = outOfHoursCalculator;
        this.notificationConfig = notificationConfig;
        this.evidenceManagementService = evidenceManagementService;
        this.sscsGeneratePdfService = sscsGeneratePdfService;
        this.idamService = idamService;
    }

    public void manageNotificationAndSubscription(NotificationWrapper notificationWrapper) {
        NotificationEventType notificationType = notificationWrapper.getNotificationType();
        final String caseId = notificationWrapper.getCaseId();
        log.info("Notification event triggered {} for case id {}", notificationType.getId(), caseId);
        for (SubscriptionWithType subscriptionWithType :
                notificationWrapper.getSubscriptionsBasedOnNotificationType()) {
            sendNotificationPerSubscription(notificationWrapper, subscriptionWithType, notificationType);
        }
    }

    private void sendNotificationPerSubscription(NotificationWrapper notificationWrapper,
                                                 SubscriptionWithType subscriptionWithType,
                                                 NotificationEventType notificationType) {
        if (isValidNotification(notificationWrapper, subscriptionWithType.getSubscription(), notificationType)) {
            Notification notification = notificationFactory.create(notificationWrapper,
                    subscriptionWithType.getSubscriptionType());
            if (notificationWrapper.getNotificationType().isAllowOutOfHours() || !outOfHoursCalculator.isItOutOfHours()) {
                sendEmailSmsLetterNotification(notificationWrapper, subscriptionWithType.getSubscription(), notification);
                processOldSubscriptionNotifications(notificationWrapper, notification);
            } else {
                notificationHandler.scheduleNotification(notificationWrapper);
            }
            reminderService.createReminders(notificationWrapper);
        }
    }

    private boolean isValidNotification(NotificationWrapper wrapper, Subscription
            subscription, NotificationEventType notificationType) {
        return (isMandatoryLetter(notificationType) || (subscription != null && subscription.doesCaseHaveSubscriptions()
                && notificationValidService.isNotificationStillValidToSend(wrapper.getNewSscsCaseData().getHearings(), notificationType)
                && notificationValidService.isHearingTypeValidToSendNotification(wrapper.getNewSscsCaseData(), notificationType)));
    }

    private void processOldSubscriptionNotifications(NotificationWrapper wrapper, Notification notification) {
        if (wrapper.getNotificationType() == NotificationEventType.SUBSCRIPTION_UPDATED_NOTIFICATION
                && wrapper.getHearingType() == AppealHearingType.PAPER) {
            Subscription newSubscription = wrapper.getNewSscsCaseData().getSubscriptions().getAppellantSubscription();
            Subscription oldSubscription = wrapper.getOldSscsCaseData().getSubscriptions().getAppellantSubscription();

            String emailAddress = null;
            String smsNumber = null;

            if (null != newSubscription.getEmail() && null != oldSubscription.getEmail()) {
                emailAddress = newSubscription.getEmail().equals(oldSubscription.getEmail()) ? null : oldSubscription.getEmail();
            } else if (null == newSubscription.getEmail() && null != oldSubscription.getEmail()) {
                emailAddress = oldSubscription.getEmail();
            }

            if (null != newSubscription.getMobile() && null != oldSubscription.getMobile()) {
                smsNumber = newSubscription.getMobile().equals(oldSubscription.getMobile()) ? null : oldSubscription.getMobile();
            } else if (null == newSubscription.getMobile() && null != oldSubscription.getMobile()) {
                smsNumber = oldSubscription.getMobile();
            }


            Destination destination = Destination.builder().email(emailAddress).sms(smsNumber).build();

            Benefit benefit = getBenefitByCode(wrapper.getSscsCaseDataWrapper()
                    .getNewSscsCaseData().getAppeal().getBenefitType().getCode());

            Template template = notificationConfig.getTemplate(
                NotificationEventType.SUBSCRIPTION_OLD_NOTIFICATION.getId(),
                NotificationEventType.SUBSCRIPTION_OLD_NOTIFICATION.getId(),
                NotificationEventType.SUBSCRIPTION_OLD_NOTIFICATION.getId(),
                benefit,
                wrapper.getHearingType()
            );

            Notification oldNotification = Notification.builder().template(template).appealNumber(notification.getAppealNumber())
                    .destination(destination)
                    .reference(new Reference(wrapper.getOldSscsCaseData().getCaseReference()))
                    .appealNumber(notification.getAppealNumber())
                    .placeholders(notification.getPlaceholders()).build();

            sendEmailSmsLetterNotification(wrapper, oldSubscription, oldNotification);
        }
    }

    private void sendEmailSmsLetterNotification(
        NotificationWrapper wrapper,
        Subscription subscription,
        Notification notification
    ) {
        sendEmailNotification(wrapper, subscription, notification);
        sendSmsNotification(wrapper, subscription, notification);
        sendBundledLetterNotificationToAppellant(wrapper, notification);
        sendBundledLetterNotificationToRepresentative(wrapper, notification);
    }

    private void sendSmsNotification(NotificationWrapper wrapper, Subscription subscription, Notification notification) {
        if (subscription.isSmsSubscribed() && notification.isSms() && notification.getSmsTemplate() != null) {
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
        if (subscription.isEmailSubscribed() && notification.isEmail() && notification.getEmailTemplate() != null) {
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

            IdamTokens idamTokens = idamService.getIdamTokens();

            byte[] bundledLetter = buildBundledLetter(
                generateCoveringLetter(wrapper, notification, idamTokens),
                downloadDirectionText(wrapper)
            );

            NotificationHandler.SendNotification sendNotification = () ->
                notificationSender.sendBundledLetter(
                    wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAddress().getPostcode(),   // Used for whitelisting only
                    bundledLetter,
                    notification.getReference(),
                    wrapper.getCaseId()
                );
            notificationHandler.sendNotification(wrapper, notification.getLetterTemplate(), "Letter", sendNotification);
        } catch (IOException ioe) {
            NotificationServiceException exception = new NotificationServiceException(wrapper.getCaseId(), ioe);
            LOG.error("Error on GovUKNotify for case id: " + wrapper.getCaseId() + ", sendBundledLetterNotification", exception);
            throw exception;
        }
    }

    protected static Address getAddressToUseForLetter(NotificationWrapper wrapper) {
        if (null != wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee()) {
            return wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee().getAddress();
        } else {
            return wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAddress();
        }
    }

    protected static Name getNameToUseForLetter(NotificationWrapper wrapper) {
        if (null != wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee()) {
            return wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee().getName();
        } else {
            return wrapper.getNewSscsCaseData().getAppeal().getAppellant().getName();
        }
    }

    private byte[] generateCoveringLetter(NotificationWrapper wrapper, Notification notification, IdamTokens idamTokens) {
        return sscsGeneratePdfService.generateAndSavePdf(
            noncompliantcaseletterTemplate,
            wrapper.getNewSscsCaseData(),
            Long.parseLong(wrapper.getNewSscsCaseData().getCcdCaseId()),
            notification.getPlaceholders(),
            idamTokens
        );
    }

    private byte[] downloadDirectionText(NotificationWrapper wrapper) {
        NotificationEventType notificationEventType = wrapper.getSscsCaseDataWrapper().getNotificationEventType();
        SscsCaseData newSscsCaseData = wrapper.getNewSscsCaseData();

        byte[] directionText = null;
        if ((notificationEventType.equals(STRUCK_OUT))
            && (newSscsCaseData.getSscsDocument() != null
            && !newSscsCaseData.getSscsDocument().isEmpty())) {
            for (SscsDocument sscsDocument : newSscsCaseData.getSscsDocument()) {
                if (DIRECTION_TEXT.equalsIgnoreCase(sscsDocument.getValue().getDocumentType())) {
                    directionText =  evidenceManagementService.download(
                        URI.create(sscsDocument.getValue().getDocumentLink().getDocumentUrl()),
                        DM_STORE_USER_ID
                    );

                    break;
                }
            }
        }

        return directionText;
    }

    private byte[] buildBundledLetter(byte[] coveringLetter, byte[] directionText) throws IOException {
        if (coveringLetter != null && directionText != null) {
            PDDocument bundledLetter = PDDocument.load(coveringLetter);

            PDDocument loadDoc = PDDocument.load(directionText);

            final PDFMergerUtility merger = new PDFMergerUtility();
            merger.appendDocument(bundledLetter, loadDoc);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bundledLetter.save(baos);

            return baos.toByteArray();
        } else {
            throw new NotificationClientRuntimeException("Can not bundle empty documents");
        }
    }
}
