package uk.gov.hmcts.reform.sscs.service.docmosis;

import static uk.gov.hmcts.reform.sscs.service.LetterUtils.getAddressToUseForLetter;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.config.DocmosisTemplatesConfig;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.docmosis.PdfCoverSheet;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.DocmosisPdfService;

@Service
public class PdfCoverSheetService {

    private final DocmosisPdfService docmosisPdfService;
    private final Map<String, String> templateNames;

    @Autowired
    public PdfCoverSheetService(DocmosisPdfService docmosisPdfService, DocmosisTemplatesConfig docmosisTemplatesConfig) {
        this.docmosisPdfService = docmosisPdfService;
        this.templateNames = docmosisTemplatesConfig.getTemplates();
    }

    public byte[] generateCoversheet(NotificationWrapper wrapper, SubscriptionType subscriptionType) {
        Address addressToUse = getAddressToUseForLetter(wrapper, subscriptionType);
        PdfCoverSheet pdfCoverSheet = new PdfCoverSheet(
                wrapper.getCaseId(),
                addressToUse.getLine1(),
                addressToUse.getLine2(),
                addressToUse.getTown(),
                addressToUse.getCounty(),
                addressToUse.getPostcode()
        );
        String templatePath = templateNames.get(wrapper.getNotificationType().getId());
        return docmosisPdfService.createPdf(pdfCoverSheet, templatePath);
    }

}
