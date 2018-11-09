package uk.gov.hmcts.reform.sscs.personalisation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.AppConstants;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;

@Component
public class AppealLapsedPersonalisation extends Personalisation<CcdNotificationWrapper> {

    @Override
    protected Map<String, String> create(SscsCaseDataWrapper responseWrapper) {
        Map<String, String> personalisation = super.create(responseWrapper);
        SscsCaseData ccdResponse = responseWrapper.getNewSscsCaseData();

        setRepresentaitveName(personalisation, ccdResponse);
        setRepresentativeDetails(personalisation, ccdResponse);

        return personalisation;
    }

    public Map<String, String> setRepresentativeDetails(Map<String, String> personalisation, SscsCaseData ccdResponse) {
        personalisation.put(AppConstants.REPRESENTATIVE_DETAILS_LITERAL, buildRepresentativeDetails(ccdResponse.getAppeal().getRep()));
        return personalisation;
    }

    private String buildRepresentativeDetails(Representative representative) {
        String hasRepresentative = (representative != null) ? "yes" : "no";

        StringBuilder representativeBuilder = new StringBuilder()
                .append("Have a representative: ")
                .append(hasRepresentative);

        if (representative != null) {
            representativeBuilder.append("\n\nName: ")
                .append(representative.getName().getFullNameNoTitle() + "\n\n")
                .append("Organisation: ")
                .append(getOptionalField(representative.getOrganisation(), "Not provided") + "\n\n")
                .append("Address: ")
                .append(representative.getAddress().getFullAddress() + "\n\n")
                .append("Email: ")
                .append(getOptionalField(representative.getContact().getEmail(), "Not provided") + "\n\n")
                .append("Phone: ")
                .append(getOptionalField(representative.getContact().getPhone(), "Not provided"))
                .toString();
        }
        return representativeBuilder.toString();
    }

    public Map<String, String> setRepresentativeName(Map<String, String> personalisation, SscsCaseData ccdResponse) {
        personalisation.put(AppConstants.REPRESENTATIVE_NAME, ccdResponse.getAppeal().getRep().getName().getFullNameNoTitle());
        return personalisation;
    }

    private String convertBooleanToRequiredText(Boolean value) {
        return value ? "Required" : "Not required";
    }

    private String getOptionalField(String field, String text) {
        return field == null || field.equals("null") || field.isEmpty() ? text : field;
    }
}
