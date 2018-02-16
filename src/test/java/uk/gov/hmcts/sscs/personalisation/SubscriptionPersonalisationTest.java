package uk.gov.hmcts.sscs.personalisation;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.sscs.config.AppConstants.*;
import static uk.gov.hmcts.sscs.domain.notify.NotificationType.DWP_RESPONSE_RECEIVED;
import static uk.gov.hmcts.sscs.domain.notify.NotificationType.SUBSCRIPTION_CREATED;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.sscs.config.NotificationConfig;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.domain.Subscription;
import uk.gov.hmcts.sscs.domain.notify.Link;

public class SubscriptionPersonalisationTest {

    SubscriptionPersonalisation personalisation;

    Subscription newAppellantSubscription;

    Subscription oldAppellantSubscription;

    @Mock
    private NotificationConfig config;

    @Before
    public void setup() {
        initMocks(this);
        personalisation = new SubscriptionPersonalisation(config);
        when(config.getHmctsPhoneNumber()).thenReturn("01234543225");
        when(config.getManageEmailsLink()).thenReturn(new Link("http://manageemails.com/mac"));
        when(config.getTrackAppealLink()).thenReturn(new Link("http://tyalink.com/appeal_id"));
        when(config.getEvidenceSubmissionInfoLink()).thenReturn(new Link("http://link.com/appeal_id"));

        newAppellantSubscription = new Subscription("Harry", "Kane", "Mr", "GLSCRR", "test@email.com",
                "07983495065", true, false);

        oldAppellantSubscription = new Subscription("Harry", "Kane", "Mr", "GLSCRR", "test@email.com",
                "07983495065", false, false);
    }

    @Test
    public void customisePersonalisation() {
        Map<String, String> result = personalisation.create(new CcdResponseWrapper(
                new CcdResponse("1234", newAppellantSubscription, null, DWP_RESPONSE_RECEIVED),
                new CcdResponse("5432", oldAppellantSubscription, null, DWP_RESPONSE_RECEIVED)));

        assertEquals(BENEFIT_NAME_ACRONYM, result.get(BENEFIT_NAME_ACRONYM_LITERAL));
        assertEquals(BENEFIT_FULL_NAME, result.get(BENEFIT_FULL_NAME_LITERAL));
        assertEquals("1234", result.get(APPEAL_REF));
        assertEquals("GLSCRR", result.get(APPEAL_ID));
        assertEquals("Harry Kane", result.get(APPELLANT_NAME));
        assertEquals("01234543225", result.get(PHONE_NUMBER));
        assertEquals("http://manageemails.com/Mactoken", result.get(MANAGE_EMAILS_LINK_LITERAL));
        assertEquals("http://tyalink.com/GLSCRR", result.get(TRACK_APPEAL_LINK_LITERAL));
        assertEquals(DWP_ACRONYM, result.get(FIRST_TIER_AGENCY_ACRONYM));
        assertEquals(DWP_FUL_NAME, result.get(FIRST_TIER_AGENCY_FULL_NAME));
        assertEquals("05 February 1900", result.get(APPEAL_RESPOND_DATE));
        assertEquals("01 January 1900", result.get(EVIDENCE_RECEIVED_DATE_LITERAL));
        assertEquals("12 February 1900", result.get(HEARING_CONTACT_DATE));
        assertEquals("http://link.com/GLSCRR", result.get(SUBMIT_EVIDENCE_LINK_LITERAL));
    }

    @Test
    public void checkSubscriptionCreatedNotificationTypeWhenSmsSubscribedIsFirstSet() {
        CcdResponse result = personalisation.checkSubscriptionCreated(
                new CcdResponse("1234", newAppellantSubscription, null, DWP_RESPONSE_RECEIVED),
                new CcdResponse("5432", oldAppellantSubscription, null, DWP_RESPONSE_RECEIVED));

        assertEquals(SUBSCRIPTION_CREATED, result.getNotificationType());
    }

    @Test
    public void checkSubscriptionCreatedNotificationTypeNotChangedWhenSmsSubscribedIsAlreadySet() {
        oldAppellantSubscription.setSubscribeSms(true);

        CcdResponse result = personalisation.checkSubscriptionCreated(
                new CcdResponse("1234", newAppellantSubscription, null, DWP_RESPONSE_RECEIVED),
                new CcdResponse("5432", oldAppellantSubscription, null, DWP_RESPONSE_RECEIVED));

        assertEquals(DWP_RESPONSE_RECEIVED, result.getNotificationType());
    }
}
