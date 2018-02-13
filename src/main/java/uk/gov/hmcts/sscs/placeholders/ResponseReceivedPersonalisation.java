package uk.gov.hmcts.sscs.placeholders;

import static com.google.common.collect.Maps.newHashMap;
import static uk.gov.hmcts.sscs.config.AppConstants.*;
import static uk.gov.hmcts.sscs.domain.notify.NotificationType.DWP_RESPONSE_RECEIVED;

import java.util.Map;
import uk.gov.hmcts.sscs.config.NotificationConfig;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.notify.Template;

public class ResponseReceivedPersonalisation extends Personalisation {

    public ResponseReceivedPersonalisation(NotificationConfig config) {
        super(config);
    }

    @Override
    protected Map<String, String> customise(CcdResponse response, Map<String, String> defaultMap) {
        Map<String, String> personalisation = newHashMap(defaultMap);
        personalisation.put(FIRST_TIER_AGENCY_ACRONYM, DWP_ACRONYM);
        personalisation.put(FIRST_TIER_AGENCY_FULL_NAME, DWP_FUL_NAME);

        personalisation.put(SUBMIT_EVIDENCE_LINK_LITERAL, config.getEvidenceSubmissionInfoLink().replace(APPEAL_ID, response.getAppellantSubscription().getAppealNumber()));
        return personalisation;
    }

    @Override
    public Template getTemplate() {
        return config.getTemplate(DWP_RESPONSE_RECEIVED.getId());
    }

}
