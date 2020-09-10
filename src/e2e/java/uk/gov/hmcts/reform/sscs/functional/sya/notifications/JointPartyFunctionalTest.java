package uk.gov.hmcts.reform.sscs.functional.sya.notifications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_LAPSED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.HEARING_BOOKED_NOTIFICATION;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import junitparams.Parameters;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.functional.AbstractFunctionalTest;
import uk.gov.service.notify.Notification;

public class JointPartyFunctionalTest extends AbstractFunctionalTest {
    @Value("${notification.english.appealLapsed.joint_party.emailId}")
    private String appealLapsedJointPartyEmailId;
    @Value("${notification.english.appealLapsed.joint_party.smsId}")
    private String appealLapsedJointPartySmsId;
    @Value("${notification.english.hearingBooked.joint_party.emailId}")
    private String hearingBookedJointPartyEmailId;
    @Value("${notification.english.hearingBooked.joint_party.smsId}")
    private String hearingBookedJointPartySmsId;

    public JointPartyFunctionalTest() {
        super(30);
    }

    @Test
    @Parameters(method = "eventTypeAndSubscriptions")
    public void givenEventAndJointPartySubscription_shouldSendNotificationToJointParty(
            NotificationEventType notificationEventType, int expectedNumberOfLetters)
            throws Exception {
        //Given
        final String jointPartyEmailId = getFieldValue(notificationEventType, "JointPartyEmailId");
        final String jointPartySmsId = getFieldValue(notificationEventType, "JointPartySmsId");

        simulateCcdCallback(notificationEventType,
            "jointParty/" + notificationEventType.getId() + "Callback.json");

        List<Notification> notifications = tryFetchNotificationsForTestCase(jointPartyEmailId, jointPartySmsId);

        assertEquals(2, notifications.size());

        String jointPartyName = "Joint Party";
        assertNotificationBodyContains(notifications, jointPartyEmailId, jointPartyName);
        assertNotificationBodyContains(notifications, jointPartySmsId);

        if (expectedNumberOfLetters > 0) {
            List<Notification> notificationLetters = fetchLetters();
            assertEquals(expectedNumberOfLetters, notificationLetters.size());
            Optional<Notification> notificationOptional =
                    notificationLetters.stream().filter(notification ->
                            notification.getLine1().map(f -> f.contains(jointPartyName)).orElse(false)).findFirst();
            assertTrue(notificationOptional.isPresent());
            assertTrue(notificationOptional.get().getBody().contains("Dear " + jointPartyName));

        }
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

    @SuppressWarnings({"Indentation", "unused"})
    private Object[] eventTypeAndSubscriptions() {
        int expectedNumberOfLettersIsTwo = 2;
        int expectedNumberOfLettersIsZero = 0;
        return new Object[]{
            new Object[]{APPEAL_LAPSED_NOTIFICATION, expectedNumberOfLettersIsTwo},
            new Object[]{HEARING_BOOKED_NOTIFICATION, expectedNumberOfLettersIsZero}
        };
    }
}