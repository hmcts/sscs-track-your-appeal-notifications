package uk.gov.hmcts.sscs.placeholders;

import static com.google.common.collect.Maps.newHashMap;
import static uk.gov.hmcts.sscs.config.AppConstants.EVIDENCE_RECEIVED_DATE_LITERAL;
import static uk.gov.hmcts.sscs.config.AppConstants.RESPONSE_DATE_FORMAT;
import static uk.gov.hmcts.sscs.domain.notify.NotificationType.EVIDENCE_RECEIVED;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import uk.gov.hmcts.sscs.config.NotificationConfig;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.notify.Template;

public class EvidenceReceivedPersonalisation extends Personalisation {

    public EvidenceReceivedPersonalisation(NotificationConfig config) {
        super(config);
    }

    @Override
    protected Map<String, String> customise(CcdResponse response, Map<String, String> defaultMap) {
        Map<String, String> personalisation = newHashMap(defaultMap);

        // TODO: Set this to the actual event date once event story has been implemented
        ZonedDateTime z = ZonedDateTime.of(1900, 1, 1, 0, 0, 0, 0, ZoneId.of("GMT"));
        String evidenceReceivedDateString = z.format(DateTimeFormatter.ofPattern(RESPONSE_DATE_FORMAT));
        personalisation.put(EVIDENCE_RECEIVED_DATE_LITERAL, evidenceReceivedDateString);

        return personalisation;
    }

    @Override
    public Template getTemplate() {
        return config.getTemplate(EVIDENCE_RECEIVED.getId());
    }

}