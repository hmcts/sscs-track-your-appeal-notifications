package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.hasAppointee;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.exception.NotificationClientRuntimeException;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;

public class LetterUtils {
    public static final String PDF_STRUCK_OUT = "Struck_Out.pdf";
    public static final String PDF_DIRECTION_NOTICE = "Direction_Notice.pdf";
    public static final String PDF_DECISION_NOTICE = "Decision_Notice.pdf";
    public static final String PDF_UNKNOWN = "unknown.pdf";
    public static final String COMMENT_UNKNOWN = "Unknown";
    public static final String COMMENT_DIRECTION_NOTICE = "Direction Notice";

    private LetterUtils() {
        // Hiding utility class constructor
    }

    public static Address getAddressToUseForLetter(NotificationWrapper wrapper, SubscriptionType subscriptionType) {
        if (REPRESENTATIVE.equals(subscriptionType)) {
            return wrapper.getNewSscsCaseData().getAppeal().getRep().getAddress();
        } else {
            if (hasAppointee(wrapper.getSscsCaseDataWrapper())) {
                return wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee().getAddress();
            }

            return wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAddress();
        }
    }

    public static String getFilename(NotificationWrapper wrapper) {
        if (STRUCK_OUT.equals(wrapper.getNotificationType())) {
            return PDF_STRUCK_OUT;
        } else if (DIRECTION_ISSUED.equals(wrapper.getNotificationType())) {
            return PDF_DIRECTION_NOTICE;
        } else if (DECISION_ISSUED.equals(wrapper.getNotificationType()) || ISSUE_FINAL_DECISION.equals(wrapper.getNotificationType())) {
            return PDF_DECISION_NOTICE;
        }

        return PDF_UNKNOWN;
    }

    public static String getNameToUseForLetter(NotificationWrapper wrapper, SubscriptionType subscriptionType) {
        if (REPRESENTATIVE.equals(subscriptionType)) {
            return SendNotificationHelper.getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep(), false);
        } else {
            if (hasAppointee(wrapper.getSscsCaseDataWrapper())) {
                return wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee().getName().getFullNameNoTitle();
            } else {
                return wrapper.getNewSscsCaseData().getAppeal().getAppellant().getName().getFullNameNoTitle();
            }
        }
    }

    public static byte[] addBlankPageAtTheEndIfOddPage(byte[] letter) throws IOException {
        if (ArrayUtils.isNotEmpty(letter)) {
            PDDocument loadDoc = PDDocument.load(letter);
            if (loadDoc.getNumberOfPages() % 2 != 0) {
                final PDPage blankPage = new PDPage(PDRectangle.A4);
                // need to add PDPageContentStream here to pass gov notify validation!
                PDPageContentStream contents = new PDPageContentStream(loadDoc, blankPage);
                contents.beginText();
                contents.endText();
                contents.close();
                loadDoc.addPage(blankPage);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                loadDoc.save(baos);
                loadDoc.close();
                byte[] bytes = baos.toByteArray();
                baos.close();
                return bytes;
            }
        }
        return letter;
    }

    public static byte[] buildBundledLetter(byte[] coveringLetter, byte[] directionText) throws IOException {
        if (coveringLetter != null && directionText != null) {
            PDDocument bundledLetter = PDDocument.load(coveringLetter);

            PDDocument loadDoc = PDDocument.load(directionText);

            final PDFMergerUtility merger = new PDFMergerUtility();
            merger.appendDocument(bundledLetter, loadDoc);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bundledLetter.save(baos);
            bundledLetter.close();

            return baos.toByteArray();
        } else {
            throw new NotificationClientRuntimeException("Can not bundle empty documents");
        }
    }

    public static String getSystemComment(NotificationWrapper wrapper) {
        if (STRUCK_OUT.equals(wrapper.getNotificationType())) {
            return COMMENT_DIRECTION_NOTICE;
        }

        return COMMENT_UNKNOWN;
    }
}
