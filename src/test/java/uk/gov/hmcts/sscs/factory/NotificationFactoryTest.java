package uk.gov.hmcts.sscs.factory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.sscs.domain.notify.NotificationType.APPEAL_RECEIVED;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.Subscription;
import uk.gov.hmcts.sscs.domain.notify.Notification;
import uk.gov.hmcts.sscs.domain.notify.Template;
import uk.gov.hmcts.sscs.personalisation.Personalisation;

public class NotificationFactoryTest {

    private NotificationFactory factory;

    private CcdResponse ccdResponse;

    @Mock
    private PersonalisationFactory personalisationFactory;

    @Mock
    private Personalisation personalisation;

    @Before
    public void setup() {
        initMocks(this);
        factory = new NotificationFactory(personalisationFactory);
        ccdResponse = new CcdResponse("SC/1234/5", new Subscription("Ronnie", "Scott", "Mr", "ABC",
                "test@testing.com", "07985858594", true, false), null, APPEAL_RECEIVED);
    }

    @Test
    public void buildNotificationFromCcdResponse() {
        when(personalisationFactory.apply(APPEAL_RECEIVED)).thenReturn(personalisation);
        when(personalisation.getTemplate(APPEAL_RECEIVED)).thenReturn(new Template("123", null));
        Notification result = factory.create(ccdResponse);

        assertEquals("123", result.getEmailTemplate());
        assertEquals("test@testing.com", result.getEmail());
        assertEquals("ABC", result.getAppealNumber());
    }

    @Test
    public void returnNullIfPersonalisationNotFound() {
        when(personalisationFactory.apply(APPEAL_RECEIVED)).thenReturn(null);
        Notification result = factory.create(ccdResponse);

        assertNull(result);
    }

    @Test
    public void returnNullIfEmptyPlaceholders() {
        when(personalisationFactory.apply(APPEAL_RECEIVED)).thenReturn(personalisation);
        when(personalisation.create(ccdResponse)).thenReturn(null);

        Notification result = factory.create(ccdResponse);

        assertNull(result);
    }
}
