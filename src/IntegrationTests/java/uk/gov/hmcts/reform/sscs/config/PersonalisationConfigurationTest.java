package uk.gov.hmcts.reform.sscs.config;


import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;


public class PersonalisationConfigurationTest {
    @Autowired
    private PersonalisationConfiguration personalisationConfiguration;

    @Test
    public void testPersonalisation() {
        personalisationConfiguration.getPersonalisation().get(LanguagePreference.ENGLISH);
    }
}