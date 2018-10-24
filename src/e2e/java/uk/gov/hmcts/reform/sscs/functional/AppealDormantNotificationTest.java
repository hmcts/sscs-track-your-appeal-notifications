package uk.gov.hmcts.reform.sscs.functional;

import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_DORMANT_NOTIFICATION;

import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.service.notify.Notification;

public class AppealDormantNotificationTest extends AbstractFunctionalTest {

    @Value("${notification.paper.appealDormant.emailId}")
    private String paperAppealDormantEmailId;

    @Value("${notification.paper.appealDormant.smsId}")
    private String paperAppealDormantSmsId;

    public AppealDormantNotificationTest() {
        super(30);
    }

    @Test
    public void shouldSendPaperAppealDormantNotification() throws Exception {
        simulateCcdCallback(APPEAL_DORMANT_NOTIFICATION, "paper-" + APPEAL_DORMANT_NOTIFICATION.getId()
                + "Callback.json");
        List<Notification> notifications = tryFetchNotificationsForTestCase(
                paperAppealDormantEmailId, paperAppealDormantSmsId);
        assertNotificationBodyContains(notifications, paperAppealDormantEmailId, caseData.getCaseReference());
    }
}
