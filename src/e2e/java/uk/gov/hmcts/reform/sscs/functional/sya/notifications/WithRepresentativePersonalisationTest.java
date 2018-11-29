package uk.gov.hmcts.reform.sscs.functional.sya.notifications;

import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_LAPSED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_RECEIVED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_WITHDRAWN_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.HEARING_BOOKED_NOTIFICATION;

import java.lang.reflect.Field;
import java.util.List;
import junitparams.Parameters;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.functional.AbstractFunctionalTest;
import uk.gov.service.notify.Notification;

public class WithRepresentativePersonalisationTest extends AbstractFunctionalTest {
    @Value("${notification.appealLapsed.appellant.emailId}")
    private String appealLapsedAppellantEmailId;
    @Value("${notification.appealLapsed.appellant.smsId}")
    private String appealLapsedAppellantSmsId;
    @Value("${notification.appealLapsed.representative.emailId}")
    private String appealLapsedRepsEmailId;
    @Value("${notification.appealLapsed.representative.smsId}")
    private String appealLapsedRepsSmsId;
    @Value("${notification.appealWithdrawn.appellant.emailId}")
    private String appealWithdrawnAppellantEmailId;
    @Value("${notification.appealWithdrawn.appellant.smsId}")
    private String appealWithdrawnAppellantSmsId;
    @Value("${notification.appealWithdrawn.representative.emailId}")
    private String appealWithdrawnRepsEmailId;
    @Value("${notification.appealWithdrawn.representative.smsId}")
    private String appealWithdrawnRepsSmsId;
    @Value("${notification.hearingBooked.appellant.emailId}")
    private String hearingBookedAppellantEmailId;
    @Value("${notification.hearingBooked.appellant.smsId}")
    private String hearingBookedAppellantSmsId;
    @Value("${notification.hearingBooked.representative.emailId}")
    private String hearingBookedRepsEmailId;
    @Value("${notification.hearingBooked.representative.smsId}")
    private String hearingBookedRepsSmsId;

    @Value("${notification.appealReceived.appellant.emailId}")
    private String appealReceivedAppellantEmailId;
    @Value("${notification.appealReceived.appellant.smsId}")
    private String appealReceivedAppellantSmsId;
    @Value("${notification.appealReceived.representative.emailId}")
    private String appealReceivedRepsEmailId;
    @Value("${notification.appealReceived.representative.smsId}")
    private String appealReceivedRepsSmsId;
    @Value("${notification.hearingPostponed.appellant.emailId}")
    private String hearingPostponedAppellantEmailId;
    @Value("${notification.hearingPostponed.representative.emailId}")
    private String hearingPostponedRepsEmailId;

    @Value("${notification.addRepresentative.representative.emailId}")
    private String addRepresentativeRepsEmailId;
    @Value("${notification.addRepresentative.representative.smsId}")
    private String addRepresentativeRepsSmsId;

    public WithRepresentativePersonalisationTest() {
        super(30);
    }

    @Test
    @Parameters(method = "eventTypeAndSubscriptions")
    public void givenEventAndRepsSubscription_shouldSendNotificationToReps(NotificationEventType notificationEventType)
        throws Exception {
        //Given
        final String appellantEmailId = getFieldValue(notificationEventType, "AppellantEmailId");
        final String appellantSmsId = getFieldValue(notificationEventType, "AppellantSmsId");
        final String repsEmailId = getFieldValue(notificationEventType, "RepsEmailId");
        final String repsSmsId = getFieldValue(notificationEventType, "RepsSmsId");

        simulateCcdCallback(notificationEventType,
            "representative/" + notificationEventType.getId() + "Callback.json");

        List<Notification> notifications = tryFetchNotificationsForTestCase(
            appellantEmailId, appellantSmsId,
            repsEmailId, repsSmsId);

        assertNotificationBodyContains(notifications, appellantEmailId);
        assertNotificationBodyContains(notifications, appellantSmsId);

        String representativeName = "Harry Potter";
        assertNotificationBodyContains(notifications, repsEmailId, representativeName);
        assertNotificationBodyContains(notifications, repsSmsId);
    }

