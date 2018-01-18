package uk.gov.hmcts.sscs.placeholders;

import static com.google.common.collect.Maps.newHashMap;

import java.util.Map;
import uk.gov.hmcts.sscs.config.NotificationConfig;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.notify.Personalisation;
import uk.gov.hmcts.sscs.domain.notify.Template;
import uk.gov.hmcts.sscs.domain.notify.TemplateId;

public class AppealReceivedPersonalisation extends Personalisation {

    public static final String FIRST_TIER_AGENCY_ACRONYM = "first_tier_agency_acronym";
    public static final String FIRST_TIER_AGENCY_FULL_NAME = "first_tier_agency_full_name";
    public static final String DWP_ACRONYM = "DWP";
    public static final String DWP_FUL_NAME = "Department for Work and Pensions";

    public AppealReceivedPersonalisation(NotificationConfig config) {
        super(config);
    }

    @Override
    protected Map<String, String> customise(CcdResponse response, Map<String, String> defaultMap) {
        Map<String, String> personalisation = newHashMap(defaultMap);
        personalisation.put(FIRST_TIER_AGENCY_ACRONYM, DWP_ACRONYM);
        personalisation.put(FIRST_TIER_AGENCY_FULL_NAME, DWP_FUL_NAME);

        return personalisation;
    }

    @Override
    public Template getTemplate() {
        return TemplateId.APPEAL_RECEIVED.template;
    }
}
