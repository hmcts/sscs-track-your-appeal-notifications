package uk.gov.hmcts.reform.sscs.config;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;


@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "text")
public class PersonalisationConfiguration {
    public Map<LanguagePreference, Map<String, String>> personalisation;
}
