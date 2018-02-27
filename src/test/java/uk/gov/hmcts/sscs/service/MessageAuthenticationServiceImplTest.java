package uk.gov.hmcts.sscs.service;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.sscs.domain.Subscription;

@RunWith(MockitoJUnitRunner.class)
public class MessageAuthenticationServiceImplTest {

    private MessageAuthenticationServiceImpl service;

    @Before
    public void setUp() throws Exception {
        service = new MessageAuthenticationServiceImpl("our-big-secret");
    }

    @Test
    public void shouldGenerateMacUsingSecureAlgorithmAndReturnBenefitType() {

        Subscription subscription = new Subscription();
        subscription.setAppealNumber("u6ml9e");

        String startEncryptedToken = service.generateToken("3", "002").substring(0, 9);

        assertEquals("M3wwMDJ8M", startEncryptedToken);
    }
}
