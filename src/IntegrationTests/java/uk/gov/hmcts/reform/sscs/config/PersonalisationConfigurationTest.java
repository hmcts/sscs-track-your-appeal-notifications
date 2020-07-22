package uk.gov.hmcts.reform.sscs.config;

import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;


public class PersonalisationConfigurationTest {
    @Autowired
    private PersonalisationConfiguration personalisationConfiguration;

    public void testPersonalisation() {
        personalisationConfiguration.getPersonalisation().get(LanguagePreference.ENGLISH);
    }
}