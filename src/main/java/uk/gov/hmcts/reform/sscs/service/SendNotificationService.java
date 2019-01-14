package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.STRUCK_OUT;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.AppConstants;
import uk.gov.hmcts.reform.sscs.domain.notify.Notification;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.exception.NotificationClientRuntimeException;
import uk.gov.hmcts.reform.sscs.exception.NotificationServiceException;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;

@Service
@Slf4j
public class SendNotificationService {
    private static final String DIRECTION_TEXT = "Direction Text";
    static final String DM_STORE_USER_ID = "sscs";

    private final NotificationSender notificationSender;
    private final EvidenceManagementService evidenceManagementService;
    private final SscsGeneratePdfService sscsGeneratePdfService;
    private final NotificationHandler notificationHandler;

    @Value("${noncompliantcaseletter.appeal.html.template.path}")
    String noncompliantcaseletterTemplate;

    @Autowired
    public SendNotificationService(
            NotificationSender notificationSender,
            EvidenceManagementService evidenceManagementService,
            SscsGeneratePdfService sscsGeneratePdfService,
            NotificationHandler notificationHandler
    ) {
        this.notificationSender = notificationSender;
        this.evidenceManagementService = evidenceManagementService;
        this.sscsGeneratePdfService = sscsGeneratePdfService;
        this.notificationHandler = notificationHandler;
    }

    void sendEmailSmsLetterNotification(
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

            byte[] bundledLetter = buildBundledLetter(
                    generateCoveringLetter(wrapper, notification),
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
            log.error("Error on GovUKNotify for case id: " + wrapper.getCaseId() + ", sendBundledLetterNotification", exception);
            throw exception;
        }
    }

    static Address getAddressToUseForLetter(NotificationWrapper wrapper) {
        if (null != wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee()) {
            return wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee().getAddress();
        } else {
            return wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAddress();
        }
    }

    private static Name getNameToUseForLetter(NotificationWrapper wrapper) {
        if (null != wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee()) {
            return wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee().getName();
        } else {
            return wrapper.getNewSscsCaseData().getAppeal().getAppellant().getName();
        }
    }

    private byte[] generateCoveringLetter(NotificationWrapper wrapper, Notification notification) {
        return sscsGeneratePdfService.generatePdf(noncompliantcaseletterTemplate, wrapper.getNewSscsCaseData(),
                Long.parseLong(wrapper.getNewSscsCaseData().getCcdCaseId()), notification.getPlaceholders());
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
