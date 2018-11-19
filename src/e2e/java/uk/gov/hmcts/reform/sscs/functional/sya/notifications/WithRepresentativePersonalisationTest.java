package uk.gov.hmcts.reform.sscs.functional.sya.notifications;

import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

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

    @Value("${notification.hearingPostponed.appellant.emailId}")
    private String hearingPostponedAppellantEmailId;
    @Value("${notification.hearingPostponed.representative.emailId}")
    private String hearingPostponedRepsEmailId;

    public WithRepresentativePersonalisationTest() {
        super(30);
    }

    @Test
    @Parameters(method = "eventTypeAndSubscriptions")
    public void givenEventAndRepsSubscription_shouldSendNotificationToReps(NotificationEventType notificationEventType,
                                                                           boolean isSmsNotificationFound)
        throws Exception {
        //Given
        final String appellantEmailId = getFieldValue(notificationEventType, "AppellantEmailId");
        final String appellantSmsId = getFieldValue(notificationEventType, "AppellantSmsId");
        final String repsEmailId = getFieldValue(notificationEventType, "RepsEmailId");
        final String repsSmsId = getFieldValue(notificationEventType, "RepsSmsId");

        simulateCcdCallback(notificationEventType,
            "representative/" + notificationEventType.getId() + "Callback.json");

        List<Notification> notifications;
        if (isSmsNotificationFound) {
            notifications = tryFetchNotificationsForTestCase(
                appellantEmailId, appellantSmsId, repsEmailId, repsSmsId);
            assertNotificationBodyContains(notifications, appellantEmailId);
            assertNotificationBodyContains(notifications, appellantSmsId);
        } else {
            notifications = tryFetchNotificationsForTestCase(
                appellantEmailId, repsEmailId);
            assertNotificationBodyContains(notifications, appellantEmailId);
        }

        String representativeName = "Harry Potter";
        assertNotificationBodyContains(notifications, repsEmailId, representativeName);

        if (isSmsNotificationFound) {
            assertNotificationBodyContains(notifications, repsSmsId);
        }
    }

    @Test
    @Parameters(method = "eventTypeAndSubscriptions")
    public void givenEventAndNoRepsSubscription_shouldNotSendNotificationToReps(NotificationEventType notificationEventType,
                                                                                boolean isSmsNotificationFound)
        throws Exception {

        final String appellantEmailId = getFieldValue(notificationEventType, "AppellantEmailId");
        final String appellantSmsId = getFieldValue(notificationEventType, "AppellantSmsId");
        final String repsEmailId = getFieldValue(notificationEventType, "RepsEmailId");
        final String repsSmsId = getFieldValue(notificationEventType, "RepsSmsId");

        simulateCcdCallback(notificationEventType,
            "representative/" + "no-reps-subscribed-" + notificationEventType.getId()
                + "Callback.json");

        List<Notification> notifications;
        if (isSmsNotificationFound) {
            notifications = tryFetchNotificationsForTestCase(appellantEmailId, appellantSmsId);
            assertNotificationBodyContains(notifications, appellantEmailId);
            assertNotificationBodyContains(notifications, appellantSmsId);
        } else {
            notifications = tryFetchNotificationsForTestCase(appellantEmailId);
            assertNotificationBodyContains(notifications, appellantEmailId);
        }

        List<Notification> notificationsNotFound = tryFetchNotificationsForTestCaseWithFlag(true,
            repsEmailId, repsSmsId);
        assertTrue(notificationsNotFound.isEmpty());
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
        return new Object[] {
            new Object[] {APPEAL_LAPSED_NOTIFICATION, true},
            new Object[] {APPEAL_WITHDRAWN_NOTIFICATION, true},
            new Object[] {POSTPONEMENT_NOTIFICATION, false},
        };
    }
}
