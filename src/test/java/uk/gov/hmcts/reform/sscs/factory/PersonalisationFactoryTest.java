package uk.gov.hmcts.reform.sscs.factory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.personalisation.*;

public class PersonalisationFactoryTest {

    @Mock
    private SubscriptionPersonalisation subscriptionPersonalisation;

    @Mock
    private Personalisation personalisation;

    @Mock
    private CohPersonalisation cohPersonalisation;

    @Mock
    private WithRepresentativePersonalisation withRepresentativePersonalisation;

    @Mock
    private SyaAppealCreatedAndReceivedPersonalisation syaAppealCreatedAndReceivedPersonalisation;

    @InjectMocks
    private PersonalisationFactory factory;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void createPersonalisationWhenNotificationApplied() {
        Personalisation result = factory.apply(DO_NOT_SEND);
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
    public void createRepsPersonalisationWhenAppealLapsedNotificationApplied() {
        Personalisation result = factory.apply(APPEAL_LAPSED_NOTIFICATION);
        assertEquals(withRepresentativePersonalisation, result);
    }

    @Test
    public void createRepsPersonalisationWhenHmtsLapsedNotificationApplied() {
        Personalisation result = factory.apply(DWP_APPEAL_LAPSED_NOTIFICATION);
        assertEquals(withRepresentativePersonalisation, result);
    }

    @Test
    public void createRepsPersonalisationWhenDwpLapsedNotificationApplied() {
        Personalisation result = factory.apply(HMCTS_APPEAL_LAPSED_NOTIFICATION);
        assertEquals(withRepresentativePersonalisation, result);
    }

    @Test
    public void createRepsPersonalisationWhenWithdrawnNotificationApplied() {
        Personalisation result = factory.apply(APPEAL_WITHDRAWN_NOTIFICATION);
        assertEquals(withRepresentativePersonalisation, result);
    }

    @Test
    public void createSyaAppealCreatedPersonalisationWhenAppealCreateNotificationApplied() {
        Personalisation result = factory.apply(SYA_APPEAL_CREATED_NOTIFICATION);
        assertEquals(syaAppealCreatedAndReceivedPersonalisation, result);
    }

    @Test
    public void createSyaAppealCreatedPersonalisationWhenValidAppealCreateNotificationApplied() {
        Personalisation result = factory.apply(VALID_APPEAL_CREATED);
        assertEquals(syaAppealCreatedAndReceivedPersonalisation, result);
    }

    @Test
    public void createRepsPersonalisationWhenAppealDormantNotificationApplied() {
        Personalisation result = factory.apply(APPEAL_DORMANT_NOTIFICATION);
        assertEquals(withRepresentativePersonalisation, result);
    }

    @Test
    public void createRepsPersonalisationWhenAdjournedNotificationApplied() {
        Personalisation result = factory.apply(ADJOURNED_NOTIFICATION);
        assertEquals(withRepresentativePersonalisation, result);
    }

    @Test
    public void createRepsPersonalisationWhenReceivedNotificationApplied() {
        Personalisation result = factory.apply(APPEAL_RECEIVED_NOTIFICATION);
        assertEquals(syaAppealCreatedAndReceivedPersonalisation, result);
    }

    @Test
    public void createRepsPersonalisationWhenCaseUpdatedValidAppealNotificationApplied() {
        Personalisation result = factory.apply(CASE_UPDATED);
        assertEquals(syaAppealCreatedAndReceivedPersonalisation, result);
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
    public void createRepsPersonalisationWhenEvidenceReminderNotificationApplied() {
        Personalisation result = factory.apply(EVIDENCE_REMINDER_NOTIFICATION);
        assertEquals(withRepresentativePersonalisation, result);
    }

    @Test
    public void createRepsPersonalisationWhenHearingReminderNotificationApplied() {
        Personalisation result = factory.apply(HEARING_REMINDER_NOTIFICATION);
        assertEquals(withRepresentativePersonalisation, result);
    }

    @Test
    public void createRepsPersonalisationWhenDwpResponseNotificationApplied() {
        Personalisation result = factory.apply(DWP_RESPONSE_RECEIVED_NOTIFICATION);
        assertEquals(withRepresentativePersonalisation, result);
    }

    @Test
    public void createRepsPersonalisationWhenDirectionIssuedNotificationApplied() {
        Personalisation result = factory.apply(DIRECTION_ISSUED);
        assertEquals(withRepresentativePersonalisation, result);
    }

    @Test
    public void shouldReturnNullWhenNotificationTypeIsNull() {
        Personalisation personalisation = factory.apply(null);
        assertNull(personalisation);
    }
}
