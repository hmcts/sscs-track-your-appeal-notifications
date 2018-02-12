package uk.gov.hmcts.sscs.placeholders;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.sscs.config.AppConstants.*;
import static uk.gov.hmcts.sscs.domain.notify.NotificationType.DWP_RESPONSE_RECEIVED;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.sscs.config.NotificationConfig;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.Subscription;
import uk.gov.hmcts.sscs.domain.notify.Link;

public class ResponseReceivedPersonalisationTest {

    private ResponseReceivedPersonalisation personalisation;

    @Mock
    private NotificationConfig config;

    @Before
    public void setup() {
        initMocks(this);
        personalisation = new ResponseReceivedPersonalisation(config);
        when(config.getEvidenceSubmissionInfoLink()).thenReturn(new Link("http://link.com/appeal_id"));
    }

    @Test
    public void customiseAppealReceivedPersonalisation() {
        Map<String, String> personalisationMap = new HashMap<>();

        Subscription appellantSubscription = new Subscription("Harry", "Kane", "Mr", "GLSCRR", "test@email.com",
                "07983495065", true, false);

        Map<String, String> result = personalisation.customise(new CcdResponse("1234", appellantSubscription, null, DWP_RESPONSE_RECEIVED),
                personalisationMap);

        assertEquals(DWP_ACRONYM, result.get(FIRST_TIER_AGENCY_ACRONYM));
        assertEquals(DWP_FUL_NAME, result.get(FIRST_TIER_AGENCY_FULL_NAME));
        assertEquals("http://link.com/GLSCRR", result.get(SUBMIT_EVIDENCE_LINK_LITERAL));
    }
}
