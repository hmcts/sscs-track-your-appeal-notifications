package uk.gov.hmcts.reform.sscs.service.docmosis;

import static uk.gov.hmcts.reform.sscs.config.AppConstants.*;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.POSTCODE_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.LetterUtils.getAddressToUseForLetter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.config.DocmosisTemplatesConfig;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.docmosis.PdfCoverSheet;
import uk.gov.hmcts.reform.sscs.domain.notify.Notification;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.exception.PdfGenerationException;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.DocmosisPdfService;
import uk.gov.hmcts.reform.sscs.service.LetterUtils;

@Service
@Slf4j
public class PdfLetterService {
    private static final String SSCS_URL_LITERAL = "sscs_url";
    public static final String SSCS_URL = "http://www.tribunals.gov.uk/";
    public static final String GENERATED_DATE_LITERAL = "generated_date";

    private final DocmosisPdfService docmosisPdfService;
    private final DocmosisTemplatesConfig docmosisTemplatesConfig;

    @Autowired
    public PdfLetterService(DocmosisPdfService docmosisPdfService, DocmosisTemplatesConfig docmosisTemplatesConfig) {
        this.docmosisPdfService = docmosisPdfService;
        this.docmosisTemplatesConfig = docmosisTemplatesConfig;
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
        String templatePath = docmosisTemplatesConfig.getCoversheets().get(wrapper.getNotificationType().getId());
        if (StringUtils.isBlank(templatePath)) {
            throw new PdfGenerationException(
                    String.format("There is no template for notificationType %s",
                            wrapper.getNotificationType().getId()),
                    new RuntimeException("Invalid notification type for docmosis coversheet."));
        }
        return docmosisPdfService.createPdf(pdfCoverSheet, templatePath);
    }


    public byte[] generateLetter(NotificationWrapper wrapper, Notification notification, SubscriptionType subscriptionType) {
        try {
            if (NotificationEventType.APPEAL_RECEIVED_NOTIFICATION.getId().equalsIgnoreCase(wrapper.getNotificationType().getId())) {


                Address addressToUse = getAddressToUseForLetter(wrapper, subscriptionType);
                Map<String, String> placeholders = notification.getPlaceholders();
                placeholders.put(SSCS_URL_LITERAL, SSCS_URL);
                placeholders.put(GENERATED_DATE_LITERAL, LocalDateTime.now().toLocalDate().toString());
                placeholders.put(docmosisTemplatesConfig.getHmctsImgKey(), docmosisTemplatesConfig.getHmctsImgVal());
                placeholders.put("address_line1", addressToUse.getLine1());
                placeholders.put("address_line2", addressToUse.getLine2() == null ? " " : addressToUse.getLine2());
                placeholders.put("address_line3", addressToUse.getTown() == null ? " " : addressToUse.getTown());
                placeholders.put("address_line4", addressToUse.getCounty() == null ? " " : addressToUse.getCounty());
                placeholders.put(POSTCODE_LITERAL, addressToUse.getPostcode());
                byte[] letter = docmosisPdfService.createPdf(placeholders, notification.getDocmosisLetterTemplate());

                byte[] coversheet = LetterUtils.addBlankPageAtTheEndIfOddPage(generateCoversheet(wrapper, subscriptionType));
                return LetterUtils.buildBundledLetter(LetterUtils.buildBundledLetter(letter, coversheet), coversheet);
            }
        } catch (IOException e) {
            log.error("Cannot generate letter", e);
        }

        return null;
    }

}

