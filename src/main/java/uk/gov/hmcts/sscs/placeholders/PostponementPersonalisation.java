package uk.gov.hmcts.sscs.placeholders;

import static com.google.common.collect.Maps.newHashMap;
import static uk.gov.hmcts.sscs.config.AppConstants.HEARING_CONTACT_DATE;
import static uk.gov.hmcts.sscs.domain.notify.NotificationType.POSTPONEMENT;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import uk.gov.hmcts.sscs.config.NotificationConfig;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.notify.Template;

public class PostponementPersonalisation extends Personalisation {

    public PostponementPersonalisation(NotificationConfig config) {
        super(config);
    }

    @Override
    protected Map<String, String> customise(CcdResponse response, Map<String, String> defaultMap) {
        Map<String, String> personalisation = newHashMap(defaultMap);

        // TODO: Set this to the actual event date once event story has been implemented
        ZonedDateTime z = ZonedDateTime.of(1900, 1, 1, 0, 0, 0, 0, ZoneId.of("GMT"));

        personalisation.put(HEARING_CONTACT_DATE, formatDate(z.plusWeeks(6)));
        return personalisation;
    }

    @Override
    public Template getTemplate() {
        return config.getTemplate(POSTPONEMENT.getId());
    }

}
