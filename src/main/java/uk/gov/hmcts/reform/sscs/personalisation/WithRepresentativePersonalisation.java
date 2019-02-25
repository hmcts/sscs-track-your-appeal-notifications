package uk.gov.hmcts.reform.sscs.personalisation;

import java.util.Map;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.AppConstants;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;

@Component
public class WithRepresentativePersonalisation extends Personalisation<CcdNotificationWrapper> {

    @Override
    protected Map<String, String> create(SscsCaseDataWrapper responseWrapper, SubscriptionWithType subscriptionWithType) {
        Map<String, String> personalisation = super.create(responseWrapper, subscriptionWithType);
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
}
