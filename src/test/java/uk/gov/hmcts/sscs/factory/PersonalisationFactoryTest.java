package uk.gov.hmcts.sscs.factory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.sscs.domain.notify.NotificationType.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.sscs.config.NotificationConfig;
import uk.gov.hmcts.sscs.placeholders.*;

public class PersonalisationFactoryTest {

    private PersonalisationFactory factory;

    @Mock
    private NotificationConfig config;

    @Before
    public void setup() {
        initMocks(this);
        factory = new PersonalisationFactory(config);
    }

    @Test
    public void createAppealReceivedPersonalisationWhenAppealReceivedNotification() {
        Personalisation result = factory.apply(APPEAL_RECEIVED);
        assertEquals(AppealReceivedPersonalisation.class, result.getClass());
    }

    @Test
    public void createDwpResponseReceivedPersonalisationWhenDwpResponseReceivedNotification() {
        Personalisation result = factory.apply(DWP_RESPONSE_RECEIVED);
        assertEquals(ResponseReceivedPersonalisation.class, result.getClass());
    }

    @Test
    public void createEvidenceReceivedPersonalisationWhenDwpResponseReceivedNotification() {
        Personalisation result = factory.apply(EVIDENCE_RECEIVED);
        assertEquals(EvidenceReceivedPersonalisation.class, result.getClass());
    }

    @Test
    public void createDefaultPersonalisationWhenHearingAdjournedNotification() {
        Personalisation result = factory.apply(ADJOURNED);
        assertEquals(DefaultPersonalisation.class, result.getClass());
    }

    @Test
    public void shouldReturnNullWhenNotificationTypeIsNull() {
        Personalisation personalisation = factory.apply(null);
        assertNull(personalisation);
    }
}
