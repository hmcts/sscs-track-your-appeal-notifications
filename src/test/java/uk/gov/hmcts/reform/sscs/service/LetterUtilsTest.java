package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.STRUCK_OUT;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SYA_APPEAL_CREATED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.service.LetterUtils.*;
import static uk.gov.hmcts.reform.sscs.service.NotificationServiceTest.APPELLANT_WITH_ADDRESS;
import static uk.gov.hmcts.reform.sscs.service.SendNotificationServiceTest.APPELLANT_WITH_ADDRESS_AND_APPOINTEE;

import org.apache.pdfbox.io.IOUtils;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.exception.NotificationClientRuntimeException;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;

import java.io.IOException;

public class LetterUtilsTest {
    @Test
    public void useAppellantAddressForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS,
            null,
            null
        );

        assertEquals(APPELLANT_WITH_ADDRESS.getAddress(), getAddressToUseForLetter(wrapper));
    }

    @Test
    public void useAppointeeAddressForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
            null,
            null
        );

        assertEquals(APPELLANT_WITH_ADDRESS_AND_APPOINTEE.getAppointee().getAddress(), getAddressToUseForLetter(wrapper));
    }

    @Test
    public void useStruckOutFilenameForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            STRUCK_OUT,
            APPELLANT_WITH_ADDRESS,
            null,
            null
        );

        assertEquals(PDF_DIRECTION_NOTICE, getFilename(wrapper));
    }

    @Test
    public void useUnknownFilenameForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS,
            null,
            null
        );

        assertEquals(PDF_UNKNOWN, getFilename(wrapper));
    }

    @Test
    public void useAppellantNameForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS,
            null,
            null
        );

        assertEquals(APPELLANT_WITH_ADDRESS.getName(), getNameToUseForLetter(wrapper));
    }

    @Test
    public void useAppointeeNameForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
            null,
            null
        );

        assertEquals(APPELLANT_WITH_ADDRESS_AND_APPOINTEE.getAppointee().getName(), getNameToUseForLetter(wrapper));
    }

    @Test
    public void useStruckOutSystemCommentForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            STRUCK_OUT,
            APPELLANT_WITH_ADDRESS,
            null,
            null
        );

        assertEquals(COMMENT_DIRECTION_NOTICE, getSystemComment(wrapper));
    }

    @Test
    public void useUnknownSystemCommentForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS,
            null,
            null
        );

        assertEquals(COMMENT_UNKNOWN, getSystemComment(wrapper));
    }

    @Test
    public void successfulBundleLetter() throws IOException {
        byte[] sampleDirectionText = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdfs/direction-text.pdf"));
        byte[] sampleDirectionCoversheet = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdfs/direction-notice-coversheet-sample.pdf"));

        assertNotNull(buildBundledLetter(sampleDirectionCoversheet, sampleDirectionText));
    }

    @Test(expected = NotificationClientRuntimeException.class)
    public void shouldNotBundleLetterWhenCoverSheetIsNull() throws IOException {
        byte[] sampleDirectionText = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdfs/direction-text.pdf"));

        buildBundledLetter(null, sampleDirectionText);
    }
    @Test(expected = NotificationClientRuntimeException.class)
    public void shouldNotBundleLetterWhenAttachmentIsNull() throws IOException {
        byte[] sampleDirectionCoversheet = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdfs/direction-notice-coversheet-sample.pdf"));

        buildBundledLetter(sampleDirectionCoversheet, null);
    }
}
