package uk.gov.hmcts.reform.sscs.personalisation;

import static uk.gov.hmcts.reform.sscs.config.AppConstants.EVIDENCE_RECEIVED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.RESPONSE_DATE_FORMAT;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.WELSH_EVIDENCE_RECEIVED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.EVIDENCE_RECEIVED_NOTIFICATION;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.config.AppConstants;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.NotificationUtils;
import uk.gov.hmcts.reform.sscs.service.SendNotificationHelper;

@Component
public class WithRepresentativePersonalisation extends Personalisation<CcdNotificationWrapper> {

    @Override
    protected Map<String, String> create(SscsCaseDataWrapper responseWrapper, SubscriptionWithType subscriptionWithType) {
        Map<String, String> personalisation = super.create(responseWrapper, subscriptionWithType);
        SscsCaseData ccdResponse = responseWrapper.getNewSscsCaseData();

        setRepresentativeName(personalisation, ccdResponse);
        setWelshEvidenceReceivedDate(personalisation, ccdResponse, responseWrapper.getNotificationEventType());

        return personalisation;
    }

    public Map<String, String> setWelshEvidenceReceivedDate(Map<String, String> personalisation, SscsCaseData ccdResponse,
                                              NotificationEventType notificationEventType) {

        if (notificationEventType.equals(EVIDENCE_RECEIVED_NOTIFICATION)) {
            if (ccdResponse.getEvidence() != null && ccdResponse.getEvidence().getDocuments() != null
                    && !ccdResponse.getEvidence().getDocuments().isEmpty()) {
                LocalDate evidenceDateTimeFormatted = ccdResponse.getEvidence().getDocuments().get(0).getValue()
                        .getEvidenceDateTimeFormatted();
                personalisation.put(EVIDENCE_RECEIVED_DATE_LITERAL, localDateFunction.apply(evidenceDateTimeFormatted));
                translateToWelshDate(evidenceDateTimeFormatted, ccdResponse, value ->
                        personalisation.put(WELSH_EVIDENCE_RECEIVED_DATE_LITERAL, value)
                );
            } else {
                personalisation.put(EVIDENCE_RECEIVED_DATE_LITERAL, StringUtils.EMPTY);
            }
        }
        return personalisation;
    }

    public Map<String, String> setRepresentativeName(Map<String, String> personalisation, SscsCaseData sscsCaseData) {
        if (NotificationUtils.hasRepresentative(sscsCaseData.getAppeal())) {
            personalisation.put(AppConstants.REPRESENTATIVE_NAME,
                    SendNotificationHelper.getRepSalutation(sscsCaseData.getAppeal().getRep(), true));
        }

        return personalisation;
    }

    private Function<LocalDate, String> localDateFunction =
            date -> date.format(DateTimeFormatter.ofPattern(RESPONSE_DATE_FORMAT));

}