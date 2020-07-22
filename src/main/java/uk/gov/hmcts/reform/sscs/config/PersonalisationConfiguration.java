package uk.gov.hmcts.reform.sscs.config;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;

@Getter
@Setter
@Component
@Configuration
public class PersonalisationConfiguration {
    public Map<LanguagePreference, Map<PersonalisationKey, String>> personalisation;
}
