package uk.gov.hmcts.reform.sscs.functional.sya.notifications;

import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.config.AppealHearingType.ORAL;
import static uk.gov.hmcts.reform.sscs.config.AppealHearingType.PAPER;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.lang.reflect.Field;
import java.util.List;
import junitparams.Parameters;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
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

    @Value("${notification.evidenceReceived.emailId}")
    private String evidenceReceivedAppellantEmailId;
    @Value("${notification.evidenceReceived.smsId}")
    private String evidenceReceivedAppellantSmsId;
    @Value("${notification.paper.evidenceReceived.representative.emailId}")
    private String paperEvidenceReceivedRepsEmailId;
    @Value("${notification.paper.evidenceReceived.representative.smsId}")
    private String paperEvidenceReceivedRepsSmsId;
    @Value("${notification.oral.evidenceReceived.representative.emailId}")
    private String oralEvidenceReceivedRepsEmailId;
    @Value("${notification.oral.evidenceReceived.representative.smsId}")
    private String oralEvidenceReceivedRepsSmsId;

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
    @Parameters(method = "evidenceReceivedNotifications")
    public void givenEventAndRepsSubscription_shouldSendNotificationToReps(AppealHearingType appealHearingType,
                                                                           NotificationEventType notificationEventType)
            throws Exception {
        //Given
        final String appellantEmailId = getFieldValue(appealHearingType, notificationEventType, "AppellantEmailId");
        final String appellantSmsId = getFieldValue(appealHearingType, notificationEventType, "AppellantSmsId");
        final String repsEmailId = getFieldValue(appealHearingType, notificationEventType, "RepsEmailId");
        final String repsSmsId = getFieldValue(appealHearingType, notificationEventType, "RepsSmsId");

        simulateCcdCallback(notificationEventType,
                "representative/"
                        + (appealHearingType != null ? (appealHearingType.name().toLowerCase() + "-") : "")
                        + notificationEventType.getId() + "Callback.json");

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
                "representative/no-reps-subscribed-" + notificationEventType.getId()
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
    @Parameters(method = "evidenceReceivedNotifications")
    public void givenEventAndNoRepsSubscription_shouldNotSendNotificationToReps(AppealHearingType appealHearingType,
                                                                                NotificationEventType notificationEventType)
            throws Exception {

        final String appellantEmailId = getFieldValue(appealHearingType, notificationEventType, "AppellantEmailId");
        final String appellantSmsId = getFieldValue(appealHearingType, notificationEventType, "AppellantSmsId");
        final String repsEmailId = getFieldValue(appealHearingType, notificationEventType, "RepsEmailId");
        final String repsSmsId = getFieldValue(appealHearingType, notificationEventType, "RepsSmsId");

        simulateCcdCallback(notificationEventType,
                "representative/" + "no-reps-subscribed-"
                        + (appealHearingType != null ? (appealHearingType.name().toLowerCase() + "-") : "")
                        + notificationEventType.getId() + "Callback.json");

        List<Notification> notifications = tryFetchNotificationsForTestCase(appellantEmailId,
                appellantSmsId);
        assertNotificationBodyContains(notifications, appellantEmailId);
        assertNotificationBodyContains(notifications, appellantSmsId);

        List<Notification> notificationsNotFound = tryFetchNotificationsForTestCaseWithFlag(true,
                repsEmailId, repsSmsId);
        assertTrue(notificationsNotFound.isEmpty());
    }

    private Object[] eventTypeAndSubscriptions() {
        return new Object[]{
            new Object[]{APPEAL_LAPSED_NOTIFICATION},
            new Object[]{APPEAL_WITHDRAWN_NOTIFICATION}
        };
    }

    private String getFieldValue(NotificationEventType notificationEventType, String fieldName) throws Exception {
        Field field = this.getClass().getDeclaredField(notificationEventType.getId() + fieldName);
        field.setAccessible(true);
        return (String) field.get(this);
    }

    private String getFieldValue(AppealHearingType appealHearingType, NotificationEventType notificationEventType, String fieldName) throws Exception {
        Field field = null;

        if (appealHearingType != null) {
            field = getField(appealHearingType, notificationEventType, fieldName);
        }

        if (field == null) {
            field = this.getClass().getDeclaredField(notificationEventType.getId() + fieldName);
        }

        field.setAccessible(true);
        return (String) field.get(this);
    }

    private Field getField(AppealHearingType appealHearingType, NotificationEventType notificationEventType, String fieldName) throws NoSuchFieldException {
        try {
            return this.getClass().getDeclaredField(appealHearingType.name().toLowerCase() + StringUtils.capitalize(notificationEventType.getId()) + fieldName);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private Object[] evidenceReceivedNotifications() {
        return new Object[]{
            new Object[]{ORAL, EVIDENCE_RECEIVED_NOTIFICATION},
            new Object[]{PAPER, EVIDENCE_RECEIVED_NOTIFICATION},
            new Object[]{APPEAL_LAPSED_NOTIFICATION},
            new Object[]{APPEAL_WITHDRAWN_NOTIFICATION},
            new Object[]{HEARING_BOOKED_NOTIFICATION}
        };
    }
}
