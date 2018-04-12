package uk.gov.hmcts.sscs.personalisation;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.sscs.config.AppConstants.*;
import static uk.gov.hmcts.sscs.domain.Benefit.PIP;
import static uk.gov.hmcts.sscs.domain.notify.EventType.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.sscs.config.NotificationConfig;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.domain.Subscription;
import uk.gov.hmcts.sscs.domain.notify.Event;
import uk.gov.hmcts.sscs.domain.notify.Link;
import uk.gov.hmcts.sscs.service.MessageAuthenticationServiceImpl;

public class SubscriptionPersonalisationTest {

    SubscriptionPersonalisation personalisation;

    Subscription newAppellantSubscription;

    Subscription oldAppellantSubscription;

    CcdResponse newCcdResponse;

    CcdResponse oldCcdResponse;

    @Mock
    private NotificationConfig config;

    @Mock
    private MessageAuthenticationServiceImpl macService;

    @Before
    public void setup() {
        initMocks(this);
        personalisation = new SubscriptionPersonalisation(config, macService);
        when(config.getHmctsPhoneNumber()).thenReturn("01234543225");
        when(config.getManageEmailsLink()).thenReturn(new Link("http://manageemails.com/mac"));
        when(config.getTrackAppealLink()).thenReturn(new Link("http://tyalink.com/appeal_id"));
        when(config.getEvidenceSubmissionInfoLink()).thenReturn(new Link("http://link.com/appeal_id"));
        when(config.getManageEmailsLink()).thenReturn(new Link("http://link.com/manage-email-notifications/mac"));
        when(config.getClaimingExpensesLink()).thenReturn(new Link("http://link.com/progress/appeal_id/expenses"));
        when(config.getHearingInfoLink()).thenReturn(new Link("http://link.com/progress/appeal_id/abouthearing"));
        when(macService.generateToken("GLSCRR", PIP.name())).thenReturn("ZYX");

        newAppellantSubscription = new Subscription("Harry", "Kane", "Mr", "GLSCRR", "test@email.com",
                "07983495065", true, true);

        oldAppellantSubscription = new Subscription("Harry", "Kane", "Mr", "GLSCRR", "test@email.com",
                "07983495065", false, false);

        newCcdResponse = new CcdResponse("54321", PIP,"1234", newAppellantSubscription, null, SUBSCRIPTION_UPDATED, null, null);
        oldCcdResponse = new CcdResponse("54321", PIP,"5432", oldAppellantSubscription, null, SUBSCRIPTION_UPDATED, null, null);
    }

    @Test
    public void customisePersonalisation() {
        Map<String, String> result = personalisation.create(new CcdResponseWrapper(newCcdResponse, oldCcdResponse));

        assertEquals("PIP benefit", result.get(BENEFIT_NAME_ACRONYM_LITERAL));
        assertEquals("Personal Independence Payment", result.get(BENEFIT_FULL_NAME_LITERAL));
        assertEquals("1234", result.get(APPEAL_REF));
        assertEquals("GLSCRR", result.get(APPEAL_ID));
        assertEquals("Harry Kane", result.get(APPELLANT_NAME));
        assertEquals("01234543225", result.get(PHONE_NUMBER));
        assertEquals("http://link.com/manage-email-notifications/ZYX", result.get(MANAGE_EMAILS_LINK_LITERAL));
        assertEquals("http://tyalink.com/GLSCRR", result.get(TRACK_APPEAL_LINK_LITERAL));
        assertEquals(DWP_ACRONYM, result.get(FIRST_TIER_AGENCY_ACRONYM));
        assertEquals(DWP_FUL_NAME, result.get(FIRST_TIER_AGENCY_FULL_NAME));
        assertEquals("http://link.com/GLSCRR", result.get(SUBMIT_EVIDENCE_LINK_LITERAL));
    }

    @Test
    public void customisePersonalisationSetsNotificationTypeToMostRecentWhenNewSubscription() {
        Event event = new Event(ZonedDateTime.now(), APPEAL_RECEIVED);
        newCcdResponse.setEvents(new ArrayList() {{
                add(event);
            }
        });

        personalisation.create(new CcdResponseWrapper(newCcdResponse, oldCcdResponse));

        assertEquals(APPEAL_RECEIVED, newCcdResponse.getNotificationType());
    }

    @Test
    public void customisePersonalisationSetsNotificationTypeToDoNotSendWhenEmailHasNotChanged() {
        newAppellantSubscription.setSubscribeEmail(true);
        oldAppellantSubscription.setSubscribeEmail(true);

        personalisation.create(new CcdResponseWrapper(newCcdResponse, oldCcdResponse));

        assertEquals(DO_NOT_SEND, newCcdResponse.getNotificationType());
    }

    @Test
    public void customisePersonalisationShouldLeaveNotificationTypeAsSubscriptionUpdatedWhenEmailHasChanged() {
        newAppellantSubscription.setSubscribeEmail(true);
        newAppellantSubscription.setEmail("changed@test.com");
        oldAppellantSubscription.setSubscribeEmail(true);

        personalisation.create(new CcdResponseWrapper(newCcdResponse, oldCcdResponse));

        assertEquals(SUBSCRIPTION_UPDATED, newCcdResponse.getNotificationType());
    }

    @Test
    public void checkSubscriptionCreatedNotificationTypeWhenSmsSubscribedIsFirstSet() {
        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(newCcdResponse, oldCcdResponse);

        assertTrue(result);
    }

    @Test
    public void checkSubscriptionCreatedNotificationTypeNotChangedWhenSmsSubscribedIsAlreadySet() {
        oldAppellantSubscription.setSubscribeSms(true);

        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(newCcdResponse, oldCcdResponse);

        assertFalse(result);
    }

