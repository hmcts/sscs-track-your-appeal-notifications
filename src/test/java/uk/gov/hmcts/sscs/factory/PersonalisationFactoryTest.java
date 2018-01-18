package uk.gov.hmcts.sscs.factory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.sscs.domain.notify.NotificationType.APPEAL_RECEIVED;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.sscs.config.NotificationConfig;
import uk.gov.hmcts.sscs.domain.notify.Personalisation;

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
        Personalisation personalisation = factory.apply(APPEAL_RECEIVED);

        assertEquals(personalisation.getTemplate().getEmailTemplateId(), "dd955503-42f4-45f8-a692-39377a0f340f");
    }

    @Test
    public void shouldReturnNullWhenNotificationTypeIsNull() {
        Personalisation personalisation = factory.apply(null);

        assertNull(personalisation);
    }
}
