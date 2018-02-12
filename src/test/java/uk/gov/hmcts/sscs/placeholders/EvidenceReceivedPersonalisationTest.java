package uk.gov.hmcts.sscs.placeholders;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.sscs.config.AppConstants.EVIDENCE_RECEIVED_DATE_LITERAL;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.sscs.domain.CcdResponse;

public class EvidenceReceivedPersonalisationTest {

    private EvidenceReceivedPersonalisation personalisation;

    @Before
    public void setup() {
        personalisation = new EvidenceReceivedPersonalisation(null);
    }

    @Test
    public void customiseAppealReceivedPersonalisation() {
        Map<String, String> personalisationMap = new HashMap<>();

        Map<String, String> result = personalisation.customise(new CcdResponse(), personalisationMap);

        assertEquals("01 January 1900", result.get(EVIDENCE_RECEIVED_DATE_LITERAL));
    }
}
