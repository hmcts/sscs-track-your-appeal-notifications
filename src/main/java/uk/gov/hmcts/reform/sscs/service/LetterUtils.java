package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.STRUCK_OUT;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.exception.NotificationClientRuntimeException;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;

public class LetterUtils {
    public static final String PDF_DIRECTION_NOTICE = "Direction_Notice.pdf";
    public static final String PDF_UNKNOWN = "unknown.pdf";
    public static final String COMMENT_UNKNOWN = "Unknown";
    public static final String COMMENT_DIRECTION_NOTICE = "Direction Notice";

    private LetterUtils() {
        // Hiding utility class constructor
    }

    public static Address getAddressToUseForLetter(NotificationWrapper wrapper) {
        if (null != wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee()) {
            return wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee().getAddress();
        }

        return wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAddress();
    }

    public static String getFilename(NotificationWrapper wrapper) {
        if (STRUCK_OUT.equals(wrapper.getNotificationType())) {
            return PDF_DIRECTION_NOTICE;
        }

        return PDF_UNKNOWN;
    }

    public static Name getNameToUseForLetter(NotificationWrapper wrapper) {
        if (null != wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee()) {
            return wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee().getName();
        } else {
            return wrapper.getNewSscsCaseData().getAppeal().getAppellant().getName();
        }
    }

    public static byte[] buildBundledLetter(byte[] coveringLetter, byte[] directionText) throws IOException {
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

    public static String getSystemComment(NotificationWrapper wrapper) {
        if (STRUCK_OUT.equals(wrapper.getNotificationType())) {
            return COMMENT_DIRECTION_NOTICE;
        }

        return COMMENT_UNKNOWN;
    }
}
