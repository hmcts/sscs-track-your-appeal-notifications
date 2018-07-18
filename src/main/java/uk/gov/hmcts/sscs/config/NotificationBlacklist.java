package uk.gov.hmcts.sscs.config;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "gov.uk.notification.api")
public class NotificationBlacklist {

    private static final Logger LOG = getLogger(NotificationBlacklist.class);

    private final List<String> testRecipients = new ArrayList<>();

    public List<String> getTestRecipients() {
        LOG.info("Blacklist recipients: " + testRecipients.toString());

        return testRecipients;
    }
}
