package uk.gov.hmcts.reform.sscs.factory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.personalisation.CohPersonalisation;
import uk.gov.hmcts.reform.sscs.personalisation.Personalisation;
import uk.gov.hmcts.reform.sscs.personalisation.SubscriptionPersonalisation;
import uk.gov.hmcts.reform.sscs.personalisation.CohPersonalisation;
import uk.gov.hmcts.reform.sscs.personalisation.Personalisation;
import uk.gov.hmcts.reform.sscs.personalisation.SubscriptionPersonalisation;

public class PersonalisationFactoryTest {

    @Mock
    private SubscriptionPersonalisation subscriptionPersonalisation;

    @Mock
    private Personalisation personalisation;

    @Mock
    private CohPersonalisation cohPersonalisation;

    @InjectMocks
    private PersonalisationFactory factory;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void createPersonalisationWhenNotificationApplied() {
        Personalisation result = factory.apply(APPEAL_RECEIVED);
        assertEquals(personalisation, result);
    }

    @Test
    public void createSubscriptionPersonalisationWhenSubscriptionUpdatedNotificationApplied() {
        Personalisation result = factory.apply(SUBSCRIPTION_UPDATED);
        assertEquals(subscriptionPersonalisation, result);
    }

    @Test
    public void createCohPersonalisationWhenQuestionRoundIssuedNotificationApplied() {
        Personalisation result = factory.apply(QUESTION_ROUND_ISSUED);
        assertEquals(cohPersonalisation, result);
    }

    @Test
    public void shouldReturnNullWhenNotificationTypeIsNull() {
        Personalisation personalisation = factory.apply(null);
        assertNull(personalisation);
    }
}
