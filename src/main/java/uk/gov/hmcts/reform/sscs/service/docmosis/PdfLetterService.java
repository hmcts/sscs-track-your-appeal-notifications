package uk.gov.hmcts.reform.sscs.service.docmosis;

import static uk.gov.hmcts.reform.sscs.config.AppConstants.*;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_RECEIVED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.personalisation.Personalisation.translateToWelshDate;
import static uk.gov.hmcts.reform.sscs.service.LetterUtils.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.config.DocmosisTemplatesConfig;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.docmosis.PdfCoverSheet;
import uk.gov.hmcts.reform.sscs.domain.notify.Notification;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.exception.NotificationClientRuntimeException;
import uk.gov.hmcts.reform.sscs.exception.PdfGenerationException;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.DocmosisPdfService;
import uk.gov.hmcts.reform.sscs.service.conversion.LocalDateToWelshStringConverter;

@Service
@Slf4j
public class PdfLetterService {
    private static final String SSCS_URL_LITERAL = "sscs_url";
    private static final String SSCS_URL = "www.gov.uk/appeal-benefit-decision";
    private static final String GENERATED_DATE_LITERAL = "generated_date";
    private static final String WELSH_GENERATED_DATE_LITERAL = "welsh_generated_date";
    private static final List<NotificationEventType> REQUIRES_TWO_COVERSHEET =
            Collections.singletonList(APPEAL_RECEIVED_NOTIFICATION);

    private final DocmosisPdfService docmosisPdfService;
    private final DocmosisTemplatesConfig docmosisTemplatesConfig;

    @Autowired
    public PdfLetterService(DocmosisPdfService docmosisPdfService, DocmosisTemplatesConfig docmosisTemplatesConfig) {
        this.docmosisPdfService = docmosisPdfService;
        this.docmosisTemplatesConfig = docmosisTemplatesConfig;
    }

    public byte[] buildCoversheet(NotificationWrapper wrapper, SubscriptionType subscriptionType) {
        try {
            byte[] coversheet = generateCoversheet(wrapper, subscriptionType);
            if (REQUIRES_TWO_COVERSHEET.contains(wrapper.getNotificationType())
                    && ArrayUtils.isNotEmpty(coversheet)) {
                return buildBundledLetter(addBlankPageAtTheEndIfOddPage(coversheet), coversheet);
            }
            return coversheet;
        } catch (IOException e) {
            String message = String.format("Cannot '%s' generate evidence coversheet to %s.",
                    wrapper.getNotificationType().getId(),
                    subscriptionType.name());
            log.error(message, e);
            throw new NotificationClientRuntimeException(message);
        }
    }

    private byte[] generateCoversheet(NotificationWrapper wrapper, SubscriptionType subscriptionType) {
        Address addressToUse = getAddressToUseForLetter(wrapper, subscriptionType);
        String name = getNameToUseForLetter(wrapper, subscriptionType);
        PdfCoverSheet pdfCoverSheet = new PdfCoverSheet(
                wrapper.getCaseId(),
                name,
                addressToUse.getLine1(),
                addressToUse.getLine2(),
                addressToUse.getTown(),
                addressToUse.getCounty(),
                addressToUse.getPostcode(),
                docmosisTemplatesConfig.getHmctsImgVal(),
                docmosisTemplatesConfig.getWelshHmctsImgVal());
        LanguagePreference languagePreference =
                wrapper.getSscsCaseDataWrapper().getNewSscsCaseData().getLanguagePreference();

        String templatePath = docmosisTemplatesConfig.getCoversheets().get(languagePreference)
                .get(wrapper.getNotificationType().getId());
        if (StringUtils.isBlank(templatePath)) {
            throw new PdfGenerationException(
                    String.format("There is no template for notificationType %s",
                            wrapper.getNotificationType().getId()),
                    new RuntimeException("Invalid notification type for docmosis coversheet."));
        }

        return docmosisPdfService.createPdf(pdfCoverSheet, templatePath);
    }


    public byte[] generateLetter(NotificationWrapper wrapper, Notification notification,
                                 SubscriptionType subscriptionType) {
        if (StringUtils.isNotBlank(notification.getDocmosisLetterTemplate())) {

            Map<String, Object> placeholders = new HashMap<>(notification.getPlaceholders());
            placeholders.put(SSCS_URL_LITERAL, SSCS_URL);
            placeholders.put(GENERATED_DATE_LITERAL, LocalDateTime.now().toLocalDate().toString());
            translateToWelshDate(LocalDateTime.now().toLocalDate(), wrapper.getNewSscsCaseData(), (value) -> placeholders.put(WELSH_GENERATED_DATE_LITERAL, value));

            placeholders.put(ADDRESS_NAME, truncateAddressLine(getNameToUseForLetter(wrapper, subscriptionType)));

            Address addressToUse = getAddressToUseForLetter(wrapper, subscriptionType);
            buildRecipientAddressPlaceholders(addressToUse, placeholders);
            placeholders.put(docmosisTemplatesConfig.getHmctsImgKey(), docmosisTemplatesConfig.getHmctsImgVal());
            placeholders.put(docmosisTemplatesConfig.getWelshHmctsImgKey(), docmosisTemplatesConfig.getWelshHmctsImgVal());

            if (wrapper.getNewSscsCaseData().isLanguagePreferenceWelsh()) {
                placeholders.put(docmosisTemplatesConfig.getHmctsWelshImgKey(),
                        docmosisTemplatesConfig.getHmctsWelshImgVal());
                placeholders.put(WELSH_GENERATED_DATE_LITERAL, LocalDateToWelshStringConverter.convert(LocalDate.now()));
            }
            return docmosisPdfService.createPdfFromMap(placeholders, notification.getDocmosisLetterTemplate());
        }
        return new byte[0];
    }

    private void buildRecipientAddressPlaceholders(Address address, Map<String, Object> placeholders) {
        String[] lines = lines(address);

        if (lines.length >= 1) {
            placeholders.put(LETTER_ADDRESS_LINE_1, truncateAddressLine(defaultToEmptyStringIfNull(lines[0])));
        }
        if (lines.length >= 2) {
            placeholders.put(LETTER_ADDRESS_LINE_2, truncateAddressLine(defaultToEmptyStringIfNull(lines[1])));
        }
        if (lines.length >= 3) {
            placeholders.put(LETTER_ADDRESS_LINE_3, truncateAddressLine(defaultToEmptyStringIfNull(lines[2])));
        }
        if (lines.length >= 4) {
            placeholders.put(LETTER_ADDRESS_LINE_4, truncateAddressLine(defaultToEmptyStringIfNull(lines[3])));
        }
        if (lines.length >= 5) {
            placeholders.put(LETTER_ADDRESS_POSTCODE, truncateAddressLine(defaultToEmptyStringIfNull(lines[4])));
        }
    }

    private static String[] lines(Address address) {
        return Stream.of(address.getLine1(), address.getLine2(), address.getTown(), address.getCounty(), address.getPostcode())
                .filter(x -> x != null)
                .toArray(String[]::new);
    }

    private static String defaultToEmptyStringIfNull(String value) {
        return (value == null) ? StringUtils.EMPTY : value;
    }

    private static String truncateAddressLine(String addressLine) {
        return addressLine != null && addressLine.length() > 45  ? addressLine.substring(0, 45) : addressLine;
    }

}

