package uk.gov.hmcts.reform.sscs.factory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.personalisation.CohPersonalisation;
import uk.gov.hmcts.reform.sscs.personalisation.Personalisation;
import uk.gov.hmcts.reform.sscs.personalisation.SubscriptionPersonalisation;
import uk.gov.hmcts.reform.sscs.personalisation.WithRepresentativePersonalisation;

public class PersonalisationFactoryTest {

    @Mock
    private SubscriptionPersonalisation subscriptionPersonalisation;

    @Mock
    private Personalisation personalisation;

    @Mock
    private CohPersonalisation cohPersonalisation;

    @Mock
    private WithRepresentativePersonalisation withRepresentativePersonalisation;

    @InjectMocks
    private PersonalisationFactory factory;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void createPersonalisationWhenNotificationApplied() {
        Personalisation result = factory.apply(APPEAL_RECEIVED_NOTIFICATION);
        assertEquals(personalisation, result);
    }

    @Test
    public void createSubscriptionPersonalisationWhenSubscriptionUpdatedNotificationApplied() {
        Personalisation result = factory.apply(SUBSCRIPTION_UPDATED_NOTIFICATION);
        assertEquals(subscriptionPersonalisation, result);
    }

    @Test
    public void createCohPersonalisationWhenQuestionRoundIssuedNotificationApplied() {
        Personalisation result = factory.apply(QUESTION_ROUND_ISSUED_NOTIFICATION);
        assertEquals(cohPersonalisation, result);
    }

    @Test
    public void createRepsPersonalisationWhenLapsedNotificationApplied() {
        Personalisation result = factory.apply(APPEAL_LAPSED_NOTIFICATION);
        assertEquals(withRepresentativePersonalisation, result);
    }

    @Test
    public void createRepsPersonalisationWhenWithdrawnNotificationApplied() {
        Personalisation result = factory.apply(APPEAL_WITHDRAWN_NOTIFICATION);
        assertEquals(withRepresentativePersonalisation, result);
    }

    @Test
    public void createRepsPersonalisationWhenAppealDormantNotificationApplied() {
        Personalisation result = factory.apply(APPEAL_DORMANT_NOTIFICATION);
        assertEquals(withRepresentativePersonalisation, result);
    }

    @Test
    public void createRepsPersonalisationWhenHearingPostponeNotificationApplied() {
        Personalisation result = factory.apply(POSTPONEMENT_NOTIFICATION);
        assertEquals(withRepresentativePersonalisation, result);
    }

    @Test
    public void createRepsPersonalisationWhenHearingBookedNotificationApplied() {
        Personalisation result = factory.apply(HEARING_BOOKED_NOTIFICATION);
        assertEquals(withRepresentativePersonalisation, result);
    }

    @Test
    public void shouldReturnNullWhenNotificationTypeIsNull() {
        Personalisation personalisation = factory.apply(null);
        assertNull(personalisation);
    }
}
