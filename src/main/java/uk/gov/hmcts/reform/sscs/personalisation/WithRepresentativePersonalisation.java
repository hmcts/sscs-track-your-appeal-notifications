package uk.gov.hmcts.reform.sscs.personalisation;

import java.util.Map;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.config.AppConstants;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
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

        return personalisation;
    }

    public Map<String, String> setRepresentativeName(Map<String, String> personalisation, SscsCaseData sscsCaseData) {
        if (NotificationUtils.hasRepresentative(sscsCaseData.getAppeal())) {
            personalisation.put(AppConstants.REPRESENTATIVE_NAME,
                    SendNotificationHelper.getRepSalutation(sscsCaseData.getAppeal().getRep(), true));
        }
        
        return personalisation;
    }

}