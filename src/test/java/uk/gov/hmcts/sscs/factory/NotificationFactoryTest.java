package uk.gov.hmcts.sscs.factory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.sscs.domain.notify.NotificationType.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.sscs.config.NotificationConfig;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.domain.Subscription;
import uk.gov.hmcts.sscs.domain.notify.Link;
import uk.gov.hmcts.sscs.domain.notify.Notification;
import uk.gov.hmcts.sscs.domain.notify.Template;
import uk.gov.hmcts.sscs.personalisation.Personalisation;
import uk.gov.hmcts.sscs.personalisation.SubscriptionPersonalisation;

public class NotificationFactoryTest {

    private NotificationFactory factory;

    private CcdResponseWrapper wrapper;

    private Personalisation personalisation;

    private SubscriptionPersonalisation subscriptionPersonalisation;

    @Mock
    private PersonalisationFactory personalisationFactory;

    @Mock
    private NotificationConfig config;


    @Before
    public void setup() {
        initMocks(this);
        personalisation = new Personalisation(config);
        subscriptionPersonalisation = new SubscriptionPersonalisation(config);
        factory = new NotificationFactory(personalisationFactory);
        wrapper = new CcdResponseWrapper(new CcdResponse("SC/1234/5", new Subscription("Ronnie", "Scott", "Mr", "ABC",
                "test@testing.com", "07985858594", true, false), null, APPEAL_RECEIVED), null);
        when(config.getHmctsPhoneNumber()).thenReturn("01234543225");
        when(config.getManageEmailsLink()).thenReturn(new Link("http://manageemails.com/mac"));
        when(config.getTrackAppealLink()).thenReturn(new Link("http://tyalink.com/appeal_id"));
        when(config.getEvidenceSubmissionInfoLink()).thenReturn(new Link("http://link.com/appeal_id"));
    }

    @Test
    public void buildNotificationFromCcdResponse() {
        when(personalisationFactory.apply(APPEAL_RECEIVED)).thenReturn(personalisation);
        when(config.getTemplate(APPEAL_RECEIVED.getId())).thenReturn(new Template("123", null));
        Notification result = factory.create(wrapper);

        assertEquals("123", result.getEmailTemplate());
        assertEquals("test@testing.com", result.getEmail());
        assertEquals("ABC", result.getAppealNumber());
    }

    @Test
    public void buildSubscriptionCreatedNotificationFromCcdResponseWithSubscriptionUpdatedNotificationAndSmsFirstSubscribed() {
        when(personalisationFactory.apply(SUBSCRIPTION_UPDATED)).thenReturn(subscriptionPersonalisation);
        when(config.getTemplate(SUBSCRIPTION_CREATED.getId())).thenReturn(new Template("123", null));

        wrapper = new CcdResponseWrapper(new CcdResponse("SC/1234/5", new Subscription("Ronnie", "Scott", "Mr", "ABC",
                "test@testing.com", "07985858594", true, false), null, SUBSCRIPTION_UPDATED),
                new CcdResponse("SC/1234/5", new Subscription("Ronnie", "Scott", "Mr", "ABC",
                "test@testing.com", "07985858594", false, false), null, SUBSCRIPTION_UPDATED));

        Notification result = factory.create(wrapper);

        assertEquals("123", result.getEmailTemplate());
    }

    @Test
    public void buildSubscriptionUpdatedNotificationFromCcdResponseWithSubscriptionUpdatedNotificationAndSmsAlreadySubscribed() {
        when(personalisationFactory.apply(SUBSCRIPTION_UPDATED)).thenReturn(subscriptionPersonalisation);
        when(config.getTemplate(SUBSCRIPTION_UPDATED.getId())).thenReturn(new Template("123", null));

        wrapper = new CcdResponseWrapper(new CcdResponse("SC/1234/5", new Subscription("Ronnie", "Scott", "Mr", "ABC",
                "test@testing.com", "07985858594", true, false), null, SUBSCRIPTION_UPDATED),
                new CcdResponse("SC/1234/5", new Subscription("Ronnie", "Scott", "Mr", "ABC",
                        "test@testing.com", "07985858594", true, false), null, SUBSCRIPTION_UPDATED));

        Notification result = factory.create(wrapper);

        assertEquals("123", result.getEmailTemplate());
    }

    @Test
    public void returnNullIfPersonalisationNotFound() {
        when(personalisationFactory.apply(APPEAL_RECEIVED)).thenReturn(null);
        Notification result = factory.create(wrapper);

        assertNull(result);
    }
}
