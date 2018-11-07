package uk.gov.hmcts.reform.sscs.functional.sya.notifications;

import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SYA_APPEAL_CREATED_NOTIFICATION;

import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.functional.AbstractFunctionalTest;
import uk.gov.service.notify.Notification;

public class AppealCreatedRepsNotificationTest extends AbstractFunctionalTest {
    @Value("${notification.appealCreated.appellant.emailId}")
    private String appealCreatedAppellantEmailId;
    @Value("${notification.appealCreated.appellant.smsId}")
    private String appealCreatedAppellantSmsId;
    @Value("${notification.appealCreated.representative.emailId}")
    private String appealCreatedRepsEmailId;
    @Value("${notification.appealCreated.representative.smsId}")
    private String appealCreatedRepsSmsId;

    public AppealCreatedRepsNotificationTest() {
        super(30);
    }

    @Test
    public void givenAppealCreatedEventAndRepsSubscription_shouldSendAppealCreatedNotificationToReps()
            throws Exception {
        simulateCcdCallback(SYA_APPEAL_CREATED_NOTIFICATION,
                "representative/" + SYA_APPEAL_CREATED_NOTIFICATION.getId() + "Callback.json");

        List<Notification> notifications = tryFetchNotificationsForTestCase(
                appealCreatedAppellantEmailId, appealCreatedAppellantSmsId,
                appealCreatedRepsEmailId, appealCreatedRepsSmsId);

        assertNotificationBodyContains(notifications, appealCreatedAppellantEmailId);
        assertNotificationBodyContains(notifications, appealCreatedAppellantSmsId);

        String representativeName = "Harry Potter";
        assertNotificationBodyContains(notifications, appealCreatedRepsEmailId, representativeName);
        assertNotificationBodyContains(notifications, appealCreatedRepsSmsId);
    }
}
