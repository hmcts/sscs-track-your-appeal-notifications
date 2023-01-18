package uk.gov.hmcts.reform.sscs.personalisation;

import static uk.gov.hmcts.reform.sscs.config.PersonalisationMappingConstants.APPELLANT_NAME;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationMappingConstants.INFO_REQUEST_DETAIL;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationMappingConstants.IS_OTHER_PARTY;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationMappingConstants.JOINT;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationMappingConstants.NAME;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationMappingConstants.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationMappingConstants.REPRESENTATIVE_NAME;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.NotificationUtils;

@Slf4j
@Component
public class GenericLetterPersonalisation extends Personalisation<CcdNotificationWrapper> {

    @Override
    public Map<String, Object> create(final SscsCaseDataWrapper responseWrapper, final SubscriptionWithType subscriptionWithType) {
        var personalisation = super.create(responseWrapper, subscriptionWithType);

        var sscsCaseData = responseWrapper.getNewSscsCaseData();

        personalisation.put(APPELLANT_NAME, sscsCaseData.getAppeal().getAppellant().getName().getFullNameNoTitle());
        personalisation.put(INFO_REQUEST_DETAIL, sscsCaseData.getGenericLetterText());
        personalisation.put(NAME, getName(subscriptionWithType.getSubscriptionType(), sscsCaseData, responseWrapper));

        if (SubscriptionType.REPRESENTATIVE.equals(subscriptionWithType.getSubscriptionType())
                || SubscriptionType.OTHER_PARTY.equals(subscriptionWithType.getSubscriptionType())) {
            personalisation.put(REPRESENTATIVE, "Yes");
            personalisation.put(REPRESENTATIVE_NAME, subscriptionWithType.getEntity().getName().getFullNameNoTitle());

            if (SubscriptionType.OTHER_PARTY.equals(subscriptionWithType.getSubscriptionType())) {
                personalisation.put(IS_OTHER_PARTY, "Yes");
            }

        } else {
            personalisation.put(REPRESENTATIVE, "No");
        }

        personalisation.put(JOINT, NotificationUtils.hasJointParty(responseWrapper.getNewSscsCaseData()) ? JOINT : "");

        return personalisation;
    }
}