    @Test
    @Parameters(method = "eventTypeAndSubscriptions")
    public void givenEventAndNoRepsSubscription_shouldNotSendNotificationToReps(NotificationEventType notificationEventType)
        throws Exception {

        final String appellantEmailId = getFieldValue(notificationEventType, "AppellantEmailId");
        final String appellantSmsId = getFieldValue(notificationEventType, "AppellantSmsId");
        final String repsEmailId = getFieldValue(notificationEventType, "RepsEmailId");
        final String repsSmsId = getFieldValue(notificationEventType, "RepsSmsId");

        simulateCcdCallback(notificationEventType,
            "representative/" + "no-reps-subscribed-" + notificationEventType.getId()
                + "Callback.json");

        List<Notification> notifications = tryFetchNotificationsForTestCaseWithFlag(true,appellantEmailId,
            appellantSmsId);
        assertNotificationBodyContains(notifications, appellantEmailId);
        assertNotificationBodyContains(notifications, appellantSmsId);

        List<Notification> notificationsNotFound = tryFetchNotificationsForTestCaseWithFlag(true,
            repsEmailId, repsSmsId);
        assertTrue(notificationsNotFound.isEmpty());
    }

    @Test
    public void givenHearingPostponedEventAndRepsSubscription_shouldSendEmailOnlyNotificationToReps()
        throws Exception {

        final String appellantEmailId = getFieldValue(POSTPONEMENT_NOTIFICATION, "AppellantEmailId");
        final String repsEmailId = getFieldValue(POSTPONEMENT_NOTIFICATION, "RepsEmailId");

        simulateCcdCallback(POSTPONEMENT_NOTIFICATION,
            "representative/" + POSTPONEMENT_NOTIFICATION.getId()
                + "Callback.json");

        List<Notification> notifications = tryFetchNotificationsForTestCase(
            appellantEmailId, repsEmailId);

        assertNotificationBodyContains(notifications, appellantEmailId);

        String representativeName = "Harry Potter";
        assertNotificationBodyContains(notifications, repsEmailId, representativeName);
    }

    @Test
    public void givenHearingPostponedEventAndNoRepsSubscription_shouldNotSendEmailOnlyNotificationToReps()
        throws Exception {

        final String appellantEmailId = getFieldValue(POSTPONEMENT_NOTIFICATION, "AppellantEmailId");
        final String repsEmailId = getFieldValue(POSTPONEMENT_NOTIFICATION, "RepsEmailId");

        simulateCcdCallback(POSTPONEMENT_NOTIFICATION,
            "representative/" + "no-reps-subscribed-" + POSTPONEMENT_NOTIFICATION.getId()
                + "Callback.json");

        List<Notification> notifications = tryFetchNotificationsForTestCase(appellantEmailId);
        assertNotificationBodyContains(notifications, appellantEmailId);

        List<Notification> notificationsNotFound = tryFetchNotificationsForTestCaseWithFlag(true,
            repsEmailId);
        assertTrue(notificationsNotFound.isEmpty());
    }

    @Test
    public void givenAddRepresentativeEventAndRepsSubscription_shouldSendNotificationToReps()
        throws Exception {

        simulateCcdCallback(ADD_REPRESENTATIVE,
            "representative/" + ADD_REPRESENTATIVE.getId()
                + "Callback.json");

        List<Notification> notifications = tryFetchNotificationsForTestCase(
            addRepresentativeRepsEmailId, addRepresentativeRepsSmsId);

        String representativeName = "Harry Potter";
        assertNotificationBodyContains(notifications, addRepresentativeRepsEmailId, representativeName);
        assertNotificationBodyContains(notifications, addRepresentativeRepsSmsId);
    }

    private String getFieldValue(NotificationEventType notificationEventType, String fieldName) throws Exception {
        String fieldValue;
        try {
            Field field = this.getClass().getDeclaredField(notificationEventType.getId() + fieldName);
            field.setAccessible(true);
            fieldValue = (String) field.get(this);
        } catch (NoSuchFieldException e) {
            fieldValue = null;
        }
        return fieldValue;
    }

    private Object[] eventTypeAndSubscriptions() {
        return new Object[]{
            new Object[]{APPEAL_LAPSED_NOTIFICATION},
            new Object[]{APPEAL_WITHDRAWN_NOTIFICATION},
            new Object[]{APPEAL_RECEIVED_NOTIFICATION},
            new Object[]{HEARING_BOOKED_NOTIFICATION}
        };
    }
}
