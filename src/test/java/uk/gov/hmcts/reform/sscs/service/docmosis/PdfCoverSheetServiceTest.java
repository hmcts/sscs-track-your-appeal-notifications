package uk.gov.hmcts.reform.sscs.service.docmosis;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_RECEIVED_NOTIFICATION;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.DocmosisTemplatesConfig;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.docmosis.PdfCoverSheet;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.DocmosisPdfService;
import uk.gov.hmcts.reform.sscs.service.NotificationServiceTest;

@RunWith(JUnitParamsRunner.class)
public class PdfCoverSheetServiceTest {

    private final DocmosisPdfService docmosisPdfService = mock(DocmosisPdfService.class);

    private static final Map<String, String> TEMPLATE_NAMES = new ConcurrentHashMap<>();
    private static final DocmosisTemplatesConfig DOCMOSIS_TEMPLATES_CONFIG = new DocmosisTemplatesConfig();

    static {
        TEMPLATE_NAMES.put(APPEAL_RECEIVED_NOTIFICATION.getId(), "my01.doc");
        DOCMOSIS_TEMPLATES_CONFIG.setTemplates(TEMPLATE_NAMES);
    }


    private final PdfCoverSheetService pdfCoverSheetService =
            new PdfCoverSheetService(docmosisPdfService, DOCMOSIS_TEMPLATES_CONFIG);

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

        pdfCoverSheetService.generateCoversheet(wrapper, subscriptionType);

        Address address = subscriptionType.equals(SubscriptionType.APPELLANT)
                ? APPELLANT.getAddress() : REPRESENTATIVE.getAddress();
        PdfCoverSheet pdfCoverSheet = new PdfCoverSheet(wrapper.getCaseId(),
                address.getLine1(),
                address.getLine2(),
                address.getTown(),
                address.getCounty(),
                address.getPostcode()
        );
        verify(docmosisPdfService).createPdf(eq(pdfCoverSheet), eq("my01.doc"));
    }
}
