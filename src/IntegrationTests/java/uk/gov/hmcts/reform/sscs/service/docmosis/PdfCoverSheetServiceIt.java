package uk.gov.hmcts.reform.sscs.service.docmosis;

import static org.junit.Assert.assertNotNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;

@RunWith(JUnitParamsRunner.class)
@SpringBootTest
@ActiveProfiles("integration")
@AutoConfigureMockMvc
@Slf4j
public class PdfCoverSheetServiceIt {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private static final String CASE_ID = "1000001";
    private static final String DATE = "2018-01-01T14:01:18.243";
    private static final String YES = "Yes";

    @Autowired
    private PdfCoverSheetService pdfCoverSheetService;

    @Test
    public void canGenerateACoversheetOnAppealReceived() throws IOException {

        SscsCaseData sscsCaseData = getSscsCaseData();
        SscsCaseDataWrapper dataWrapper = SscsCaseDataWrapper.builder()
                .newSscsCaseData(sscsCaseData)
                .oldSscsCaseData(sscsCaseData)
                .notificationEventType(NotificationEventType.APPEAL_RECEIVED_NOTIFICATION)
                .build();
        NotificationWrapper wrapper = new CcdNotificationWrapper(dataWrapper);
        byte[] bytes = pdfCoverSheetService.generateCoversheet(wrapper, SubscriptionType.APPELLANT);
        assertNotNull(bytes);
        assertPdfIsValid(bytes);
    }

    private void assertPdfIsValid(byte[] bytes) throws IOException {
        File tempFile = File.createTempFile("test", ".pdf");
        log.info("Creating PDF temp file: " + tempFile.getAbsoluteFile());
        FileUtils.writeByteArrayToFile(tempFile, bytes);
        RandomAccessFile accessFile = new RandomAccessFile(tempFile, "r");
        PDFParser parser = new PDFParser(accessFile);
        parser.setLenient(false);
        parser.parse();
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
