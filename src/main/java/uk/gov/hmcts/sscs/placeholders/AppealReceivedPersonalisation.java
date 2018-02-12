package uk.gov.hmcts.sscs.placeholders;

import static com.google.common.collect.Maps.newHashMap;
import static java.time.temporal.ChronoUnit.DAYS;
import static uk.gov.hmcts.sscs.config.AppConstants.*;
import static uk.gov.hmcts.sscs.domain.notify.NotificationType.APPEAL_RECEIVED;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import uk.gov.hmcts.sscs.config.NotificationConfig;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.notify.Personalisation;
import uk.gov.hmcts.sscs.domain.notify.Template;

public class AppealReceivedPersonalisation extends Personalisation {

    private static final int MAX_DWP_RESPONSE_DAYS = 35;
    private static final String RESPONSE_DATE_FORMAT = "dd MMMM yyyy";

    public AppealReceivedPersonalisation(NotificationConfig config) {
        super(config);
    }

    @Override
    protected Map<String, String> customise(CcdResponse response, Map<String, String> defaultMap) {
        Map<String, String> personalisation = newHashMap(defaultMap);
        personalisation.put(FIRST_TIER_AGENCY_ACRONYM, DWP_ACRONYM);
        personalisation.put(FIRST_TIER_AGENCY_FULL_NAME, DWP_FUL_NAME);

        // TODO: Set this to the actual event date once event story has been implemented
        ZonedDateTime z = ZonedDateTime.of(1900, 1, 1, 0, 0, 0, 0, ZoneId.of("GMT"));

        String dwpResponseDateString = z.plus(MAX_DWP_RESPONSE_DAYS, DAYS).format(DateTimeFormatter.ofPattern(RESPONSE_DATE_FORMAT));
        personalisation.put(APPEAL_RESPOND_DATE, dwpResponseDateString);

        return personalisation;
    }

    @Override
    public Template getTemplate() {
        return config.getTemplate(APPEAL_RECEIVED.getId());
    }
}
