package uk.gov.hmcts.reform.sscs.personalisation;

import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.AppConstants;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.NotificationUtils;

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
        if (NotificationUtils.hasRepresentative(sscsCaseData.getAppeal()) && isValidReps(sscsCaseData.getAppeal().getRep())) {
            personalisation.put(AppConstants.REPRESENTATIVE_NAME, 
                    getDefaultName(sscsCaseData.getAppeal().getRep().getName(),
                    AppConstants.REP_SALUTATION));
        }
        return personalisation;
    }

    private boolean isValidReps(Representative representative) {
        if (representative == null) {
            return false;
        }

        Name repName = representative.getName();

        return (null != repName && StringUtils.isNotBlank(repName.getFirstName()) && StringUtils.isNotBlank(repName.getLastName()))
                    || StringUtils.isNotBlank(representative.getOrganisation());
    }
}
