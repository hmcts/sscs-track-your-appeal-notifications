package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.REP_SALUTATION;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPOINTEE;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.service.LetterUtils.*;
import static uk.gov.hmcts.reform.sscs.service.NotificationServiceTest.APPELLANT_WITH_ADDRESS;
import static uk.gov.hmcts.reform.sscs.service.SendNotificationServiceTest.APPELLANT_WITH_ADDRESS_AND_APPOINTEE;
import static uk.gov.hmcts.reform.sscs.service.SendNotificationServiceTest.REP_WITH_ADDRESS;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.exception.NotificationClientRuntimeException;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;

@RunWith(JUnitParamsRunner.class)
public class LetterUtilsTest {
    @Test
    public void useAppellantAddressForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS,
            null,
            null
        );

        assertEquals(APPELLANT_WITH_ADDRESS.getAddress(), getAddressToUseForLetter(wrapper, APPELLANT));
    }

    @Test
    public void useAppointeeAddressForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
            null,
            null
        );

        assertEquals(APPELLANT_WITH_ADDRESS_AND_APPOINTEE.getAppointee().getAddress(), getAddressToUseForLetter(wrapper, APPOINTEE));
    }

    @Test
    public void useRepAddressForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
            REP_WITH_ADDRESS,
            null
        );

        assertEquals(REP_WITH_ADDRESS.getAddress(), getAddressToUseForLetter(wrapper, REPRESENTATIVE));
    }

    @Test
    @Parameters(method = "filenameForLetter")
    public void useCorrectFilenameForLetter(NotificationEventType eventType, String filename) {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            eventType,
            APPELLANT_WITH_ADDRESS,
            null,
            null
        );

        assertEquals(filename, getFilename(wrapper));
    }

    public Object[] filenameForLetter() {
        return new Object[] {
            new Object[] { STRUCK_OUT, PDF_STRUCK_OUT},
            new Object[] { DIRECTION_ISSUED, PDF_DIRECTION_NOTICE},
            new Object[] { DECISION_ISSUED, PDF_DECISION_NOTICE}
        };
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

        assertEquals(APPELLANT_WITH_ADDRESS.getName().getFullNameNoTitle(), getNameToUseForLetter(wrapper, APPELLANT));
    }

    @Test
    public void useAppointeeNameForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
            null,
            null
        );

        assertEquals(APPELLANT_WITH_ADDRESS_AND_APPOINTEE.getAppointee().getName().getFullNameNoTitle(), getNameToUseForLetter(wrapper, APPOINTEE));
    }

    @Test
    @Parameters(method = "repNamesForLetters")
    public void useRepNameForLetter(Name name, String expectedResult) {
        Representative rep = REP_WITH_ADDRESS;
        rep.setName(name);

        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
            rep,
            null
        );

        assertEquals(expectedResult, getNameToUseForLetter(wrapper, REPRESENTATIVE));
    }

    private Object[] repNamesForLetters() {
        return new Object[] {
                new Object[]{REP_WITH_ADDRESS.getName(), REP_WITH_ADDRESS.getName().getFullNameNoTitle()},
                new Object[]{Name.builder().build(), REP_SALUTATION},
                new Object[]{Name.builder().firstName("undefined").lastName("undefined").build(), REP_SALUTATION}
        };
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

    @Test
    @Parameters({"1", "2", "3", "4"})
    public void willAddABlankPageAtTheEndIfAnOddPageIsGiven(int pages) throws IOException {
        PDDocument originalDocument = new PDDocument();

        // Create a new blank page and add it to the originalDocument
        PDPage blankPage = new PDPage();
        for (int i = 1; i <= pages; i++) {
            originalDocument.addPage(blankPage);
        }
        assertEquals(pages, originalDocument.getNumberOfPages());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        originalDocument.save(baos);
        originalDocument.close();
        byte[] bytes = baos.toByteArray();
        baos.close();

        byte[] newBytes = addBlankPageAtTheEndIfOddPage(bytes);
        PDDocument newDocument = PDDocument.load(newBytes);
        int expectedPages = (pages % 2 == 0) ? pages : pages + 1;
        assertEquals(expectedPages, newDocument.getNumberOfPages());
    }

}
