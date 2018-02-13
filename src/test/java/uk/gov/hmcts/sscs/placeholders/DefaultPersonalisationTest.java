package uk.gov.hmcts.sscs.placeholders;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.sscs.config.AppConstants.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.notify.NotificationType;

public class DefaultPersonalisationTest {

    private DefaultPersonalisation personalisation;

    @Before
    public void setup() {
        personalisation = new DefaultPersonalisation(null, NotificationType.ADJOURNED);
    }

    @Test
    public void customiseAppealReceivedPersonalisation() {
        Map<String, String> personalisationMap = new HashMap<>();

        Map<String, String> result = personalisation.customise(new CcdResponse(), personalisationMap);

        assertEquals(0, result.size());
    }
}
