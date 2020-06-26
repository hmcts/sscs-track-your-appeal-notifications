package uk.gov.hmcts.reform.sscs.service.docmosis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.*;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_LAPSED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_RECEIVED_NOTIFICATION;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
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
import org.mockito.ArgumentCaptor;
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

    private static final Map<LanguagePreference, Map<String, String>> TEMPLATE_NAMES = new ConcurrentHashMap<>();
    private static final DocmosisTemplatesConfig DOCMOSIS_TEMPLATES_CONFIG = new DocmosisTemplatesConfig();


    static {
        TEMPLATE_NAMES.put(LanguagePreference.ENGLISH, Collections.singletonMap(APPEAL_RECEIVED_NOTIFICATION.getId(), "my01.doc"));
        DOCMOSIS_TEMPLATES_CONFIG.setCoversheets(TEMPLATE_NAMES);
    }


    private final PdfLetterService pdfLetterService =
            new PdfLetterService(docmosisPdfService, DOCMOSIS_TEMPLATES_CONFIG);

    private final Appellant appellant = Appellant.builder()
            .name(Name.builder().firstName("Ap").lastName("pellant").build())
            .address(Address.builder().line1("Appellant Line 1").town("Appellant Town").county("Appellant County").postcode("AP9 3LL").build())
            .appointee(Appointee.builder().build())
            .build();

    private final Representative representative = Representative.builder()
            .name(Name.builder().firstName("Rep").lastName("resentative").build())
            .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RP9 3LL").build())
            .build();

    @Test
    @Parameters({"APPELLANT", "REPRESENTATIVE"})
    public void willCreateAPdfToTheCorrectAddress(final SubscriptionType subscriptionType) {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
                APPEAL_RECEIVED_NOTIFICATION,
                appellant,
                representative,
                null
        );

        pdfLetterService.buildCoversheet(wrapper, subscriptionType);

        Address address = subscriptionType.equals(SubscriptionType.APPELLANT)
                ? appellant.getAddress() : representative.getAddress();
        Name name = subscriptionType.equals(SubscriptionType.APPELLANT)
                ? appellant.getName() : representative.getName();
        PdfCoverSheet pdfCoverSheet = new PdfCoverSheet(wrapper.getCaseId(),
                name.getFullNameNoTitle(),
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
                appellant,
                representative,
                null
        );

        pdfLetterService.buildCoversheet(wrapper, SubscriptionType.APPELLANT);
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
                appellant,
                representative,
                null
        );
        Notification notification = Notification.builder().template(Template.builder().docmosisTemplateId("docmosis.doc").build()).placeholders(new HashMap<>()).build();
        byte[] letter = pdfLetterService.generateLetter(wrapper, notification, SubscriptionType.APPELLANT);
        byte[] coversheet = pdfLetterService.buildCoversheet(wrapper, SubscriptionType.APPELLANT);
        assertTrue(ArrayUtils.isNotEmpty(letter));
        assertTrue(ArrayUtils.isNotEmpty(coversheet));
        verify(docmosisPdfService).createPdfFromMap(any(), eq(notification.getDocmosisLetterTemplate()));
        verify(docmosisPdfService).createPdf(any(), anyString());
    }

    @Test
    public void givenAnAddressWithMoreThan45CharactersInEachLine_willTruncateAddressAndGenerateALetter() throws IOException {
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        Mockito.reset(docmosisPdfService);
        when(docmosisPdfService.createPdfFromMap(any(), anyString())).thenReturn(baos.toByteArray());
        when(docmosisPdfService.createPdf(any(), anyString())).thenReturn(baos.toByteArray());
        baos.close();
        doc.close();

        Name name = Name.builder().firstName("Jimmy").lastName("AVeryLongNameWithLotsaAdLotsAndLotsOfCharacters").build();

        Address address = Address.builder()
                .line1("MyFirstVeryVeryLongAddressLineWithLotsOfCharacters")
                .line2("MySecondVeryVeryLongAddressLineWithLotsOfCharacters")
                .town("MyTownVeryVeryLongAddressLineWithLotsOfCharacters")
                .county("MyCountyVeryVeryLongAddressLineWithLotsOfCharacters")
                .postcode("L2 5UZ").build();

        appellant.setName(name);
        appellant.setAddress(address);

        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
                APPEAL_RECEIVED_NOTIFICATION,
                appellant,
                representative,
                null
        );
        Notification notification = Notification.builder().template(Template.builder().docmosisTemplateId("docmosis.doc").build()).placeholders(new HashMap<>()).build();
        byte[] letter = pdfLetterService.generateLetter(wrapper, notification, SubscriptionType.APPELLANT);
        assertTrue(ArrayUtils.isNotEmpty(letter));

        ArgumentCaptor<Map<String, Object>> placeholderCaptor = ArgumentCaptor.forClass(Map.class);

        verify(docmosisPdfService).createPdfFromMap(placeholderCaptor.capture(), eq(notification.getDocmosisLetterTemplate()));
        assertEquals("Jimmy AVeryLongNameWithLotsaAdLotsAndLotsOfCh", placeholderCaptor.getValue().get(ADDRESS_NAME));
        assertEquals("MyFirstVeryVeryLongAddressLineWithLotsOfChara", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_1));
        assertEquals("MySecondVeryVeryLongAddressLineWithLotsOfChar", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_2));
        assertEquals("MyTownVeryVeryLongAddressLineWithLotsOfCharac", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_3));
        assertEquals("MyCountyVeryVeryLongAddressLineWithLotsOfChar", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_4));
        assertEquals("L2 5UZ", placeholderCaptor.getValue().get(LETTER_ADDRESS_POSTCODE));
    }

    @Test
    public void givenAnAddressForARepWithNoName_thenAddressToOrganisationIfExists() throws IOException {
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        Mockito.reset(docmosisPdfService);
        when(docmosisPdfService.createPdfFromMap(any(), anyString())).thenReturn(baos.toByteArray());
        when(docmosisPdfService.createPdf(any(), anyString())).thenReturn(baos.toByteArray());
        baos.close();
        doc.close();

        Address address = Address.builder()
                .line1("FirstLine")
                .line2("SecondLine")
                .town("Town")
                .county("County")
                .postcode("L2 5UZ").build();

        representative.setName(null);
        representative.setAddress(address);
        representative.setOrganisation("My Company");

        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
                APPEAL_RECEIVED_NOTIFICATION,
                appellant,
                representative,
                null
        );
        Notification notification = Notification.builder().template(Template.builder().docmosisTemplateId("docmosis.doc").build()).placeholders(new HashMap<>()).build();
        byte[] letter = pdfLetterService.generateLetter(wrapper, notification, SubscriptionType.REPRESENTATIVE);
        assertTrue(ArrayUtils.isNotEmpty(letter));

        ArgumentCaptor<Map<String, Object>> placeholderCaptor = ArgumentCaptor.forClass(Map.class);

        verify(docmosisPdfService).createPdfFromMap(placeholderCaptor.capture(), eq(notification.getDocmosisLetterTemplate()));
        assertEquals("My Company", placeholderCaptor.getValue().get(ADDRESS_NAME));
        assertEquals("FirstLine", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_1));
        assertEquals("SecondLine", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_2));
        assertEquals("Town", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_3));
        assertEquals("County", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_4));
        assertEquals("L2 5UZ", placeholderCaptor.getValue().get(LETTER_ADDRESS_POSTCODE));
    }

    @Test
    public void givenAnAddressForARepWithNoName_thenAddressToSirAndMadamIfOrganisationDoesNotExists() throws IOException {
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        Mockito.reset(docmosisPdfService);
        when(docmosisPdfService.createPdfFromMap(any(), anyString())).thenReturn(baos.toByteArray());
        when(docmosisPdfService.createPdf(any(), anyString())).thenReturn(baos.toByteArray());
        baos.close();
        doc.close();

        Address address = Address.builder()
                .line1("FirstLine")
                .line2("SecondLine")
                .town("Town")
                .county("County")
                .postcode("L2 5UZ").build();

        representative.setName(null);
        representative.setAddress(address);
        representative.setOrganisation(null);

        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
                APPEAL_RECEIVED_NOTIFICATION,
                appellant,
                representative,
                null
        );
        Notification notification = Notification.builder().template(Template.builder().docmosisTemplateId("docmosis.doc").build()).placeholders(new HashMap<>()).build();
        byte[] letter = pdfLetterService.generateLetter(wrapper, notification, SubscriptionType.REPRESENTATIVE);
        assertTrue(ArrayUtils.isNotEmpty(letter));

        ArgumentCaptor<Map<String, Object>> placeholderCaptor = ArgumentCaptor.forClass(Map.class);

        verify(docmosisPdfService).createPdfFromMap(placeholderCaptor.capture(), eq(notification.getDocmosisLetterTemplate()));
        assertEquals("Sir / Madam", placeholderCaptor.getValue().get(ADDRESS_NAME));
        assertEquals("FirstLine", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_1));
        assertEquals("SecondLine", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_2));
        assertEquals("Town", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_3));
        assertEquals("County", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_4));
        assertEquals("L2 5UZ", placeholderCaptor.getValue().get(LETTER_ADDRESS_POSTCODE));
    }

    @Test
    public void willNotGenerateALetterIfNoDocmosisTemplateExists() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
                APPEAL_RECEIVED_NOTIFICATION,
                appellant,
                representative,
                null
        );
        Notification notification = Notification.builder().template(Template.builder().docmosisTemplateId(null).build()).placeholders(new HashMap<>()).build();

        byte[] bytes = pdfLetterService.generateLetter(wrapper, notification, SubscriptionType.REPRESENTATIVE);
        verifyNoInteractions(docmosisPdfService);
        assertTrue(ArrayUtils.isEmpty(bytes));
    }

    @Test(expected = NotificationClientRuntimeException.class)
    public void willHandleLoadingAnInvalidPdf() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
                APPEAL_RECEIVED_NOTIFICATION,
                appellant,
                representative,
                null
        );
        Notification notification = Notification.builder().template(Template.builder().docmosisTemplateId("some.doc").build()).placeholders(new HashMap<>()).build();

        when(docmosisPdfService.createPdfFromMap(any(), anyString())).thenReturn("Invalid PDF".getBytes());
        when(docmosisPdfService.createPdf(any(), anyString())).thenReturn("Invalid PDF".getBytes());
        pdfLetterService.generateLetter(wrapper, notification, SubscriptionType.REPRESENTATIVE);
        pdfLetterService.buildCoversheet(wrapper, SubscriptionType.REPRESENTATIVE);
    }
}
