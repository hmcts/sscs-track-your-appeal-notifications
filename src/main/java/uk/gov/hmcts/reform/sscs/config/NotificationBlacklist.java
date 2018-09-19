package uk.gov.hmcts.reform.sscs.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "gov.uk.notification.api")
public class NotificationBlacklist {

    private final List<String> testRecipients = new ArrayList<>();

    public List<String> getTestRecipients() {
        return testRecipients;
    }
}
