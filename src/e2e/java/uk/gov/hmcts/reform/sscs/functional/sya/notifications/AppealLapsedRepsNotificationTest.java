package uk.gov.hmcts.reform.sscs.functional.sya.notifications;

import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_LAPSED_NOTIFICATION;

import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.functional.AbstractFunctionalTest;
import uk.gov.service.notify.Notification;

public class AppealLapsedRepsNotificationTest extends AbstractFunctionalTest {
    @Value("${notification.appealLapsed.appellant.emailId}")
    private String appealLapsedAppellantEmailId;
    @Value("${notification.appealLapsed.appellant.smsId}")
    private String appealLapsedAppellantSmsId;
    @Value("${notification.appealLapsed.representative.emailId}")
    private String appealLapsedRepsEmailId;
    @Value("${notification.appealLapsed.representative.smsId}")
    private String appealLapsedRepsSmsId;

    public AppealLapsedRepsNotificationTest() {
        super(30);
    }

    @Test
    public void givenAppealLapsedEventAndRepsSubscription_shouldSendAppealLapsedNotificationToReps()
            throws Exception {
        simulateCcdCallback(APPEAL_LAPSED_NOTIFICATION,
                "representative/" + APPEAL_LAPSED_NOTIFICATION.getId() + "Callback.json");

        List<Notification> notifications = tryFetchNotificationsForTestCase(
                appealLapsedAppellantEmailId, appealLapsedAppellantSmsId,
                appealLapsedRepsEmailId, appealLapsedRepsSmsId);

        assertNotificationBodyContains(notifications, appealLapsedAppellantEmailId);
        assertNotificationBodyContains(notifications, appealLapsedAppellantSmsId);

        String representativeName = "Harry Potter";
        assertNotificationBodyContains(notifications, appealLapsedRepsEmailId, representativeName);
        assertNotificationBodyContains(notifications, appealLapsedRepsSmsId);
    }

    @Test
    public void givenAppealLapsedEventAndNoRepsSubscription_shouldNotSendAppealLapsedNotificationToReps()
            throws Exception {
        simulateCcdCallback(APPEAL_LAPSED_NOTIFICATION,
                "representative/" + "no-reps-subscribed-" + APPEAL_LAPSED_NOTIFICATION.getId()
                        + "Callback.json");

        List<Notification> notifications = tryFetchNotificationsForTestCase(appealLapsedAppellantEmailId,
                appealLapsedAppellantSmsId);
        assertNotificationBodyContains(notifications, appealLapsedAppellantEmailId);
        assertNotificationBodyContains(notifications, appealLapsedAppellantSmsId);

        List<Notification> notificationsNotFound = tryFetchNotificationsForTestCaseWithFlag(true,
                appealLapsedRepsEmailId, appealLapsedRepsSmsId);
        assertTrue(notificationsNotFound.isEmpty());
    }
}
