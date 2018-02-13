package uk.gov.hmcts.sscs.placeholders;

import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.sscs.config.AppConstants.HEARING_CONTACT_DATE;
import static uk.gov.hmcts.sscs.domain.notify.NotificationType.DWP_RESPONSE_RECEIVED;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.sscs.config.NotificationConfig;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.Subscription;

public class PostponementPersonalisationTest {

    private PostponementPersonalisation personalisation;

    @Mock
    private NotificationConfig config;

    @Before
    public void setup() {
        initMocks(this);
        personalisation = new PostponementPersonalisation(config);
    }

    @Test
    public void customiseAppealReceivedPersonalisation() {
        Map<String, String> personalisationMap = new HashMap<>();

        Subscription appellantSubscription = new Subscription("Harry", "Kane", "Mr", "GLSCRR", "test@email.com",
                "07983495065", true, false);

        Map<String, String> result = personalisation.customise(new CcdResponse("1234", appellantSubscription, null, DWP_RESPONSE_RECEIVED),
                personalisationMap);

        assertEquals("12 February 1900", result.get(HEARING_CONTACT_DATE));
    }
}
