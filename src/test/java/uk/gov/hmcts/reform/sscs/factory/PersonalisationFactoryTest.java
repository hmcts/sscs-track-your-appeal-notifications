package uk.gov.hmcts.reform.sscs.factory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.personalisation.*;

@RunWith(JUnitParamsRunner.class)
public class PersonalisationFactoryTest {

    @Mock
    private SubscriptionPersonalisation subscriptionPersonalisation;

    @Mock
    private Personalisation personalisation;

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
    @Parameters({"APPEAL_LAPSED_NOTIFICATION", "APPEAL_LAPSED_NOTIFICATION", "DWP_APPEAL_LAPSED_NOTIFICATION",
        "HMCTS_APPEAL_LAPSED_NOTIFICATION", "APPEAL_WITHDRAWN_NOTIFICATION", "APPEAL_DORMANT_NOTIFICATION",
        "ADJOURNED_NOTIFICATION", "POSTPONEMENT_NOTIFICATION", "HEARING_BOOKED_NOTIFICATION", "EVIDENCE_REMINDER_NOTIFICATION",
        "HEARING_REMINDER_NOTIFICATION", "DWP_RESPONSE_RECEIVED_NOTIFICATION", "DWP_UPLOAD_RESPONSE_NOTIFICATION",
        "DIRECTION_ISSUED", "DECISION_ISSUED", "DIRECTION_ISSUED_WELSH", "DECISION_ISSUED_WELSH",
        "PROCESS_AUDIO_VIDEO", "PROCESS_AUDIO_VIDEO_WELSH", "ACTION_POSTPONEMENT_REQUEST", "ACTION_POSTPONEMENT_REQUEST_WELSH",
        "ISSUE_FINAL_DECISION", "ISSUE_FINAL_DECISION_WELSH", "ISSUE_ADJOURNMENT_NOTICE", "STRUCK_OUT", "NON_COMPLIANT_NOTIFICATION",
        "DEATH_OF_APPELLANT", "PROVIDE_APPOINTEE_DETAILS"})
    public void createRepsPersonalisationWhenNotificationApplied(NotificationEventType eventType) {
        Personalisation result = factory.apply(eventType);
        assertEquals(withRepresentativePersonalisation, result);
    }

    @Test
    @Parameters({"SYA_APPEAL_CREATED_NOTIFICATION", "VALID_APPEAL_CREATED", "APPEAL_RECEIVED_NOTIFICATION",
        "CASE_UPDATED"})
    public void createSyaAppealCreatedPersonalisationWhenNotificationApplied(NotificationEventType eventType) {
        Personalisation result = factory.apply(eventType);
        assertEquals(syaAppealCreatedAndReceivedPersonalisation, result);
    }

    @Test
    public void shouldReturnNullWhenNotificationTypeIsNull() {
        Personalisation personalisation = factory.apply(null);
        assertNull(personalisation);
    }

}
