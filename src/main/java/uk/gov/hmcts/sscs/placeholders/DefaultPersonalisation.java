package uk.gov.hmcts.sscs.placeholders;

import static com.google.common.collect.Maps.newHashMap;

import java.util.Map;
import uk.gov.hmcts.sscs.config.NotificationConfig;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.notify.NotificationType;
import uk.gov.hmcts.sscs.domain.notify.Template;

public class DefaultPersonalisation extends Personalisation {

    private NotificationType notificationType;

    public DefaultPersonalisation(NotificationConfig config, NotificationType notificationType) {
        super(config);
        this.notificationType = notificationType;
    }

    @Override
    protected Map<String, String> customise(CcdResponse response, Map<String, String> defaultMap) {
        return newHashMap(defaultMap);
    }

    @Override
    public Template getTemplate() {
        return config.getTemplate(notificationType.getId());
    }

}
