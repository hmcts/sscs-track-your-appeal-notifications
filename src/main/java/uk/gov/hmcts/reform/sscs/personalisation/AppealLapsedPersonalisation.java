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

        setRepresentativeName(personalisation, ccdResponse);

        return personalisation;
    }

    public Map<String, String> setRepresentativeName(Map<String, String> personalisation, SscsCaseData sscsCaseData) {
        if (isValidReps(sscsCaseData.getAppeal().getRep())) {
            personalisation.put(AppConstants.REPRESENTATIVE_NAME, String.format("%s %s",
                    sscsCaseData.getAppeal().getRep().getName().getFirstName(),
                    sscsCaseData.getAppeal().getRep().getName().getLastName()));
        }
        return personalisation;
    }

    private boolean isValidReps(Representative representative) {
        return null != (representative) && null != representative.getName();
    }

    private String getOptionalField(String field, String text) {
        return field == null || field.equals("null") || field.isEmpty() ? text : field;
    }
}
