package uk.gov.hmcts.sscs.placeholders;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.sscs.config.AppConstants.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.sscs.domain.CcdResponse;

public class AppealReceivedPersonalisationTest {

    private AppealReceivedPersonalisation personalisation;

    @Before
    public void setup() {
        personalisation = new AppealReceivedPersonalisation(null);
    }

    @Test
    public void customiseAppealReceivedPersonalisation() {
        Map<String, String> personalisationMap = new HashMap<>();

        Map<String, String> result = personalisation.customise(new CcdResponse(), personalisationMap);

        assertEquals(DWP_ACRONYM, result.get(FIRST_TIER_AGENCY_ACRONYM));
        assertEquals(DWP_FUL_NAME, result.get(FIRST_TIER_AGENCY_FULL_NAME));
        assertEquals("05 February 1900", result.get(APPEAL_RESPOND_DATE));
    }
}
