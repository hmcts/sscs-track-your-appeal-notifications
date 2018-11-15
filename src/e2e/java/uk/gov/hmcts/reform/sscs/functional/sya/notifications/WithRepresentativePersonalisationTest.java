package uk.gov.hmcts.reform.sscs.functional.sya.notifications;

import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_LAPSED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_WITHDRAWN_NOTIFICATION;

import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.functional.AbstractFunctionalTest;
import uk.gov.service.notify.Notification;

@RunWith(JUnitParamsRunner.class)
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

    public WithRepresentativePersonalisationTest() {
        super(30);
    }

    @Test
    @Parameters(method = "eventTypeAndSubscriptions")
    public void givenEventAndRepsSubscription_shouldSendNotificationToReps(NotificationEventType notificationEventType,
                                                                           String appellantEmailId,
                                                                           String appellantSmsId,
                                                                           String repsEmailId,
                                                                           String repsSmsId)
            throws Exception {
        //Given
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
    public void givenEventAndNoRepsSubscription_shouldNotSendNotificationToReps(NotificationEventType notificationEventType,
                                                                                String appellantEmailId,
                                                                                String appellantSmsId,
                                                                                String repsEmailId,
                                                                                String repsSmsId)
            throws Exception {
        simulateCcdCallback(notificationEventType,
                "representative/" + "no-reps-subscribed-" + notificationEventType.getId()
                        + "Callback.json");

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
            new Object[]{APPEAL_LAPSED_NOTIFICATION,
                appealLapsedAppellantEmailId,
                appealLapsedAppellantSmsId,
                appealLapsedRepsEmailId,
                appealLapsedRepsSmsId},
            new Object[]{APPEAL_WITHDRAWN_NOTIFICATION,
                appealWithdrawnAppellantEmailId,
                appealWithdrawnAppellantSmsId,
                appealWithdrawnRepsEmailId,
                appealWithdrawnRepsSmsId}
        };
    }
}
