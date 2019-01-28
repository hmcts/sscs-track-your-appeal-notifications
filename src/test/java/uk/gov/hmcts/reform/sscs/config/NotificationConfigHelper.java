package uk.gov.hmcts.reform.sscs.config;

import org.springframework.core.env.Environment;

public class NotificationConfigHelper {

    private NotificationConfigHelper() {
        // Hidden
    }

    public static NotificationConfig create(Environment env) {
        return new NotificationConfig(env);
    }
}
