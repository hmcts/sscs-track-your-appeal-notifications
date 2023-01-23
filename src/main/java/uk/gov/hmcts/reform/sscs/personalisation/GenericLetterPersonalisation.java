package uk.gov.hmcts.reform.sscs.personalisation;

import static uk.gov.hmcts.reform.sscs.config.PersonalisationMappingConstants.APPELLANT_NAME;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationMappingConstants.INFO_REQUEST_DETAIL;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationMappingConstants.IS_OTHER_PARTY;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationMappingConstants.IS_REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationMappingConstants.JOINT;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationMappingConstants.NAME;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationMappingConstants.REPRESENTATIVE_NAME;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.OTHER_PARTY;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.REPRESENTATIVE;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
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
        Map<String, Object> personalisation = super.create(responseWrapper, subscriptionWithType);

        SscsCaseData sscsCaseData = responseWrapper.getNewSscsCaseData();
        SubscriptionType subscriptionType = subscriptionWithType.getSubscriptionType();
        String appellantName = sscsCaseData.getAppeal().getAppellant().getName().getFullNameNoTitle();

        personalisation.put(APPELLANT_NAME, appellantName);
        personalisation.put(INFO_REQUEST_DETAIL, sscsCaseData.getGenericLetterText());
        personalisation.put(NAME, getName(subscriptionType, sscsCaseData, responseWrapper));

        if (REPRESENTATIVE.equals(subscriptionType) || OTHER_PARTY.equals(subscriptionType)) {
            String representativeName = subscriptionWithType.getEntity().getName().getFullNameNoTitle();

            personalisation.put(IS_REPRESENTATIVE, "Yes");
            personalisation.put(REPRESENTATIVE_NAME, representativeName);

            if (OTHER_PARTY.equals(subscriptionType)) {
                personalisation.put(IS_OTHER_PARTY, "Yes");
            }
        } else {
            personalisation.put(IS_REPRESENTATIVE, "No");
        }

        personalisation.put(JOINT, NotificationUtils.hasJointParty(sscsCaseData) ? JOINT : "");

        return personalisation;
    }
}
