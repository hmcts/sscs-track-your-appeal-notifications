package uk.gov.hmcts.reform.sscs.service.docmosis;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_LAPSED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_RECEIVED_NOTIFICATION;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.DocmosisTemplatesConfig;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.docmosis.PdfCoverSheet;
import uk.gov.hmcts.reform.sscs.domain.notify.Notification;
import uk.gov.hmcts.reform.sscs.domain.notify.Template;
import uk.gov.hmcts.reform.sscs.exception.NotificationClientRuntimeException;
import uk.gov.hmcts.reform.sscs.exception.PdfGenerationException;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.DocmosisPdfService;
import uk.gov.hmcts.reform.sscs.service.NotificationServiceTest;

@RunWith(JUnitParamsRunner.class)
public class PdfLetterServiceTest {

    private final DocmosisPdfService docmosisPdfService = mock(DocmosisPdfService.class);

    private static final Map<String, String> TEMPLATE_NAMES = new ConcurrentHashMap<>();
    private static final DocmosisTemplatesConfig DOCMOSIS_TEMPLATES_CONFIG = new DocmosisTemplatesConfig();

    static {
        TEMPLATE_NAMES.put(APPEAL_RECEIVED_NOTIFICATION.getId(), "my01.doc");
        DOCMOSIS_TEMPLATES_CONFIG.setCoversheets(TEMPLATE_NAMES);
    }


    private final PdfLetterService pdfLetterService =
            new PdfLetterService(docmosisPdfService, DOCMOSIS_TEMPLATES_CONFIG);

    private static final Appellant APPELLANT = Appellant.builder()
            .name(Name.builder().firstName("Ap").lastName("pellant").build())
            .address(Address.builder().line1("Appellant Line 1").town("Appellant Town").county("Appellant County").postcode("AP9 3LL").build())
            .appointee(Appointee.builder().build())
            .build();

    private static final Representative REPRESENTATIVE = Representative.builder()
            .name(Name.builder().firstName("Rep").lastName("resentative").build())
            .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RP9 3LL").build())
            .build();

    @Test
    @Parameters({"APPELLANT", "REPRESENTATIVE"})
    public void willCreateAPdfToTheCorrectAddress(final SubscriptionType subscriptionType) {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
                APPEAL_RECEIVED_NOTIFICATION,
                APPELLANT,
                REPRESENTATIVE,
                null
        );

        pdfLetterService.generateCoversheet(wrapper, subscriptionType);

        Address address = subscriptionType.equals(SubscriptionType.APPELLANT)
                ? APPELLANT.getAddress() : REPRESENTATIVE.getAddress();
        PdfCoverSheet pdfCoverSheet = new PdfCoverSheet(wrapper.getCaseId(),
                address.getLine1(),
                address.getLine2(),
                address.getTown(),
                address.getCounty(),
                address.getPostcode(),
                DOCMOSIS_TEMPLATES_CONFIG.getHmctsImgVal()
        );
        verify(docmosisPdfService).createPdf(eq(pdfCoverSheet), eq("my01.doc"));
    }

    @Test(expected = PdfGenerationException.class)
    public void willThrowAnErrorIfIncorrectEventIsSentToGenerateACoversheet() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
                APPEAL_LAPSED_NOTIFICATION,
                APPELLANT,
                REPRESENTATIVE,
                null
        );

        pdfLetterService.generateCoversheet(wrapper, SubscriptionType.APPELLANT);
    }

    @Test
    public void willGenerateALetter() throws IOException {
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        Mockito.reset(docmosisPdfService);
        when(docmosisPdfService.createPdfFromMap(any(), anyString())).thenReturn(baos.toByteArray());
        when(docmosisPdfService.createPdf(any(), anyString())).thenReturn(baos.toByteArray());
        baos.close();
        doc.close();
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
                APPEAL_RECEIVED_NOTIFICATION,
                APPELLANT,
                REPRESENTATIVE,
                null
        );
        Notification notification = Notification.builder().template(Template.builder().docmosisTemplateId("docmosis.doc").build()).placeholders(new HashMap<>()).build();
        byte[] bytes = pdfLetterService.generateLetter(wrapper, notification, SubscriptionType.APPELLANT);
        assertTrue(ArrayUtils.isNotEmpty(bytes));
        verify(docmosisPdfService).createPdfFromMap(any(), eq(notification.getDocmosisLetterTemplate()));
        verify(docmosisPdfService).createPdf(any(), anyString());
    }

    @Test
    public void willNotGenerateALetterIfNoDocmosisTemplateExists() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
                APPEAL_RECEIVED_NOTIFICATION,
                APPELLANT,
                REPRESENTATIVE,
                null
        );
        Notification notification = Notification.builder().template(Template.builder().docmosisTemplateId(null).build()).placeholders(new HashMap<>()).build();

        byte[] bytes = pdfLetterService.generateLetter(wrapper, notification, SubscriptionType.REPRESENTATIVE);
        verifyZeroInteractions(docmosisPdfService);
        assertTrue(ArrayUtils.isEmpty(bytes));
    }

    @Test(expected = NotificationClientRuntimeException.class)
    public void willHandleLoadingAnInvalidPdf() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
                APPEAL_RECEIVED_NOTIFICATION,
                APPELLANT,
                REPRESENTATIVE,
                null
        );
        Notification notification = Notification.builder().template(Template.builder().docmosisTemplateId("some.doc").build()).placeholders(new HashMap<>()).build();

        when(docmosisPdfService.createPdfFromMap(any(), anyString())).thenReturn("Invalid PDF".getBytes());
        when(docmosisPdfService.createPdf(any(), anyString())).thenReturn("Invalid PDF".getBytes());
        pdfLetterService.generateLetter(wrapper, notification, SubscriptionType.REPRESENTATIVE);
    }
}
