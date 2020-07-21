package uk.gov.hmcts.reform.sscs.config;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("integration")
public class PersonalisationConfigurationTest {
    @Autowired
    private PersonalisationConfiguration personalisationConfiguration;

    @Test
    public void testPersonalisation() {
        personalisationConfiguration.getPersonalisation().get(LanguagePreference.ENGLISH);
    }
}