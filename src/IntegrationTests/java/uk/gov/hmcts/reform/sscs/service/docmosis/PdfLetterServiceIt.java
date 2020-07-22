package uk.gov.hmcts.reform.sscs.service.docmosis;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.config.DocmosisTemplatesConfig;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.docmosis.PdfCoverSheet;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.exception.PdfGenerationException;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.DocmosisPdfService;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("integration")
@AutoConfigureMockMvc
@Slf4j
public class PdfLetterServiceIt {
    private static final String CASE_ID = "1000001";
    private static final String DATE = "2018-01-01T14:01:18.243";
    private static final String YES = "Yes";

    @Autowired
    private PdfLetterService pdfLetterService;

    @Autowired
    private DocmosisTemplatesConfig docmosisTemplatesConfig;

    @MockBean
    private DocmosisPdfService docmosisPdfService;

    @Test
    public void canGenerateACoversheetOnAppealReceived() throws IOException {
        byte[] pdfbytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream(
                "pdfs/direction-notice-coversheet-sample.pdf"));
        when(docmosisPdfService.createPdf(any(Object.class), anyString())).thenReturn(pdfbytes);
        SscsCaseData sscsCaseData = getSscsCaseData();
        SscsCaseDataWrapper dataWrapper = SscsCaseDataWrapper.builder()
                .newSscsCaseData(sscsCaseData)
                .oldSscsCaseData(sscsCaseData)
                .createdDate(LocalDateTime.now().minusMinutes(10))
                .notificationEventType(NotificationEventType.APPEAL_RECEIVED_NOTIFICATION)
                .build();
        NotificationWrapper wrapper = new CcdNotificationWrapper(dataWrapper);
        byte[] bytes = pdfLetterService.buildCoversheet(wrapper, SubscriptionType.APPELLANT);
        assertNotNull(bytes);
        PdfCoverSheet pdfCoverSheet = new PdfCoverSheet(
                wrapper.getCaseId(),
                wrapper.getNewSscsCaseData().getAppeal().getAppellant().getName().getFullNameNoTitle(),
                wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAddress().getLine1(),
                wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAddress().getLine2(),
                wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAddress().getTown(),
                wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAddress().getCounty(),
                wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAddress().getPostcode(),
                docmosisTemplatesConfig.getHmctsImgVal(),
                docmosisTemplatesConfig.getWelshHmctsImgVal());

        verify(docmosisPdfService).createPdf(eq(pdfCoverSheet), eq(docmosisTemplatesConfig.getCoversheets()
                .get(LanguagePreference.ENGLISH).get(APPEAL_RECEIVED.getType())));
    }

    @Test(expected = PdfGenerationException.class)
    public void willNotGenerateACoversheetOnAppealDormant() {

        SscsCaseData sscsCaseData = getSscsCaseData();
        SscsCaseDataWrapper dataWrapper = SscsCaseDataWrapper.builder()
                .newSscsCaseData(sscsCaseData)
                .oldSscsCaseData(sscsCaseData)
                .createdDate(LocalDateTime.now().minusMinutes(10))
                .notificationEventType(NotificationEventType.APPEAL_DORMANT_NOTIFICATION)
                .build();
        NotificationWrapper wrapper = new CcdNotificationWrapper(dataWrapper);
        pdfLetterService.buildCoversheet(wrapper, SubscriptionType.APPELLANT);
        verifyZeroInteractions(docmosisPdfService);
    }

    private SscsCaseData getSscsCaseData() {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(APPEAL_RECEIVED.getCcdType()).build()).build());

        return SscsCaseData.builder().ccdCaseId(CASE_ID).events(events)
                .appeal(Appeal.builder()
                        .mrnDetails(MrnDetails.builder().mrnDate(DATE).dwpIssuingOffice("office").build())
                        .appealReasons(AppealReasons.builder().build())
                        .rep(Representative.builder()
                                .hasRepresentative(YES)
                                .name(Name.builder().firstName("Rep").lastName("lastName").build())
                                .contact(Contact.builder().build())
                                .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 7SE").build())
                                .build())
                        .appellant(Appellant.builder()
                                .name(Name.builder().firstName("firstName").lastName("lastName").build())
                                .address(Address.builder().line1("122 Breach Street").line2("The Village").town("My town").county("Cardiff").postcode("CF11 2HB").build())
                                .contact(Contact.builder().build())
                                .identity(Identity.builder().nino("NP 27 28 67 B").dob("12 March 1971").build()).build())
                        .hearingType(AppealHearingType.ORAL.name())
                        .benefitType(BenefitType.builder().code(Benefit.PIP.name()).build())
                        .hearingOptions(HearingOptions.builder()
                                .wantsToAttend(YES)
                                .build())
                        .build())
                .build();
    }

}