    @Test
    public void checkSubscriptionCreatedNotificationTypeNotChangedWhenSmsSubscribedIsNotSet() {
        newAppellantSubscription.setSubscribeSms(false);

        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(newCcdResponse, oldCcdResponse);

        assertFalse(result);
    }

    @Test
    public void emptyOldAppellantSubscriptionReturnsFalseForSubscriptionCreatedNotificationType() {
        oldCcdResponse.setAppellantSubscription(null);

        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(newCcdResponse, oldCcdResponse);

        assertFalse(result);
    }

    @Test
    public void emptyNewAppellantSubscriptionReturnsFalseForSubscriptionCreatedNotificationType() {
        newCcdResponse.setAppellantSubscription(null);

        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(newCcdResponse, oldCcdResponse);

        assertFalse(result);
    }

    @Test
    public void setMostRecentEventTypeNotificationWhenEmailSubscribedIsFirstSet() {
        Event event = new Event(ZonedDateTime.now(), APPEAL_RECEIVED);
        newCcdResponse.setEvents(new ArrayList() {{
                add(event);
            }
        });

        CcdResponse result = personalisation.setMostRecentEventTypeNotification(newCcdResponse, oldCcdResponse);

        assertEquals(APPEAL_RECEIVED, result.getNotificationType());
    }

    @Test
    public void doNotUpdateMostRecentEventTypeNotificationWhenEmailSubscribedIsAlreadySet() {
        oldAppellantSubscription.setSubscribeEmail(true);

        Event event = new Event(ZonedDateTime.now(), APPEAL_RECEIVED);
        newCcdResponse.setEvents(new ArrayList() {{
                add(event);
            }
        });

        CcdResponse result = personalisation.setMostRecentEventTypeNotification(newCcdResponse, oldCcdResponse);

        assertEquals(SUBSCRIPTION_UPDATED, result.getNotificationType());
    }

    @Test
    public void doNotUpdateMostRecentEventTypeNotificationWhenEventTypeIsNotKnown() {
        Event event = new Event(ZonedDateTime.now(), null);
        newCcdResponse.setEvents(new ArrayList() {{
                add(event);
            }
        });

        CcdResponse result = personalisation.setMostRecentEventTypeNotification(newCcdResponse, oldCcdResponse);

        assertEquals(SUBSCRIPTION_UPDATED, result.getNotificationType());
    }

    @Test
    public void emptyOldAppellantSubscriptionDoesNotUpdateNotificationType() {
        oldCcdResponse.setAppellantSubscription(null);

        Event event = new Event(ZonedDateTime.now(), APPEAL_RECEIVED);
        newCcdResponse.setEvents(new ArrayList() {{
                add(event);
            }
        });

        CcdResponse result = personalisation.setMostRecentEventTypeNotification(newCcdResponse, oldCcdResponse);

        assertEquals(SUBSCRIPTION_UPDATED, result.getNotificationType());
    }

    @Test
    public void emptyNewAppellantSubscriptionDoesNotUpdateNotificationType() {
        newCcdResponse.setAppellantSubscription(null);

        Event event = new Event(ZonedDateTime.now(), APPEAL_RECEIVED);
        newCcdResponse.setEvents(new ArrayList() {{
                add(event);
            }
        });

        CcdResponse result = personalisation.setMostRecentEventTypeNotification(newCcdResponse, oldCcdResponse);

        assertEquals(SUBSCRIPTION_UPDATED, result.getNotificationType());
    }

    @Test
    public void emptyEventsDoesNotUpdateNotificationType() {
        newCcdResponse.setEvents(new ArrayList<>());

        CcdResponse result = personalisation.setMostRecentEventTypeNotification(newCcdResponse, oldCcdResponse);

        assertEquals(SUBSCRIPTION_UPDATED, result.getNotificationType());
    }

    @Test
    public void setNotificationTypeToDoNotSendWhenEmailNotChangedAndSubscriptionWasAndStillIsSubscribed() {
        newAppellantSubscription.setSubscribeEmail(true);
        oldAppellantSubscription.setSubscribeEmail(true);

        CcdResponse result = personalisation.doNotSendEmailUpdatedNotificationWhenEmailNotChanged(newCcdResponse, oldCcdResponse);

        assertEquals(DO_NOT_SEND, result.getNotificationType());
    }

    @Test
    public void leaveNotificationTypeAsSubscriptionUpdatedWhenEmailChangedAndSubscriptionWasAndStillIsSubscribed() {
        newAppellantSubscription.setSubscribeEmail(true);
        newAppellantSubscription.setEmail("changed@test.com");
        oldAppellantSubscription.setSubscribeEmail(true);

        CcdResponse result = personalisation.doNotSendEmailUpdatedNotificationWhenEmailNotChanged(newCcdResponse, oldCcdResponse);

        assertEquals(SUBSCRIPTION_UPDATED, result.getNotificationType());
    }

    @Test
    public void doNotChangeNotificationTypeWhenEmailNotChangedAndNowUnsubscribed() {
        newAppellantSubscription.setSubscribeEmail(true);
        newAppellantSubscription.setEmail("changed@test.com");
        oldAppellantSubscription.setSubscribeEmail(false);
        newCcdResponse.setNotificationType(APPEAL_RECEIVED);

        CcdResponse result = personalisation.doNotSendEmailUpdatedNotificationWhenEmailNotChanged(newCcdResponse, oldCcdResponse);

        assertEquals(APPEAL_RECEIVED, result.getNotificationType());
    }
}
